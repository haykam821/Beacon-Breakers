package io.github.haykam821.beaconbreakers.game.phase;

import io.github.haykam821.beaconbreakers.game.BeaconBreakersConfig;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;

public class BeaconBreakersWaitingPhase {
	private final GameSpace gameSpace;
	private final BeaconBreakersMap map;
	private final BeaconBreakersConfig config;

	public BeaconBreakersWaitingPhase(GameSpace gameSpace, BeaconBreakersMap map, BeaconBreakersConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<BeaconBreakersConfig> context) {
		BeaconBreakersConfig config = context.getConfig();
		BeaconBreakersMap map = new BeaconBreakersMap(context.getServer(), config.getMapConfig());

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.getChunkGenerator())
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			BeaconBreakersWaitingPhase waiting = new BeaconBreakersWaitingPhase(game.getGameSpace(), map, config);
			GameWaitingLobby.applyTo(game, config.getPlayerConfig());

			// Rules
			game.deny(GameRule.BLOCK_DROPS);
			game.deny(GameRule.CRAFTING);
			game.deny(GameRule.FALL_DAMAGE);
			game.deny(GameRule.HUNGER);
			game.deny(GameRule.INTERACTION);
			game.deny(GameRule.PORTALS);
			game.deny(GameRule.PVP);
			game.deny(GameRule.THROW_ITEMS);

			// Listeners
			game.listen(PlayerAddListener.EVENT, waiting::addPlayer);
			game.listen(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
			game.listen(OfferPlayerListener.EVENT, waiting::offerPlayer);
			game.listen(RequestStartListener.EVENT, waiting::requestStart);
		});
	}

	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		BeaconBreakersActivePhase.open(this.gameSpace, this.map, this.config);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		BeaconBreakersActivePhase.spawn(this.gameSpace.getWorld(), this.map, this.config.getMapConfig(), player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		BeaconBreakersActivePhase.spawn(this.gameSpace.getWorld(), this.map, this.config.getMapConfig(), player);
		player.setHealth(player.getMaxHealth());
		return ActionResult.FAIL;
	}
}
