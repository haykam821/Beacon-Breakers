package io.github.haykam821.beaconbreakers.game.player;

import java.util.Objects;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public sealed interface BeaconPlacement {
	public default boolean isAt(BlockPos pos) {
		return false;
	}

	public Text getSidebarEntryIcon();

	public record Unplaced() implements BeaconPlacement {
		public static final Unplaced INSTANCE = new Unplaced();

		private static final Text ICON = BeaconPlacement.createIcon("⌛", Formatting.YELLOW);

		@Override
		public Text getSidebarEntryIcon() {
			return ICON;
		}

		@Override
		public String toString() {
			return "Unplaced";
		}
	}

	public record Placed(BlockPos pos) implements BeaconPlacement {
		private static final Text ICON = BeaconPlacement.createIcon("✔", Formatting.GREEN);

		public Placed(BlockPos pos) {
			this.pos = Objects.requireNonNull(pos);
		}

		@Override
		public boolean isAt(BlockPos pos) {
			return this.pos.equals(pos);
		}

		@Override
		public Text getSidebarEntryIcon() {
			return ICON;
		}

		@Override
		public String toString() {
			return "Placed at " + this.pos;
		}
	}

	public record Broken() implements BeaconPlacement {
		public static final Broken INSTANCE = new Broken();

		private static final Text ICON = BeaconPlacement.createIcon("❌", Formatting.RED);

		@Override
		public Text getSidebarEntryIcon() {
			return ICON;
		}

		@Override
		public String toString() {
			return "Broken";
		}
	}

	private static Text createIcon(String icon, Formatting color) {
		return Text.literal(icon).styled(style -> {
			return style.withColor(color).withBold(true);
		});
	}
}
