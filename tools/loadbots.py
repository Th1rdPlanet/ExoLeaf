#!/usr/bin/env python3
"""ExoLeaf load generator: offline-mode bots for Minecraft 1.21.11 (protocol 774).

Each bot logs in, finishes configuration, answers keep-alives/pings/teleports and walks
randomly. Meant for an offline test server without a proxy.

    python3 loadbots.py --host 127.0.0.1 --port 25570 --bots 100 --duration 120
"""
import argparse, asyncio, random, struct, time, uuid, zlib

PROTOCOL = 774
# Packet ids for 1.21.11 from the vanilla data generator (reports/packets.json).
L_S_HELLO, L_S_ACK = 0x00, 0x03
L_C_DISCONNECT, L_C_ENCRYPT, L_C_FINISHED, L_C_COMPRESSION, L_C_QUERY = 0, 1, 2, 3, 4
L_S_QUERY_ANSWER = 2
C_C_DISCONNECT, C_C_FINISH, C_C_KEEPALIVE, C_C_PING, C_C_KNOWN_PACKS = 2, 3, 4, 5, 14
C_S_INFO, C_S_FINISH, C_S_KEEPALIVE, C_S_PONG, C_S_KNOWN_PACKS = 0, 3, 4, 5, 7
P_C_DISCONNECT, P_C_KEEPALIVE, P_C_PING, P_C_POSITION, P_C_START_CONFIG = 32, 43, 59, 70, 116
P_S_TELEPORT, P_S_KEEPALIVE, P_S_POS, P_S_PONG, P_S_LOADED, P_S_CONFIG_ACK = 0, 27, 29, 44, 43, 15

PLAY_INTERESTING = {P_C_DISCONNECT, P_C_KEEPALIVE, P_C_PING, P_C_POSITION, P_C_START_CONFIG}

stats = {"connected": 0, "playing": 0, "failed": 0, "disconnected": 0, "bytes_in": 0}
reasons = {}


def varint(n):
    n &= 0xFFFFFFFF
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def read_varint(buf, i):
    n = shift = 0
    while True:
        b = buf[i]
        i += 1
        n |= (b & 0x7F) << shift
        shift += 7
        if not b & 0x80:
            if n & 0x80000000:
                n -= 1 << 32
            return n, i


def string(s):
    b = s.encode()
    return varint(len(b)) + b


