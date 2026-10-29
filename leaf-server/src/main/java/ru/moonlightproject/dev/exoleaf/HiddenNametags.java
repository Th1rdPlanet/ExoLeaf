package ru.moonlightproject.dev.exoleaf;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.dreeam.leaf.config.modules.lobby.LobbyMode;

/**
 * Hides name tags in lobby mode with a client-side team that exists only in packets. The server
 * scoreboard is untouched, so a plugin that assigns its own team to a player wins on that client.
 */
public final class HiddenNametags {

    private static final String TEAM = "exo_hidden";

    private HiddenNametags() {
    }

    private static PlayerTeam team(List<String> members) {
        PlayerTeam team = new PlayerTeam(new Scoreboard(), TEAM);
        team.setNameTagVisibility(Team.Visibility.NEVER);
        for (String member : members) {
            team.getPlayers().add(member);
        }
        return team;
    }

    /** Sends the team with every online player to the new player and adds the new player for others. */
    public static void onJoin(PlayerList list, ServerPlayer joined) {
        if (!LobbyMode.enabled || !LobbyMode.hideNametags) {
            return;
        }
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : list.getPlayers()) {
            names.add(player.getScoreboardName());
        }
        if (!names.contains(joined.getScoreboardName())) {
            names.add(joined.getScoreboardName());
        }
        joined.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team(names), true));
        ClientboundSetPlayerTeamPacket add = ClientboundSetPlayerTeamPacket.createPlayerPacket(
            team(List.of()), joined.getScoreboardName(), ClientboundSetPlayerTeamPacket.Action.ADD);
        for (ServerPlayer player : list.getPlayers()) {
            if (player != joined) {
                player.connection.send(add);
            }
        }
    }
}
