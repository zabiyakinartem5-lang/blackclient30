package dev.legit.client.gui;

import dev.legit.client.feature.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Всплывающие уведомления справа внизу: выезжают, висят пару секунд и уезжают обратно. */
public final class Toasts {
	private static final class Toast {
		String text;
		int color;
		float age;
		float anim;
	}

	private static final List<Toast> LIST = new ArrayList<>();
	private static final float LIFE = 2.8f;

	/** Включается на время загрузки конфига и профилей, чтобы не было лавины уведомлений. */
	public static boolean mute;

	private Toasts() {}

	public static void push(String text, int color) {
		if (mute || !Features.THEME.toasts.value) return;
		Toast t = new Toast();
		t.text = text;
		t.color = color;
		LIST.add(t);
		if (LIST.size() > 6) LIST.remove(0);
	}

	public static void render(DrawContext c, MinecraftClient mc, float dt, int sw, int sh) {
		if (LIST.isEmpty()) return;
		var tr = mc.textRenderer;
		float k = Gfx.animK(dt, 12f);

		int idx = 0;
		Iterator<Toast> it = LIST.iterator();
		while (it.hasNext()) {
			Toast t = it.next();
			t.age += dt;
			float target = t.age < LIFE ? 1f : 0f;
			t.anim += (target - t.anim) * k;
			if (target == 0f && t.anim < 0.03f) {
				it.remove();
				continue;
			}
			float ease = Gfx.easeOut(Math.min(1f, t.anim));
			int w = tr.getWidth(t.text) + 24;
			int h = 18;
			int x = sw - w - 8 + Math.round((1f - ease) * (w + 12));
			int y = sh - 58 - idx * (h + 4);

			Gfx.rrect(c, x, y, w, h, 5, Gfx.withAlpha(Gfx.hudBg() | 0xFF000000, ease * 0.95f));
			c.fill(x + 3, y + 4, x + 5, y + h - 4, Gfx.withAlpha(t.color, ease));
			c.drawText(tr, t.text, x + 10, y + 5, Gfx.withAlpha(Gfx.text(), ease), true);
			idx++;
		}
	}
}
