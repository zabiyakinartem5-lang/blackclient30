package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.NumSetting;
import net.minecraft.client.MinecraftClient;

/** Сам нажимает «Возродиться» после смерти. Удобство, никакого преимущества. */
public class AutoRespawnFeature extends Feature {
	public final NumSetting delay = add(new NumSetting("Delay", 20, 0, 60, 5, " ticks"));

	private int timer;

	public AutoRespawnFeature() {
		super("Auto Respawn", "Respawn automatically after death", Category.MOVEMENT);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (!enabled || mc.player == null || !mc.player.isDead()) {
			timer = 0;
			return;
		}
		if (++timer >= (int) delay.value) {
			mc.player.requestRespawn();
			timer = 0;
		}
	}
}
