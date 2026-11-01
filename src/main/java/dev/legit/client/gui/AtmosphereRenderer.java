package dev.legit.client.gui;

import dev.legit.client.feature.Features;
import dev.legit.client.feature.impl.AtmosphereFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class AtmosphereRenderer {
	private AtmosphereRenderer() {}

	public static void draw(DrawContext c, int w, int h) {
		AtmosphereFeature f = Features.ATMOSPHERE;
		if (!f.enabled) return;

		int tintA = (int) (f.tint.value / 100.0 * 255.0 * 0.55);
		if (tintA > 0) {
			int rgb = Gfx.hsb((float) (f.hue.value / 360.0), 0.75f, 1f) & 0xFFFFFF;
			c.fill(0, 0, w, h, (tintA << 24) | rgb);
		}
		vignette(c, w, h, 0x000000, f.vignette.value / 100.0);
	}

	/** Красная пульсация по краям, когда здоровья мало (модуль Alerts). */
	public static void drawLowHealth(DrawContext c, MinecraftClient mc, int w, int h) {
		var a = Features.ALERTS;
		if (!a.enabled || !a.pulse.value || mc.player == null) return;
		float hp = mc.player.getHealth();
		float limit = (float) (a.healthHearts.value * 2.0);
		if (hp <= 0f || hp > limit) return;

		float intensity = 1f - hp / limit;
		double pulse = 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 1000.0 * 4.0);
		vignette(c, w, h, 0xFF2020, Math.min(1.0, intensity * pulse * 1.1 + 0.15));
	}

	private static void vignette(DrawContext c, int w, int h, int rgb, double v) {
		if (v <= 0) return;
		int steps = 20;
		int sx = Math.max(2, w / (steps * 6));
		int sy = Math.max(2, h / (steps * 6));
		for (int i = 0; i < steps; i++) {
			float k = 1f - i / (float) steps;
			int a = (int) (v * 0.7 * 255 * k * k);
			if (a <= 0) continue;
			int col = (Math.min(255, a) << 24) | rgb;
			c.fill(i * sx, 0, (i + 1) * sx, h, col);
			c.fill(w - (i + 1) * sx, 0, w - i * sx, h, col);
			c.fill(0, i * sy, w, (i + 1) * sy, col);
			c.fill(0, h - (i + 1) * sy, w, h - i * sy, col);
		}
	}
}
