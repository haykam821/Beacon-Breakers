package io.github.haykam821.beaconbreakers;

import io.github.haykam821.beaconbreakers.game.BeaconBreakersConfig;
import io.github.haykam821.beaconbreakers.game.phase.BeaconBreakersWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "beaconbreakers";

	private static final Identifier RESPAWN_BEACONS_ID = new Identifier(MOD_ID, "respawn_beacons");
	public static final TagKey<Block> RESPAWN_BEACONS = TagKey.of(RegistryKeys.BLOCK, RESPAWN_BEACONS_ID);

	private static final Identifier BEACON_BREAKERS_ID = new Identifier(MOD_ID, "beacon_breakers");
	public static final GameType<BeaconBreakersConfig> BEACON_BREAKERS_TYPE = GameType.register(BEACON_BREAKERS_ID, BeaconBreakersConfig.CODEC, BeaconBreakersWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}
}
