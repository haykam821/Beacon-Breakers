package io.github.haykam821.beaconbreakers.game.phase;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import io.github.haykam821.beaconbreakers.Main;
import io.github.haykam821.beaconbreakers.game.BeaconBreakersConfig;
import io.github.haykam821.beaconbreakers.game.BeaconBreakersSidebar;
import io.github.haykam821.beaconbreakers.game.InvulnerabilityTimerBar;
import io.github.haykam821.beaconbreakers.game.PlayerEntry;
import io.github.haykam821.beaconbreakers.game.event.AfterBlockPlaceListener;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMap;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMapConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

public class BeaconBreakersActivePhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final BeaconBreakersMap map;
	private final BeaconBreakersConfig config;
	private final Set<PlayerEntry> players;
	private final InvulnerabilityTimerBar bar;
	private final BeaconBreakersSidebar sidebar;
	private boolean singleplayer;
	private int ticksUntilClose = -1;
	private int invulnerability;

	public BeaconBreakersActivePhase(GameSpace gameSpace, ServerWorld world, GlobalWidgets widgets, BeaconBreakersMap map, BeaconBreakersConfig config, Set<ServerPlayerEntity> players) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;

		this.sidebar = new BeaconBreakersSidebar(widgets, this);
		this.bar = new InvulnerabilityTimerBar(this, widgets);
		this.players = players.stream().map(player -> {
			return new PlayerEntry(player, this);
		}).collect(Collectors.toSet());

		this.invulnerability = this.config.getInvulnerability();
	}

	public static void open(GameSpace gameSpace, ServerWorld world, BeaconBreakersMap map, BeaconBreakersConfig config) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			Set<ServerPlayerEntity> players = Sets.newHashSet(gameSpace.getPlayers());
			BeaconBreakersActivePhase active = new BeaconBreakersActivePhase(gameSpace, world, widgets, map, config, players);

			activity.allow(GameRuleType.BLOCK_DROPS);
			activity.allow(GameRuleType.CRAFTING);
			activity.allow(GameRuleType.FALL_DAMAGE);
			activity.allow(GameRuleType.HUNGER);
			activity.deny(GameRuleType.PORTALS);
			activity.allow(GameRuleType.PVP);

			// Listeners
			activity.listen(AfterBlockPlaceListener.EVENT, active::afterBlockPlace);
			activity.listen(BlockBreakEvent.EVENT, active::onBreakBlock);
			activity.listen(GameActivityEvents.ENABLE, active::enable);
			activity.listen(GameActivityEvents.TICK, active::tick);
			activity.listen(GamePlayerEvents.OFFER, active::offerPlayer);
			activity.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);
			activity.listen(GamePlayerEvents.REMOVE, active::removePlayer);
			activity.listen(ExplosionDetonatedEvent.EVENT, active::onExplosionDetonated);
		});
	}

	private void enable() {
		this.singleplayer = this.players.size() == 1;

		for (PlayerEntry entry : this.players) {
			ServerPlayerEntity player = entry.getPlayer();
			
			if (player != null) {
				entry.initializePlayer();
				BeaconBreakersActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), player);
			}
		}

		this.sidebar.update();
	}
	
	private void tick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		if (this.invulnerability > 0) {
			this.bar.tick();

			this.invulnerability -= 1;
			if (this.invulnerability == 0) {
				this.gameSpace.getPlayers().playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1, 1);
				this.gameSpace.getPlayers().sendMessage(Text.translatable("text.beaconbreakers.invulnerability.ended").formatted(Formatting.GOLD));

				this.bar.remove();
			}
		}

		Iterator<PlayerEntry> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			PlayerEntry entry = iterator.next();

			if (entry.tick()) {
				ServerPlayerEntity player = entry.getPlayer();

				if (player != null) {
					this.setSpectator(player);
				}

				this.sendEliminateMessage(entry);
				iterator.remove();
				this.sidebar.update();
			}
		}
	
		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;

			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage().formatted(Formatting.GOLD));
			this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
		}
	}

	public boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}
	
	private MutableText getEndingMessage() {
		if (this.players.size() == 1) {
			PlayerEntry winner = this.players.iterator().next();
			return Text.translatable("text.beaconbreakers.win", winner.getName());
		}
		return Text.translatable("text.beaconbreakers.win.none");
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private void sendEliminateMessage(PlayerEntry entry) {
		if (this.isGameEnding()) {
			return;
		}

		this.gameSpace.getPlayers().sendMessage(Text.translatable("text.beaconbreakers.eliminate", entry.getName()).formatted(Formatting.RED));
	}

	private void eliminate(PlayerEntry entry) {
		ServerPlayerEntity player = entry.getPlayer();

		if (player != null) {
			this.setSpectator(player);
		}

		this.sendEliminateMessage(entry);
		this.players.remove(entry);
		this.sidebar.update();
	}

	private PlayerEntry getEntryFromPlayer(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player.equals(entry.getPlayer())) {
				return entry;
			}
		}
		return null;
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		ServerPlayerEntity player = offer.player();

		for (PlayerEntry entry : this.players) {
			if (player.getUuid().equals(entry.getUuid())) {
				Vec3d spawnPos = this.getRespawnPos(entry);

				if (spawnPos == null) {
					spawnPos = BeaconBreakersActivePhase.getSpawnPos(world, map, this.config.getMapConfig(), player);
				}

				return offer.accept(this.world, spawnPos).and(() -> {
					entry.restorePlayer(player);
					entry.initializePlayer();
				});
			}
		}

		Vec3d spawnPos = BeaconBreakersActivePhase.getSpawnPos(world, map, this.config.getMapConfig(), player);
		return offer.accept(this.world, spawnPos).and(() -> {
			this.setSpectator(player);
		});
	}

	private void removePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getEntryFromPlayer(player);

		if (entry != null) {
			entry.removePlayer();
		}
	}

	private Vec3d getRespawnPos(PlayerEntry entry) {
		BlockPos beaconPos = entry.getBeaconPos();
		if (beaconPos == null) {
			return null;
		}

		BlockState beaconState = this.world.getBlockState(beaconPos);
		if (!beaconState.isIn(Main.RESPAWN_BEACONS)) {
			return null;
		}

		Optional<Vec3d> spawnOptional = RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, world, beaconPos);
		if (spawnOptional.isPresent()) {
			Vec3d spawn = spawnOptional.get();
			if (spawn.getY() <= 255) {
				return spawn;
			}
		}
	
		return new Vec3d(beaconPos.getX() + 0.5, beaconPos.getY(), beaconPos.getZ() + 0.5);
	}

	private ActionResult attemptBeaconRespawn(PlayerEntry entry) {
		Vec3d spawn = this.getRespawnPos(entry);

		if (spawn == null) {
			return ActionResult.FAIL;
		}

		entry.getPlayer().teleport(world, spawn.getX(), spawn.getY(), spawn.getZ(), 0, 0);;

		return ActionResult.SUCCESS;
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return this.invulnerability > 0 || this.isGameEnding() ? ActionResult.FAIL : ActionResult.PASS;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		if (this.isGameEnding()) {
			BeaconBreakersActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), player);
			return ActionResult.FAIL;
		}

		if (!this.config.shouldKeepInventory()) {
			player.vanishCursedItems();
			player.getInventory().dropAll();

			player.dropXp();
			player.dropShoulderEntities();
		}

		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry != null) {
			if (this.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)) {
				this.gameSpace.getPlayers().sendMessage(player.getDamageTracker().getDeathMessage());
			}

			if (this.attemptBeaconRespawn(entry) == ActionResult.FAIL) {
				if (this.invulnerability > 0) {
					BeaconBreakersActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), entry.getPlayer());
				} else {
					this.eliminate(entry);
				}
			}
		}

		player.setHealth(player.getMaxHealth());
		player.getHungerManager().setFoodLevel(20);

		player.extinguish();
		player.getDamageTracker().update();
		player.fallDistance = 0;
	
		return ActionResult.FAIL;
	}

	private void breakBeacon(PlayerEntry breaker, BlockPos pos, boolean explosion) {
		if (this.isGameEnding()) {
			return;
		}

		boolean found = false;

		for (PlayerEntry entry : this.players) {
			if (pos.equals(entry.getBeaconPos())) {
				entry.setBeaconBroken();

				this.gameSpace.getPlayers().playSound(SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 1);

				String translationKey = "text.beaconbreakers.beacon_break";
				if (explosion) translationKey += ".explosion";

				MutableText message;
				Text playerName = entry.getName();

				if (breaker == null) {
					message = Text.translatable(translationKey + ".unattributed", playerName);
				} else {
					Text breakerName = breaker.getName();
					message = Text.translatable(translationKey, playerName, breakerName);
				}

				this.gameSpace.getPlayers().sendMessage(message.formatted(Formatting.RED));

				found = true;
				break;
			}
		}

		if (found) {
			this.sidebar.update();
		}
	}

	private ActionResult onBreakBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry == null) return ActionResult.PASS;

		BlockState state = world.getBlockState(pos);

		if (pos.equals(entry.getBeaconPos())) {
			if (this.invulnerability > 0) {
				entry.setBeaconPos(null);
				world.setBlockState(pos, state.getFluidState().getBlockState());

				entry.giveRespawnBeacon();
	
				this.sidebar.update();
			} else if (!this.config.shouldAllowSelfBreaking()) {
				player.sendMessage(Text.translatable("text.beaconbreakers.cannot_break_own_beacon").formatted(Formatting.RED), false);
			}

			return ActionResult.FAIL;
		}

		if (!state.isIn(Main.RESPAWN_BEACONS)) return ActionResult.SUCCESS;
			
		if (this.invulnerability > 0) {
			player.sendMessage(Text.translatable("text.beaconbreakers.cannot_break_invulnerable_beacon").formatted(Formatting.RED), false);
			return ActionResult.FAIL;
		}

		this.breakBeacon(entry, pos, false);
		world.setBlockState(pos, state.getFluidState().getBlockState());

		return ActionResult.FAIL;		
	}

	private void afterBlockPlace(BlockPos pos, World world, ServerPlayerEntity player, ItemStack stack, BlockState state) {
		if (this.isGameEnding()) return;
		if (!state.isIn(Main.RESPAWN_BEACONS)) return;
		
		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry == null) return;
		
		if (entry.getBeaconPos() != null) return;
		if (!this.map.getBox().contains(pos)) {
			entry.sendMessage(Text.translatable("text.beaconbreakers.cannot_place_out_of_bounds_beacon").formatted(Formatting.RED), false);
			return;
		}

		entry.setBeaconPos(pos);
		this.sidebar.update();
	}

	private void onExplosionDetonated(Explosion explosion, boolean particles) {
		if (explosion.world.isClient()) return;
		ServerWorld world = (ServerWorld) explosion.world;

		LivingEntity causingEntity = explosion.getCausingEntity();
		PlayerEntry entry = causingEntity instanceof ServerPlayerEntity player ? this.getEntryFromPlayer(player) : null;

		Iterator<BlockPos> iterator = explosion.getAffectedBlocks().iterator();

		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();

			// Ignore non-respawn beacons
			BlockState state = world.getBlockState(pos);
			if (!state.isIn(Main.RESPAWN_BEACONS)) continue;

			// Prevent players from blowing up beacons during the invulnerability phase
			if (this.invulnerability > 0) {
				iterator.remove();
				continue;
			}

			// Prevent players from blowing up their own beacons
			if (!this.config.shouldAllowSelfBreaking() && entry != null && pos.equals(entry.getBeaconPos())) {
				iterator.remove();
				continue;
			}

			this.breakBeacon(entry, pos, true);
			world.setBlockState(pos, state.getFluidState().getBlockState());
		}
	}

	public GameSpace getGameSpace() {
		return this.gameSpace;
	}

	public BeaconBreakersMap getMap() {
		return this.map;
	}

	public BeaconBreakersConfig getConfig() {
		return this.config;
	}

	public int getInvulnerability() {
		return this.invulnerability;
	}

	public Set<PlayerEntry> getPlayers() {
		return this.players;
	}

	public static Vec3d getSpawnPos(ServerWorld world, BeaconBreakersMap map, BeaconBreakersMapConfig mapConfig, ServerPlayerEntity player) {
		int x = mapConfig.getX() * 8;
		int z = mapConfig.getZ() * 8;

		int surfaceY = map.getChunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE, world, world.getChunkManager().getNoiseConfig());
		return new Vec3d(x + 0.5, surfaceY, z + 0.5);
	}

	public static void spawn(ServerWorld world, BeaconBreakersMap map, BeaconBreakersMapConfig mapConfig, ServerPlayerEntity player) {
		Vec3d spawnPos = BeaconBreakersActivePhase.getSpawnPos(world, map, mapConfig, player);
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
	}
}
