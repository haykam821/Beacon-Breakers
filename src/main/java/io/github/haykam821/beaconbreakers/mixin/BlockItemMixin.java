package io.github.haykam821.beaconbreakers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.haykam821.beaconbreakers.game.event.AfterBlockPlaceListener;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.GameWorld;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	@Inject(method = "postPlacement", at = @At("HEAD"))
	private void invokeAfterBlockPlaceListeners(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state, CallbackInfoReturnable<Boolean> ci) {
		if (world.isClient) return;

		GameWorld gameWorld = GameWorld.forWorld(world);
		if (gameWorld == null) return;
		if (!gameWorld.containsEntity(player)) return;
		
		gameWorld.invoker(AfterBlockPlaceListener.EVENT).afterBlockPlace(pos, world, (ServerPlayerEntity) player, stack, state);
	}
}