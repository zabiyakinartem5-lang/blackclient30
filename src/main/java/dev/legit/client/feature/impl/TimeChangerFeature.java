package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;
import net.minecraft.client.MinecraftClient;

import java.time.LocalTime;

/** Меняет время суток только на вашем экране. Сервер и другие игроки ничего не замечают. */
public class TimeChangerFeature extends Feature {
	public final BoolSetting realTime = add(new BoolSetting("Real time", false));
	public final NumSetting time = add(new NumSetting("Time", 6000, 0, 24000, 500, ""));

	public TimeChangerFeature() {
		super("Time Changer", "Client-side time of day (0 dawn, 6000 noon, 18000 night)", Category.VISUAL);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (!enabled || mc.world == null) return;
		long t;
		if (realTime.value) {
			LocalTime n = LocalTime.now();
			double day = (n.getHour() * 3600 + n.getMinute() * 60 + n.getSecond()) / 86400.0;
			// в Minecraft 0 тиков = 6:00, поэтому полдень по часам даёт 6000
			t = ((long) (day * 24000.0) + 18000L) % 24000L;
		} else {
			t = (long) time.value;
		}
		mc.world.getLevelProperties().setTimeOfDay(t);
	}
}
