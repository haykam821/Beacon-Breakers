package io.github.haykam821.beaconbreakers.game;

import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class PlayerEntry {
	private static final Text UNPLACED_BEACON_ICON = PlayerEntry.createIcon("⌛", Formatting.YELLOW);
	private static final Text PLACED_BEACON_ICON = PlayerEntry.createIcon("✔", Formatting.GREEN);
	private static final Text NO_BEACON_ICON= PlayerEntry.createIcon("❌", Formatting.RED);

	private final ServerPlayerEntity player;
	private BlockPos beaconPos;
	private boolean beaconBroken = false;

	public PlayerEntry(ServerPlayerEntity player) {
		this.player = player;
	}

	public ServerPlayerEntity getPlayer() {
		return player;
	}

	public BlockPos getBeaconPos() {
		return this.beaconPos;
	}

	public void setBeaconPos(BlockPos beaconPos) {
		this.beaconPos = beaconPos;
	}

	public void setBeaconBroken() {
		this.beaconBroken = true;
	}

	public Text getSidebarEntryText() {
		return Text.empty().append(this.getSidebarEntryIcon()).append(ScreenTexts.SPACE).append(this.player.getEntityName());
	}

	public Text getSidebarEntryIcon() {
		if (this.beaconPos == null) {
			return PlayerEntry.UNPLACED_BEACON_ICON;
		} else if (this.beaconBroken) {
			return PlayerEntry.NO_BEACON_ICON;
		} else {
			return PlayerEntry.PLACED_BEACON_ICON;
		}
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.getPlayer() + ", beaconPos=" + this.getBeaconPos() + "}";
	}

	private static Text createIcon(String icon, Formatting color) {
		return Text.literal(icon).styled(style -> {
			return style.withColor(color).withBold(true);
		});
	}
}