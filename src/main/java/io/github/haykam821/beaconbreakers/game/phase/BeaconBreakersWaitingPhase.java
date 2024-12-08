package io.github.haykam821.beaconbreakers.game.phase;

import io.github.haykam821.beaconbreakers.game.BeaconBreakersConfig;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class BeaconBreakersWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final TeamSelectionLobby teamSelection;
	private final BeaconBreakersMap map;
	private final BeaconBreakersConfig config;

	public BeaconBreakersWaitingPhase(GameSpace gameSpace, ServerWorld world, TeamSelectionLobby teamSelection, BeaconBreakersMap map, BeaconBreakersConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.teamSelection = teamSelection;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<BeaconBreakersConfig> context) {
		BeaconBreakersConfig config = context.config();

		RegistryEntry<DimensionType> dimensionType = config.getMapConfig().getDimensionOptions().dimensionTypeEntry();
		BeaconBreakersMap map = new BeaconBreakersMap(context.server(), config.getMapConfig(), dimensionType.value());

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setSeed(RandomSeed.getSeed())
			.setDimensionType(dimensionType)
			.setGenerator(map.getChunkGenerator());

		return context.openWithWorld(worldConfig, (activity, world) -> {
			TeamSelectionLobby teamSelection = config.getTeams().isPresent() ? TeamSelectionLobby.addTo(activity, config.getTeams().get()) : null;

			BeaconBreakersWaitingPhase waiting = new BeaconBreakersWaitingPhase(activity.getGameSpace(), world, teamSelection, map, config);
			GameWaitingLobby.addTo(activity, config.getPlayerConfig());

			// Rules
			activity.deny(GameRuleType.BLOCK_DROPS);
			activity.deny(GameRuleType.CRAFTING);
			activity.deny(GameRuleType.FALL_DAMAGE);
			activity.deny(GameRuleType.HUNGER);
			activity.deny(GameRuleType.INTERACTION);
			activity.deny(GameRuleType.PORTALS);
			activity.deny(GameRuleType.PVP);
			activity.deny(GameRuleType.THROW_ITEMS);

			// Listeners
			activity.listen(PlayerDamageEvent.EVENT, waiting::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
			activity.listen(GamePlayerEvents.ACCEPT, waiting::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
		});
	}

	private GameResult requestStart() {
		BeaconBreakersActivePhase.open(this.gameSpace, this.world, this.teamSelection, this.map, this.config);
		return GameResult.ok();
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		Vec3d spawnPos = BeaconBreakersActivePhase.getSpawnPos(this.world, this.map, this.config.getMapConfig());
		return acceptor.teleport(this.world, spawnPos).thenRunForEach(player -> {
			player.changeGameMode(GameMode.ADVENTURE);
		});
	}

	private EventResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return EventResult.DENY;
	}

	private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		BeaconBreakersActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), player);
		player.setHealth(player.getMaxHealth());
		return EventResult.DENY;
	}
}
