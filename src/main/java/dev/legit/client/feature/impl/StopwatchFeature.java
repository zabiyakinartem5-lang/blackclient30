package dev.legit.client.feature.impl;

import dev.legit.client.Keys;
import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BindSetting;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;
import net.minecraft.client.MinecraftClient;

/** Секундомер на экране. Клавиши старт/стоп и сброс назначаются в меню. */
public class StopwatchFeature extends Feature {
	public final BindSetting startStop = add(new BindSetting("Start / Stop", -1));
	public final BindSetting reset = add(new BindSetting("Reset", -1));
	public final BoolSetting millis = add(new BoolSetting("Show milliseconds", false));
	public final NumSetting scale = add(new NumSetting("Scale", 1.0, 0.7, 1.6, 0.1, "x"));

	private boolean running;
	private long accumulated;
	private long startedAt;

	public StopwatchFeature() {
		super("Stopwatch", "On-screen stopwatch", Category.HUD);
	}

	public long elapsed() {
		return accumulated + (running ? System.currentTimeMillis() - startedAt : 0L);
	}

	public boolean running() {
		return running;
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (mc.currentScreen != null || mc.player == null) return;
		if (startStop.key > 0 && Keys.pressedOnce(startStop.key)) {
			if (running) accumulated += System.currentTimeMillis() - startedAt;
			else startedAt = System.currentTimeMillis();
			running = !running;
		}
		if (reset.key > 0 && Keys.pressedOnce(reset.key)) {
			running = false;
			accumulated = 0L;
		}
	}
}
