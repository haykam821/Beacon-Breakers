package io.github.haykam821.beaconbreakers.game.player;

import java.util.Optional;
import java.util.UUID;

import io.github.haykam821.beaconbreakers.Main;
import io.github.haykam821.beaconbreakers.game.player.team.TeamEntry;
import net.minecraft.block.Block;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.util.InventoryUtil;

public class PlayerEntry {
	private ServerPlayerEntity player;
	private UUID uuid;
	private Text name;

	private final TeamEntry team;

	public PlayerEntry(ServerPlayerEntity player, TeamEntry team) {
		this.player = player;
		this.team = team;
	}

	/**
	 * {@return whether the player should be eliminated}
	 */
	public boolean tick() {
		if (this.player == null) {
			return this.team.getPhase().getInvulnerability() == 0;
		}

		return !this.team.getPhase().getMap().getBox().contains(player.getBlockPos());
	}

	public void removePlayer() {
		this.uuid = this.player.getUuid();
		this.name = this.player.getDisplayName();

		this.player = null;
	}

	public void restorePlayer(ServerPlayerEntity player) {
		this.uuid = null;
		this.name = null;

		this.player = player;
	}

	public void giveRespawnBeacon() {
		if (this.player == null) return;
		if (!(this.team.getBeacon() instanceof BeaconPlacement.Unplaced)) return;

		RegistryWrapper.WrapperLookup registries = this.player.getWorld().getRegistryManager();
		RegistryWrapper<Block> blocks = registries.getOrThrow(RegistryKeys.BLOCK);
		Optional<RegistryEntryList.Named<Block>> maybeBeacons = blocks.getOptional(Main.RESPAWN_BEACONS);
		if (maybeBeacons.isPresent()) {
			Optional<RegistryEntry<Block>> maybeBeacon = maybeBeacons.get().getRandom(this.player.getRandom());
			if (maybeBeacon.isPresent()) {
				this.player.giveItemStack(new ItemStack(maybeBeacon.get().value()));
			}
		}
	}

	public void initializePlayer() {
		if (this.player == null) return;

		this.player.changeGameMode(GameMode.SURVIVAL);

		InventoryUtil.clear(player);

		if (this.player == this.team.getMainPlayer().getPlayer()) {
			this.giveRespawnBeacon();
		}

		int invulnerability = this.team.getPhase().getInvulnerability();

		this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, invulnerability, 1, true, false));
		this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, invulnerability, 127, true, false));
	}

	public void dropInventory(DamageSource source) {
		this.player.vanishCursedItems();
		this.player.getInventory().dropAll();

		this.player.dropXp(this.player.getServerWorld(), source.getAttacker());
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

	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public UUID getUuid() {
		return this.uuid;
	}

	public TeamEntry getTeam() {
		return this.team;
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.getPlayer() + ", team=" + this.team + "}";
	}
}