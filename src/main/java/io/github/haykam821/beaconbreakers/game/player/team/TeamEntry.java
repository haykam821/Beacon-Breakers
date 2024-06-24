package io.github.haykam821.beaconbreakers.game.player.team;

import java.util.Collection;
import java.util.Iterator;

import io.github.haykam821.beaconbreakers.game.phase.BeaconBreakersActivePhase;
import io.github.haykam821.beaconbreakers.game.player.BeaconPlacement;
import io.github.haykam821.beaconbreakers.game.player.PlayerEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public abstract class TeamEntry {
	private final BeaconBreakersActivePhase phase;

	private BeaconPlacement beacon = BeaconPlacement.Unplaced.INSTANCE;

	protected TeamEntry(BeaconBreakersActivePhase phase) {
		this.phase = phase;
	}

	public final BeaconBreakersActivePhase getPhase() {
		return this.phase;
	}

	public final BeaconPlacement getBeacon() {
		return this.beacon;
	}

	public final void setBeacon(BeaconPlacement beacon) {
		this.beacon = beacon;
	}

	public final void tick() {
		Iterator<PlayerEntry> iterator = this.getPlayers().iterator();

		while (iterator.hasNext()) {
			PlayerEntry entry = iterator.next();

			if (entry.tick()) {
				this.getPhase().applyEliminationToPlayer(entry);
				iterator.remove();
			}
		}
	}

	// Messages

	protected abstract Text getName();

	public final Text getSidebarEntryText() {
		return Text.empty().append(this.beacon.getSidebarEntryIcon()).append(ScreenTexts.SPACE).append(this.getName());
	}

	protected String getBeaconBreakTranslationKey() {
		return "text.beaconbreakers.beacon_break";
	}

	public final MutableText getBeaconBreakMessage(PlayerEntry breaker, boolean explosion) {
		String translationKey = this.getBeaconBreakTranslationKey();
		if (explosion) translationKey += ".explosion";

		Text name = this.getName();

		if (breaker == null) {
			return Text.translatable(translationKey + ".unattributed", name);
		} else {
			return Text.translatable(translationKey, name, breaker.getName());
		}
	}

	public MutableText getCannotBreakOwnBeaconMessage() {
		return Text.translatable("text.beaconbreakers.cannot_break_own_beacon");
	}

	public MutableText getWinMessage() {
		return Text.translatable("text.beaconbreakers.win", this.getName());
	}

	// Players

	public abstract void addPlayer(PlayerEntry player);

	public abstract void removePlayer(PlayerEntry player);

	public abstract Collection<PlayerEntry> getPlayers();

	/**
	 * {@return the player that receives the team's beacon to place}
	 */
	public abstract PlayerEntry getMainPlayer();

	public final boolean isEliminated() {
		return this.getPlayers().isEmpty();
	}
}
