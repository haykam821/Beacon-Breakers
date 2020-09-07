package io.github.haykam821.beaconbreakers.game.phase;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.beaconbreakers.game.BeaconBreakersConfig;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMap;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

public class BeaconBreakersWaitingPhase {
	private final GameWorld gameWorld;
	private final BeaconBreakersMap map;
	private final BeaconBreakersConfig config;

	public BeaconBreakersWaitingPhase(GameWorld gameWorld, BeaconBreakersMap map, BeaconBreakersConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<GameWorld> open(GameOpenContext<BeaconBreakersConfig> context) {
		BeaconBreakersConfig config = context.getConfig();
		BeaconBreakersMapBuilder mapBuilder = new BeaconBreakersMapBuilder(config.getMapConfig());

		return mapBuilder.create(context.getServer()).thenCompose(map -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
				.setGenerator(map.getChunkGenerator())
				.setDefaultGameMode(GameMode.ADVENTURE);

			return context.openWorld(worldConfig).thenApply(gameWorld -> {
				BeaconBreakersWaitingPhase waiting = new BeaconBreakersWaitingPhase(gameWorld, map, config);

				return GameWaitingLobby.open(gameWorld, config.getPlayerConfig(), game -> {
					game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
					game.setRule(GameRule.CRAFTING, RuleResult.DENY);
					game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
					game.setRule(GameRule.HUNGER, RuleResult.DENY);
					game.setRule(GameRule.INTERACTION, RuleResult.DENY);
					game.setRule(GameRule.PORTALS, RuleResult.DENY);
					game.setRule(GameRule.PVP, RuleResult.DENY);
					game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);

					// Listeners
					game.on(PlayerAddListener.EVENT, waiting::addPlayer);
					game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
					game.on(OfferPlayerListener.EVENT, waiting::offerPlayer);
					game.on(RequestStartListener.EVENT, waiting::requestStart);
				});
			});
		});
	}

	private boolean isFull() {
		return this.gameWorld.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameWorld.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		BeaconBreakersActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		BeaconBreakersActivePhase.spawn(this.gameWorld.getWorld(), this.map, this.config.getMapConfig(), player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		BeaconBreakersActivePhase.spawn(this.gameWorld.getWorld(), this.map, this.config.getMapConfig(), player);
		player.setHealth(player.getMaxHealth());
		return ActionResult.FAIL;
	}
}
