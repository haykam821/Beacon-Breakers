package io.github.haykam821.beaconbreakers.game.player;

import java.util.Optional;
import java.util.UUID;

import io.github.haykam821.beaconbreakers.Main;
import io.github.haykam821.beaconbreakers.game.phase.BeaconBreakersActivePhase;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class PlayerEntry {
	private ServerPlayerEntity player;
	private UUID uuid;
	private Text name;
	private String sidebarName;

	private final BeaconBreakersActivePhase phase;

	private BeaconPlacement beacon = BeaconPlacement.Unplaced.INSTANCE;

	public PlayerEntry(ServerPlayerEntity player, BeaconBreakersActivePhase phase) {
		this.player = player;
		this.phase = phase;
	}

	/**
	 * {@return whether the player should be eliminated}
	 */
	public boolean tick() {
		if (this.player == null) {
			return this.phase.getInvulnerability() == 0;
		}

		return !this.phase.getMap().getBox().contains(player.getBlockPos());
	}

	public void removePlayer() {
		this.uuid = this.player.getUuid();

		this.name = this.player.getDisplayName();
		this.sidebarName = this.player.getEntityName();

		this.player = null;
	}

	public void restorePlayer(ServerPlayerEntity player) {
		this.uuid = null;

		this.name = null;
		this.sidebarName = null;

		this.player = player;
	}

	public void giveRespawnBeacon() {
		if (this.player == null) return;
		if (!(this.beacon instanceof BeaconPlacement.Unplaced)) return;

		Optional<RegistryEntryList.Named<Block>> maybeBeacons = Registries.BLOCK.getEntryList(Main.RESPAWN_BEACONS);
		if (maybeBeacons.isPresent()) {
			Optional<RegistryEntry<Block>> maybeBeacon = maybeBeacons.get().getRandom(this.player.getRandom());
			if (maybeBeacon.isPresent()) {
				this.player.giveItemStack(new ItemStack(maybeBeacon.get().value()));
			}
		}
	}

	public void initializePlayer() {
		this.player.changeGameMode(GameMode.SURVIVAL);

		this.giveRespawnBeacon();

		this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, this.phase.getInvulnerability(), 1, true, false));
		this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, this.phase.getInvulnerability(), 127, true, false));
	}

	public void dropInventory() {
		this.player.vanishCursedItems();
		this.player.getInventory().dropAll();

		this.player.dropXp();
		this.player.dropShoulderEntities();
	}

	public void sendMessage(Text message, boolean overlay) {
		if (this.player != null) {
			this.player.sendMessage(message, overlay);
		}
	}

	public Text getName() {
		return this.player == null ? this.name : this.player.getDisplayName();
	}

	private String getSidebarName() {
		return this.player == null ? this.sidebarName : this.player.getEntityName();
	}

	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public UUID getUuid() {
		return this.uuid;
	}

	public BeaconPlacement getBeacon() {
		return this.beacon;
	}

	public void setBeacon(BeaconPlacement beacon) {
		this.beacon = beacon;
	}

	public Text getSidebarEntryText() {
		return Text.empty().append(this.beacon.getSidebarEntryIcon()).append(ScreenTexts.SPACE).append(this.getSidebarName());
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.getPlayer() + ", beacon=" + this.beacon + "}";
	}
}