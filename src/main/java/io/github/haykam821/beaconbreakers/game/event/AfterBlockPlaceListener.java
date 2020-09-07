package io.github.haykam821.beaconbreakers.game.event;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.event.EventType;

public interface AfterBlockPlaceListener {
	public EventType<AfterBlockPlaceListener> EVENT = EventType.create(AfterBlockPlaceListener.class, listeners -> {
		return (pos, world, player, stack, state) -> {
			for (AfterBlockPlaceListener listener : listeners) {
				listener.afterBlockPlace(pos, world, player, stack, state);
			}
		};
	});

	public void afterBlockPlace(BlockPos pos, World world, ServerPlayerEntity player, ItemStack stack, BlockState state);
}