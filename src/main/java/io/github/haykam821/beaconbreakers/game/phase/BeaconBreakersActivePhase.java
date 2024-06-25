package io.github.haykam821.beaconbreakers.game.phase;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import io.github.haykam821.beaconbreakers.Main;
import io.github.haykam821.beaconbreakers.game.BeaconBreakersConfig;
import io.github.haykam821.beaconbreakers.game.BeaconBreakersSidebar;
import io.github.haykam821.beaconbreakers.game.InvulnerabilityTimerBar;
import io.github.haykam821.beaconbreakers.game.event.AfterBlockPlaceListener;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMap;
import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersMapConfig;
import io.github.haykam821.beaconbreakers.game.player.BeaconPlacement;
import io.github.haykam821.beaconbreakers.game.player.PlayerEntry;
import io.github.haykam821.beaconbreakers.game.player.team.MultipleTeamEntry;
import io.github.haykam821.beaconbreakers.game.player.team.SingleTeamEntry;
import io.github.haykam821.beaconbreakers.game.player.team.TeamEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.explosion.Explosion;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

public class BeaconBreakersActivePhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final BeaconBreakersMap map;
	private final BeaconBreakersConfig config;
	private final List<TeamEntry> teams;
	private final InvulnerabilityTimerBar bar;
	private final BeaconBreakersSidebar sidebar;
	private boolean singleplayer;
	private int ticksUntilClose = -1;
	private int invulnerability;

	public BeaconBreakersActivePhase(GameSpace gameSpace, ServerWorld world, TeamSelectionLobby teamSelection, TeamManager teamManager, GlobalWidgets widgets, BeaconBreakersMap map, BeaconBreakersConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;

		this.sidebar = new BeaconBreakersSidebar(widgets, this);
		this.bar = new InvulnerabilityTimerBar(this, widgets);

		this.teams = config.getTeams()
			.map(teams -> MultipleTeamEntry.allocate(this, gameSpace.getPlayers(), teamSelection, teamManager, teams))
			.orElseGet(() -> SingleTeamEntry.ofAll(this, gameSpace.getPlayers()));

		this.invulnerability = this.config.getInvulnerability();
	}

	public static void open(GameSpace gameSpace, ServerWorld world, TeamSelectionLobby teamSelection, BeaconBreakersMap map, BeaconBreakersConfig config) {
		gameSpace.setActivity(activity -> {
			TeamManager teamManager = TeamManager.addTo(activity);
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);

			BeaconBreakersActivePhase active = new BeaconBreakersActivePhase(gameSpace, world, teamSelection, teamManager, widgets, map, config);

			activity.allow(GameRuleType.BLOCK_DROPS);
			activity.allow(GameRuleType.CRAFTING);
			activity.allow(GameRuleType.FALL_DAMAGE);
			activity.allow(GameRuleType.HUNGER);
			activity.deny(GameRuleType.PORTALS);
			activity.allow(GameRuleType.PVP);

			// Listeners
			activity.listen(AfterBlockPlaceListener.EVENT, active::afterBlockPlace);
			activity.listen(BlockUseEvent.EVENT, active::onUseBlock);
			activity.listen(BlockBreakEvent.EVENT, active::onBreakBlock);
			activity.listen(GameActivityEvents.ENABLE, active::enable);
			activity.listen(GameActivityEvents.TICK, active::tick);
			activity.listen(GamePlayerEvents.OFFER, active::offerPlayer);
			activity.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);
			activity.listen(GamePlayerEvents.REMOVE, active::removePlayer);
			activity.listen(ExplosionDetonatedEvent.EVENT, active::onExplosionDetonated);
			activity.listen(ItemThrowEvent.EVENT, active::onThrowItem);
		});
	}

	private void enable() {
		this.singleplayer = this.teams.size() == 1;

		for (TeamEntry team : this.teams) {
			if (team.isEliminated()) {
				throw new IllegalStateException("Team should not be immediately eliminated");
			}

			for (PlayerEntry entry : team.getPlayers()) {
				ServerPlayerEntity player = entry.getPlayer();

				if (player != null) {
					entry.initializePlayer();
					BeaconBreakersActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), player);
				}
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

		Iterator<TeamEntry> iterator = this.teams.iterator();
		boolean sidebarDirty = false;

		while (iterator.hasNext()) {
			TeamEntry entry = iterator.next();
			entry.tick();

			if (entry.isEliminated()) {
				iterator.remove();
				sidebarDirty = true;
			}
		}

		if (sidebarDirty) {
			this.sidebar.update();
		}
	
		if (this.teams.size() < 2) {
			if (this.teams.size() == 1 && this.singleplayer) return;

			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage().formatted(Formatting.GOLD));
			this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
		}
	}

	public boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}
	
	private MutableText getEndingMessage() {
		if (this.teams.size() == 1) {
			TeamEntry winner = this.teams.iterator().next();
			return winner.getWinMessage();
		}
		return Text.translatable("text.beaconbreakers.win.none");
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	public void applyEliminationToPlayer(PlayerEntry entry) {
		ServerPlayerEntity player = entry.getPlayer();

		if (player != null) {
			this.setSpectator(player);
		}

		if (!this.isGameEnding()) {
			this.gameSpace.getPlayers().sendMessage(Text.translatable("text.beaconbreakers.eliminate", entry.getName()).formatted(Formatting.RED));
		}
	}

	private void eliminate(PlayerEntry entry) {
		this.applyEliminationToPlayer(entry);

		TeamEntry team = entry.getTeam();
		team.removePlayer(entry);

		if (team.isEliminated() && this.teams.remove(team)) {
			this.sidebar.update();
		}
	}

	private PlayerEntry getEntryFromPlayer(ServerPlayerEntity player) {
		for (TeamEntry team : this.teams) {
			for (PlayerEntry entry : team.getPlayers()) {
				if (player.equals(entry.getPlayer())) {
					return entry;
				}
			}
		}
		return null;
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		ServerPlayerEntity player = offer.player();

		for (TeamEntry team : this.teams) {
			for (PlayerEntry entry : team.getPlayers()) {
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
		if (!(entry.getTeam().getBeacon() instanceof BeaconPlacement.Placed placed)) {
			return null;
		}

		BlockPos beaconPos = placed.pos();

		BlockState beaconState = this.world.getBlockState(beaconPos);
		if (!beaconState.isIn(Main.RESPAWN_BEACONS)) {
			return null;
		}

		int topY = this.world.getTopY() - 3;

		Optional<Vec3d> spawnOptional = RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, world, beaconPos);
		if (spawnOptional.isPresent()) {
			Vec3d spawn = spawnOptional.get();
			if (spawn.getY() <= topY) {
				return spawn;
			}
		}
	
		Direction direction = Direction.Type.HORIZONTAL.random(this.world.getRandom());
		float widthOffset = beaconPos.getY() > topY ? EntityType.PLAYER.getWidth() : 0;

		double x = beaconPos.getX() + 0.5 + (direction.getOffsetX() * widthOffset);
		double y = Math.min(beaconPos.getY() + 1, topY);
		double z = beaconPos.getZ() + 0.5 + (direction.getOffsetZ() * widthOffset);

		return new Vec3d(x, y, z);
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

		PlayerEntry entry = this.getEntryFromPlayer(player);

		if (entry != null) {
			if (this.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)) {
				this.gameSpace.getPlayers().sendMessage(player.getDamageTracker().getDeathMessage());
			}

			if (!this.config.shouldKeepInventory()) {
				entry.dropInventory();
			}

			if (this.attemptBeaconRespawn(entry) == ActionResult.FAIL) {
				if (this.invulnerability > 0) {
					BeaconBreakersActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), entry.getPlayer());
				} else {
					if (this.config.shouldKeepInventory()) {
						entry.dropInventory();
					}

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

		for (TeamEntry entry : this.teams) {
			if (entry.getBeacon().isAt(pos)) {
				entry.setBeacon(BeaconPlacement.Broken.INSTANCE);

				this.gameSpace.getPlayers().playSound(SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 1);

				MutableText message = entry.getBeaconBreakMessage(breaker, explosion);
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
		TeamEntry team = entry.getTeam();

		if (team.getBeacon().isAt(pos)) {
			if (this.invulnerability > 0) {
				team.setBeacon(BeaconPlacement.Unplaced.INSTANCE);
				world.setBlockState(pos, state.getFluidState().getBlockState());

				entry.giveRespawnBeacon();
	
				this.sidebar.update();
			} else if (!this.config.shouldAllowSelfBreaking()) {
				player.sendMessage(team.getCannotBreakOwnBeaconMessage().formatted(Formatting.RED), false);
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

		TeamEntry team = entry.getTeam();

		if (!(team.getBeacon() instanceof BeaconPlacement.Unplaced)) return;
		if (!this.map.getBox().contains(pos)) {
			entry.sendMessage(Text.translatable("text.beaconbreakers.cannot_place_out_of_bounds_beacon").formatted(Formatting.RED), false);
			return;
		}

		team.setBeacon(new BeaconPlacement.Placed(pos));
		this.sidebar.update();
	}

	private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		BlockPos pos = hitResult.getBlockPos();
		BlockState state = this.world.getBlockState(pos);

		if (state.isIn(Main.RESPAWN_BEACONS)) {
			for (TeamEntry team : this.teams) {
				if (team.getBeacon().isAt(pos)) {
					PlayerEntry entry = this.getEntryFromPlayer(player);
					player.sendMessage(team.getTattleMessageFor(entry), true);

					return ActionResult.FAIL;
				}
			}
		}

		return ActionResult.PASS;
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
			if (!this.config.shouldAllowSelfBreaking() && entry != null && entry.getTeam().getBeacon().isAt(pos)) {
				iterator.remove();
				continue;
			}

			this.breakBeacon(entry, pos, true);
			world.setBlockState(pos, state.getFluidState().getBlockState());
		}
	}

	@SuppressWarnings("deprecation")
	private ActionResult onThrowItem(ServerPlayerEntity player, int slot, ItemStack stack) {
		if (stack.getItem() instanceof BlockItem blockItem) {
			RegistryEntry<Block> entry = blockItem.getBlock().getRegistryEntry();
			
			if (entry.isIn(Main.RESPAWN_BEACONS)) {
				return ActionResult.FAIL;
			}
		}

		return ActionResult.PASS;
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

	public Iterable<TeamEntry> getTeams() {
		return this.teams;
	}

	public static Vec3d getSpawnPos(ServerWorld world, BeaconBreakersMap map, BeaconBreakersMapConfig mapConfig, ServerPlayerEntity player) {
		int x = mapConfig.getX() * 8;
		int z = mapConfig.getZ() * 8;

		int bottomY = world.getBottomY();
		int maxY = Math.min(world.getTopY(), bottomY + world.getLogicalHeight()) - 1;

		BlockPos.Mutable pos = new BlockPos.Mutable(x, maxY, z);
		Chunk chunk = world.getChunk(pos);

		int air = 0;

		while (pos.getY() > bottomY) {
			if (chunk.getBlockState(pos).isAir()) {
				air += 1;
			} else if (air > EntityType.PLAYER.getHeight()) {
				air = 0;
				break;
			} else {
				air = 0;
			}

			pos.move(Direction.DOWN);
		}

		if (pos.getY() == bottomY) {
			pos.setY(chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z));
		}

		return new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
	}

	public static void spawn(ServerWorld world, BeaconBreakersMap map, BeaconBreakersMapConfig mapConfig, ServerPlayerEntity player) {
		Vec3d spawnPos = BeaconBreakersActivePhase.getSpawnPos(world, map, mapConfig, player);
		player.teleport(world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
	}
}
