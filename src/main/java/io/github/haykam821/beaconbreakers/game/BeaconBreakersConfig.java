package io.github.haykam821.beaconbreakers.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class BeaconBreakersConfig {
	public static final Codec<BeaconBreakersConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			PlayerConfig.CODEC.fieldOf("players").forGetter(BeaconBreakersConfig::getPlayerConfig),
			BeaconBreakersMapConfig.CODEC.fieldOf("map").forGetter(BeaconBreakersConfig::getMapConfig),
			Codec.INT.optionalFieldOf("invulnerability", 2 * 60 * 20).forGetter(BeaconBreakersConfig::getInvulnerability),
			Codec.BOOL.optionalFieldOf("keep_inventory", false).forGetter(BeaconBreakersConfig::shouldKeepInventory),
			Codec.BOOL.optionalFieldOf("allow_self_breaking", false).forGetter(BeaconBreakersConfig::shouldAllowSelfBreaking)
		).apply(instance, BeaconBreakersConfig::new);
	});

	private final PlayerConfig playerConfig;
	private final BeaconBreakersMapConfig mapConfig;
	private final int invulnerability;
	private final boolean keepInventory;
	private final boolean allowSelfBreaking;

	public BeaconBreakersConfig(PlayerConfig playerConfig, BeaconBreakersMapConfig mapConfig, int invulnerability, boolean keepInventory, boolean allowSelfBreaking) {
		this.playerConfig = playerConfig;
		this.mapConfig = mapConfig;
		this.invulnerability = invulnerability;
		this.keepInventory = keepInventory;
		this.allowSelfBreaking = allowSelfBreaking;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public BeaconBreakersMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public int getInvulnerability() {
		return this.invulnerability;
	}

	public boolean shouldKeepInventory() {
		return this.keepInventory;
	}

	public boolean shouldAllowSelfBreaking() {
		return this.allowSelfBreaking;
	}
}