class Bot:
    def __init__(self, name, host, port, walk, view=2):
        self.name, self.host, self.port, self.walk, self.view = name, host, port, walk, view
        self.threshold = -1
        self.state = "login"
        self.pos = None
        self.home = None
        self.radius = 40

    async def read_packet(self):
        n = shift = 0
        while True:
            b = (await self.reader.readexactly(1))[0]
            n |= (b & 0x7F) << shift
            shift += 7
            if not b & 0x80:
                break
        data = await self.reader.readexactly(n)
        stats["bytes_in"] += n + 1
        if self.threshold >= 0:
            size, i = read_varint(data, 0)
            if size:
                if self.state == "play":
                    # Peek at the packet id; only decompress packets the bot reacts to.
                    head = zlib.decompressobj().decompress(data[i:], 5)
                    pid, _ = read_varint(head, 0)
                    if pid not in PLAY_INTERESTING:
                        return pid, b""
                data = zlib.decompress(data[i:])
            else:
                data = data[i:]
        pid, i = read_varint(data, 0)
        return pid, data[i:]

    def send(self, pid, payload=b""):
        body = varint(pid) + payload
        if self.threshold >= 0:
            if len(body) >= self.threshold:
                body = varint(len(body)) + zlib.compress(body)
            else:
                body = varint(0) + body
        self.writer.write(varint(len(body)) + body)

    async def run(self, duration):
        try:
            self.reader, self.writer = await asyncio.wait_for(asyncio.open_connection(self.host, self.port), 10)
        except Exception as e:
            stats["failed"] += 1
            reasons[type(e).__name__] = reasons.get(type(e).__name__, 0) + 1
            return
        stats["connected"] += 1
        self.send(0, varint(PROTOCOL) + string(self.host) + struct.pack(">H", self.port) + varint(2))
        offline = uuid.uuid3(uuid.NAMESPACE_DNS, "x")  # any UUID; the server derives the real one
        self.send(L_S_HELLO, string(self.name) + offline.bytes)
        walker = None
        end = time.time() + duration
        try:
            while time.time() < end:
                pid, p = await asyncio.wait_for(self.read_packet(), 30)
                if self.state == "login":
                    if pid == L_C_COMPRESSION:
                        self.threshold, _ = read_varint(p, 0)
                    elif pid == L_C_FINISHED:
                        self.send(L_S_ACK)
                        self.state = "config"
                        self.send(C_S_INFO, string("en_us") + bytes([self.view]) + varint(0) + b"\x01" + bytes([0x7F]) + varint(1) + b"\x00\x01" + varint(0))
                    elif pid == L_C_QUERY:
                        mid, _ = read_varint(p, 0)
                        self.send(L_S_QUERY_ANSWER, varint(mid) + b"\x00")
                    elif pid in (L_C_DISCONNECT, L_C_ENCRYPT):
                        raise ConnectionError("login refused" if pid == 0 else "server wants encryption (online mode)")
                elif self.state == "config":
                    if pid == C_C_KNOWN_PACKS:
                        self.send(C_S_KNOWN_PACKS, varint(1) + string("minecraft") + string("core") + string("1.21.11"))
                    elif pid == C_C_KEEPALIVE:
                        self.send(C_S_KEEPALIVE, p[:8])
                    elif pid == C_C_PING:
                        self.send(C_S_PONG, p[:4])
                    elif pid == C_C_FINISH:
                        self.send(C_S_FINISH)
                        self.state = "play"
                    elif pid == C_C_DISCONNECT:
                        raise ConnectionError("disconnected in configuration")
                else:
                    if pid == P_C_KEEPALIVE:
                        self.send(P_S_KEEPALIVE, p[:8])
                    elif pid == P_C_PING:
                        self.send(P_S_PONG, p[:4])
                    elif pid == P_C_POSITION:
                        tid, i = read_varint(p, 0)
                        x, y, z = struct.unpack(">ddd", p[i:i + 24])
                        self.pos = [x, y, z]
                        self.send(P_S_TELEPORT, varint(tid))
                        if walker is None:
                            self.send(P_S_LOADED)
                            stats["playing"] += 1
                            if self.walk:
                                walker = asyncio.create_task(self.walk_loop())
                    elif pid == P_C_START_CONFIG:
                        self.send(P_S_CONFIG_ACK)
                        self.state = "config"
                    elif pid == P_C_DISCONNECT:
                        raise ConnectionError("kicked")
                await self.writer.drain()
        except Exception as e:
            stats["disconnected"] += 1
            key = str(e) if isinstance(e, ConnectionError) else type(e).__name__
            reasons[key] = reasons.get(key, 0) + 1
        finally:
            if walker:
                walker.cancel()
            self.writer.close()

    async def walk_loop(self):
        angle = random.random() * 6.283
        while True:
            await asyncio.sleep(0.05)
            if self.pos is None:
                continue
            if random.random() < 0.05:
                angle += random.uniform(-1.5, 1.5)
            import math
            if self.home is None:
                self.home = list(self.pos)
            dx, dz = self.pos[0] - self.home[0], self.pos[2] - self.home[2]
            if dx * dx + dz * dz > self.radius * self.radius:
                angle = math.atan2(-dz, -dx) + random.uniform(-0.5, 0.5)
            self.pos[0] += math.cos(angle) * 0.2
            self.pos[2] += math.sin(angle) * 0.2
            self.send(P_S_POS, struct.pack(">ddd", *self.pos) + b"\x01")


async def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25570)
    ap.add_argument("--bots", type=int, default=50)
    ap.add_argument("--rate", type=float, default=10, help="new bots per second")
    ap.add_argument("--duration", type=int, default=120)
    ap.add_argument("--prefix", default="Bot")
    ap.add_argument("--no-walk", action="store_true")
    ap.add_argument("--view-distance", type=int, default=2, help="client view distance (fewer chunks to decode)")
    a = ap.parse_args()
    tasks = []
    start = time.time()
    for n in range(a.bots):
        bot = Bot(f"{a.prefix}{n:04d}"[:16], a.host, a.port, not a.no_walk, a.view_distance)
        tasks.append(asyncio.create_task(bot.run(a.duration - (time.time() - start))))
        await asyncio.sleep(1 / a.rate)

    async def report():
        while True:
            await asyncio.sleep(5)
            print(f"[{time.time() - start:5.0f}s] {stats} reasons={reasons}", flush=True)
    r = asyncio.create_task(report())
    await asyncio.gather(*tasks)
    r.cancel()
    print("FINAL", stats, reasons)


if __name__ == "__main__":
    asyncio.run(main())
