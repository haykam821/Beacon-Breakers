package io.github.haykam821.beaconbreakers.game.player.team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.haykam821.beaconbreakers.game.phase.BeaconBreakersActivePhase;
import io.github.haykam821.beaconbreakers.game.player.PlayerEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.common.team.GameTeamList;
import xyz.nucleoid.plasmid.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.player.PlayerSet;

public class MultipleTeamEntry extends TeamEntry {
	private final List<PlayerEntry> players = new ArrayList<>();

	private final TeamManager teamManager;
	private final GameTeam team;

	private MultipleTeamEntry(BeaconBreakersActivePhase phase, TeamManager teamManager, GameTeam team) {
		super(phase);

		this.teamManager = teamManager;
		this.team = team;
	}

	// Messages

	@Override
	protected Text getName() {
		return this.team.config().name();
	}

	@Override
	protected String getBeaconBreakTranslationKey() {
		return super.getBeaconBreakTranslationKey() + ".team";
	}

	@Override
	public MutableText getCannotBreakOwnBeaconMessage() {
		return Text.translatable("text.beaconbreakers.cannot_break_own_beacon.team");
	}

	@Override
	public MutableText getWinMessage() {
		return Text.translatable("text.beaconbreakers.win.team", this.getName());
	}

	// Players

	@Override
	public void addPlayer(PlayerEntry player) {
		this.players.add(player);
		this.teamManager.addPlayerTo(player.getPlayer(), this.team.key());
	}

	@Override
	public void removePlayer(PlayerEntry player) {
		this.players.remove(player);
	}

	@Override
	public Collection<PlayerEntry> getPlayers() {
		return this.players;
	}

	@Override
	public PlayerEntry getMainPlayer() {
		return this.players.get(0);
	}

	// Helpers

	/**
	 * Creates a team entry for multiple players by adding a game team to a team manager.
	 */
	public static TeamEntry of(BeaconBreakersActivePhase phase, TeamManager teamManager, GameTeam initialTeam) {
		GameTeamConfig teamConfig = GameTeamConfig.builder(initialTeam.config())
			.setFriendlyFire(false)
			.setCollision(Team.CollisionRule.PUSH_OTHER_TEAMS)
			.build();

		GameTeam team = initialTeam.withConfig(teamConfig);
		teamManager.addTeam(team.key(), teamConfig);

		return new MultipleTeamEntry(phase, teamManager, team);
	}

	public static List<TeamEntry> allocate(BeaconBreakersActivePhase phase, PlayerSet players, TeamSelectionLobby teamSelection, TeamManager teamManager, GameTeamList teams) {
		Map<GameTeamKey, TeamEntry> teamsByKey = new HashMap<>();

		teamSelection.allocate(players, (teamKey, player) -> {
			TeamEntry team = teamsByKey.computeIfAbsent(teamKey, teamKeyx -> {
				GameTeam initialTeam = teams.byKey(teamKeyx);
				return of(phase, teamManager, initialTeam);
			});

			team.addPlayer(new PlayerEntry(player, team));
		});

		return teamsByKey.values().stream()
			.collect(Collectors.toCollection(ArrayList::new));
	}
}