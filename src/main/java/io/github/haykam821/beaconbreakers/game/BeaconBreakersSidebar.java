package io.github.haykam821.beaconbreakers.game;

import io.github.haykam821.beaconbreakers.game.phase.BeaconBreakersActivePhase;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;

public class BeaconBreakersSidebar {
	private final SidebarWidget widget;
	private final BeaconBreakersActivePhase phase;

	public BeaconBreakersSidebar(GlobalWidgets widgets, BeaconBreakersActivePhase phase) {
		Text name = new TranslatableText("gameType.beaconbreakers.beacon_breakers").styled(style -> {
			return style.withBold(true);
		});
		this.widget = widgets.addSidebar(name);

		this.phase = phase;
	}

	public void update() {
		this.widget.set(content -> {
			for (PlayerEntry player : this.phase.getPlayers()) {
				content.add(player.getSidebarEntryText());
			}
		});
	}
}
