package io.github.haykam821.beaconbreakers.game.player.team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.haykam821.beaconbreakers.game.phase.BeaconBreakersActivePhase;
import io.github.haykam821.beaconbreakers.game.player.PlayerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;

public class SingleTeamEntry extends TeamEntry {
	private PlayerEntry player = null;

	private SingleTeamEntry(BeaconBreakersActivePhase phase) {
		super(phase);
	}

	// Messages

	@Override
	protected Text getName() {
		return this.player.getName();
	}

	// Players

	@Override
	public void addPlayer(PlayerEntry player) {
		if (this.player != null) {
			throw new IllegalStateException("SingleTeamEntry can only have one player");
		}

		this.player = player;
	}

	@Override
	public void removePlayer(PlayerEntry player) {
		this.player = null;
	}

	@Override
	public Collection<PlayerEntry> getPlayers() {
		return this.player == null ? Set.of() : Set.of(this.player);
	}

	@Override
	public PlayerEntry getMainPlayer() {
		return this.player;
	}

	// Helpers

	private static TeamEntry of(BeaconBreakersActivePhase phase, ServerPlayerEntity player) {
		TeamEntry team = new SingleTeamEntry(phase);
		team.addPlayer(new PlayerEntry(player, team));

		return team;
	}

	public static List<TeamEntry> ofAll(BeaconBreakersActivePhase phase, PlayerSet players) {
		return players.stream()
			.map(player -> SingleTeamEntry.of(phase, player))
			.collect(Collectors.toCollection(ArrayList::new));
	}
}
