package dev.legit.client.gui;

import dev.legit.client.feature.Features;
import dev.legit.client.feature.impl.SnowFeature;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Частицы поверх экрана: снег, дождь, лепестки, угли. Простые квадратики и линии. */
public final class SnowRenderer {
	private static final int MAX = 400;
	private static final float[] X = new float[MAX];
	private static final float[] Y = new float[MAX];
	private static final float[] V = new float[MAX];
	private static final float[] P = new float[MAX];
	private static final float[] S = new float[MAX];
	private static boolean init;
	private static long last = System.nanoTime();

	private SnowRenderer() {}

	public static void draw(DrawContext c, int w, int h) {
		SnowFeature f = Features.SNOW;
		if (!init) {
			Random r = new Random(7);
			for (int i = 0; i < MAX; i++) {
				X[i] = r.nextFloat();
				Y[i] = r.nextFloat();
				V[i] = r.nextFloat() * 0.06f;
				P[i] = r.nextFloat() * 6.2832f;
				S[i] = 0.5f + r.nextFloat() * 0.5f;
			}
			init = true;
		}

		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
		last = now;
		float time = now / 1_000_000_000f;

		int type = (int) Math.round(f.type.value);
		int n = Math.min(MAX, (int) f.amount.value);
		float speed = (float) f.speed.value;
		float size = (float) f.size.value;
		float wind = (float) f.wind.value;
		float op = (float) (f.opacity.value / 100.0);
		int rgb = switch (type) {
			case 1 -> 0xAFC8FF;
			case 2 -> 0xFFB7D5;
			case 3 -> 0xFF9A3C;
			default -> 0xFFFFFF;
		};

		for (int i = 0; i < n; i++) {
			boolean rising = type == 3;
			if (type == 1) {
				Y[i] += dt * (0.9f + V[i] * 8f) * speed * 0.35f;
				X[i] += dt * wind * 0.02f;
			} else if (rising) {
				Y[i] -= dt * (0.05f + V[i]) * speed * 0.30f;
				X[i] += (float) Math.sin(time * 1.3f + P[i]) * 0.02f * dt + dt * wind * 0.01f;
			} else {
				float fall = type == 2 ? 0.8f : 1f;
				float sway = type == 2 ? 2.5f : 1f;
				Y[i] += dt * (0.05f + V[i]) * speed * 0.35f * fall;
				X[i] += (float) Math.sin(time * 0.9f + P[i]) * 0.012f * sway * dt + dt * wind * 0.01f;
			}

			if (!rising && Y[i] > 1.02f) {
				Y[i] = -0.02f;
				X[i] = (X[i] + 0.37f) % 1f;
			}
			if (rising && Y[i] < -0.02f) {
				Y[i] = 1.02f;
				X[i] = (X[i] + 0.37f) % 1f;
			}
			if (X[i] < 0f) X[i] += 1f;
			if (X[i] > 1f) X[i] -= 1f;

			int px = (int) (X[i] * w), py = (int) (Y[i] * h);
			float alpha = (90 + 140 * S[i]) * op;
			if (rising) alpha *= 0.6f + 0.4f * (float) Math.sin(time * 6f + P[i]);
			int a = Math.max(0, Math.min(255, (int) alpha));
			int col = (a << 24) | rgb;

			if (type == 1) {
				int len = 5 + (int) (S[i] * size * 3f);
				float slope = wind * 0.15f;
				for (int k = 0; k < len; k++) {
					int qx = px + Math.round(k * slope);
					c.fill(qx, py + k, qx + 1, py + k + 1, col);
				}
			} else {
				int s = Math.max(1, Math.round(S[i] * size));
				c.fill(px, py, px + s, py + s, col);
			}
		}
	}
}
