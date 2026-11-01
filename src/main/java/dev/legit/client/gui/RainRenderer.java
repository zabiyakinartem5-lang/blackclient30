package dev.legit.client.gui;

import dev.legit.client.feature.Features;
import dev.legit.client.feature.impl.RainFeature;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Дождь поверх экрана: наклонные чёрточки, падающие вниз. */
public final class RainRenderer {
	private static final int MAX = 300;
	private static final float[] X = new float[MAX];
	private static final float[] Y = new float[MAX];
	private static final float[] V = new float[MAX];
	private static boolean init;
	private static long last = System.nanoTime();

	private RainRenderer() {}

	public static void draw(DrawContext c, int w, int h) {
		RainFeature f = Features.RAIN;
		if (!init) {
			Random r = new Random(11);
			for (int i = 0; i < MAX; i++) {
				X[i] = r.nextFloat();
				Y[i] = r.nextFloat();
				V[i] = r.nextFloat() * 0.5f;
			}
			init = true;
		}

		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
		last = now;

		int n = Math.min(MAX, (int) f.amount.value);
		float speed = (float) f.speed.value;
		float wind = (float) f.wind.value;
		int len = (int) f.length.value;
		int slant = wind > 0.5f ? 1 : wind < -0.5f ? -1 : 0;

		for (int i = 0; i < n; i++) {
			Y[i] += dt * (0.9f + V[i]) * speed * 0.25f;
			X[i] += dt * wind * 0.012f;
			if (Y[i] > 1.05f) {
				Y[i] = -0.05f;
				X[i] = (X[i] + 0.41f) % 1f;
			}
			if (X[i] < 0f) X[i] += 1f;
			if (X[i] > 1f) X[i] -= 1f;

			int px = (int) (X[i] * w), py = (int) (Y[i] * h);
			int a = (int) (60 + 110 * V[i]);
			int col = (a << 24) | 0xAFC8FF;
			for (int k = 0; k < len; k++) {
				int x = px + (slant * k) / 3;
				c.fill(x, py + k, x + 1, py + k + 1, col);
			}
		}
	}
}
