package dev.legit.client.gui;

import dev.legit.client.data.Friends;
import dev.legit.client.data.Waypoints;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.impl.StaffListFeature;
import dev.legit.client.feature.Features;
import dev.legit.client.feature.impl.HudFeature;
import dev.legit.client.feature.impl.KeystrokesFeature;
import dev.legit.client.gui.hud.HudElement;
import dev.legit.client.gui.hud.HudElements;
import dev.legit.client.pvp.PvpStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Рисует все блоки HUD. Каждый блок рисуется в своей системе координат (0,0 - левый верхний угол блока). */
public final class HudRenderer {
	private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>();
	private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>();
	private static boolean leftDown, rightDown;

	// анимации клавиш: W A S D LMB RMB SPACE
	private static final float[] KEY_ANIM = new float[7];
	private static long lastFrame = System.nanoTime();

	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

	private static final EquipmentSlot[] ARMOR = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private static int BG = 0xB0101018;

	/** Быстро скрыть весь HUD клиента клавишей из меню (Keybinds → Toggle HUD). */
	public static boolean hidden;

	private HudRenderer() {}

	public static void render(DrawContext c, MinecraftClient mc) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		if (mc.player == null) return;
		if (hidden) return;

		int sw = mc.getWindow().getScaledWidth();
		int sh = mc.getWindow().getScaledHeight();

		AtmosphereRenderer.draw(c, sw, sh);
		AtmosphereRenderer.drawLowHealth(c, mc, sw, sh);
		if (Features.SNOW.enabled && Features.SNOW.screen.value) SnowRenderer.draw(c, sw, sh);

		if (mc.options.hudHidden) return;
		pollClicks(mc);

		for (HudElement el : HudElements.ALL) {
			if (!el.visible.getAsBoolean() || el.hidden) {
				el.vis = 0f;
				el.shown = false;
				continue;
			}

			float s = (float) el.scaleOf.getAsDouble() * el.scaleMul;
			el.scale = s;

			// появление: фон проявляется, блок слегка вырастает и опускается на место
			el.vis += ((el.shown ? 1f : 0f) - el.vis) * Gfx.animK(dt, 12f);
			float ease = Gfx.easeOut(el.vis);
			float pop = 0.9f + 0.1f * ease;
			BG = (((int) (Features.THEME.hudAlpha() * ease * el.bgMul)) << 24) | Features.THEME.panelRgb(0.9f);

			MatrixStack m = c.getMatrices();
			m.push();
			m.translate(el.left(sw), el.top(sh) + (1f - ease) * 6f, 0f);
			m.scale(s * pop, s * pop, 1f);
			if (el.customColor) {
				Gfx.overrideA = Gfx.hsb(el.hue / 360f, 0.75f, 1f);
				Gfx.overrideB = Gfx.hsb((el.hue + 40f) / 360f, 0.75f, 1f);
			}
			int[] size = drawElement(el, c, mc, dt);
			if (size != null && el.outline) {
				int ow = size[0], oh = size[1], oc = Gfx.withAlpha(Gfx.accentA(), ease);
				c.fill(0, 0, ow, 1, oc);
				c.fill(0, oh - 1, ow, oh, oc);
				c.fill(0, 0, 1, oh, oc);
				c.fill(ow - 1, 0, ow, oh, oc);
			}
			Gfx.overrideA = 0;
			Gfx.overrideB = 0;
			m.pop();

			el.shown = size != null;
			if (size != null) {
				el.w = size[0];
				el.h = size[1];
			}
		}

		Toasts.render(c, mc, dt, sw, sh);
	}

	private static int[] drawElement(HudElement el, DrawContext c, MinecraftClient mc, float dt) {
		if (el == HudElements.INFO) return drawInfo(c, mc, Features.HUD);
		if (el == HudElements.ARMOR) return drawArmor(c, mc);
		if (el == HudElements.EFFECTS) return drawEffects(c, mc);
		if (el == HudElements.KEYSTROKES) return drawKeystrokes(c, mc, Features.KEYSTROKES, dt);
		if (el == HudElements.MODULE_LIST) return drawModuleList(c, mc, dt);
		if (el == HudElements.WATERMARK) return drawWatermark(c, mc);
		if (el == HudElements.TARGET) return drawTarget(c, mc);
		if (el == HudElements.COMBO) return drawCombo(c, mc);
		if (el == HudElements.ITEMS) return drawItems(c, mc);
		if (el == HudElements.STATUS) return drawStatus(c, mc);
		if (el == HudElements.DEATH) return drawDeath(c, mc);
		if (el == HudElements.INVENTORY) return drawInventory(c, mc);
		if (el == HudElements.STAFF) return drawStaff(c, mc);
		if (el == HudElements.FRIENDS) return drawFriends(c, mc);
		if (el == HudElements.MARKS) return drawMarks(c, mc);
		if (el == HudElements.COMPASS) return drawCompass(c, mc);
		if (el == HudElements.HELD) return drawHeld(c, mc);
		if (el == HudElements.GRAPH) return drawGraph(c, mc, dt);
		if (el == HudElements.STOPWATCH) return drawStopwatch(c, mc);
		return null;
	}

	// ------------------------------------------------------------------ frame graph / stopwatch

	private static final float[] FRAMES = new float[120];
	private static int frameIdx;

	private static int[] drawGraph(DrawContext c, MinecraftClient mc, float dt) {
		var g = Features.FRAME_GRAPH;
		FRAMES[frameIdx] = dt * 1000f;
		frameIdx = (frameIdx + 1) % FRAMES.length;

		int w = (int) g.width.value, h = (int) g.height.value;
		int top = g.showFps.value ? 12 : 0;
		int total = h + top + 4;
		Gfx.rrect(c, 0, 0, w, total, 5, BG);
		if (g.showFps.value) c.drawText(mc.textRenderer, mc.getCurrentFps() + " fps", 5, 3, Gfx.text(), true);

		float limit = (float) g.limit.value;
		int bars = Math.min(w - 6, FRAMES.length);
		for (int i = 0; i < bars; i++) {
			int idx = ((frameIdx - bars + i) % FRAMES.length + FRAMES.length) % FRAMES.length;
			float ms = FRAMES[idx];
			float f = Math.max(0.02f, Math.min(1f, ms / limit));
			int bh = Math.max(1, (int) (f * h));
			int col = f > 0.75f ? 0xFFEF4444 : f > 0.45f ? 0xFFFACC15 : Gfx.accentB();
			c.fill(3 + i, top + 2 + h - bh, 4 + i, top + 2 + h, col);
		}
		return new int[]{w, total};
	}

	private static int[] drawStopwatch(DrawContext c, MinecraftClient mc) {
		var sw = Features.STOPWATCH;
		long ms = sw.elapsed();
		long totalSec = ms / 1000L;
		String t = String.format(java.util.Locale.ROOT, "%02d:%02d", totalSec / 60L, totalSec % 60L);
		if (sw.millis.value) t += String.format(java.util.Locale.ROOT, ".%03d", ms % 1000L);
		var tr = mc.textRenderer;
		int w = tr.getWidth(t) + 22;
		Gfx.rrect(c, 0, 0, w, 16, 5, BG);
		Gfx.rrect(c, 5, 5, 6, 6, 3, sw.running() ? 0xFF4ADE80 : Gfx.OFF);
		c.drawText(tr, t, 15, 4, Gfx.text(), true);
		return new int[]{w, 16};
	}

	// ------------------------------------------------------------------ info panel

	private static int[] drawInfo(DrawContext c, MinecraftClient mc, HudFeature h) {
		List<String[]> rows = new ArrayList<>();
		if (h.fps.value) rows.add(new String[]{"FPS", String.valueOf(mc.getCurrentFps())});
		if (h.coords.value) {
			rows.add(new String[]{"XYZ", mc.player.getBlockX() + "  " + mc.player.getBlockY() + "  " + mc.player.getBlockZ()});
		}
		if (h.biome.value && mc.world != null) {
			String b = mc.world.getBiome(mc.player.getBlockPos()).getKey()
				.map(k -> k.getValue().getPath()).orElse("?").replace('_', ' ');
			rows.add(new String[]{"BIOME", b});
		}
		if (h.cps.value) rows.add(new String[]{"CPS", LEFT_CLICKS.size() + " | " + RIGHT_CLICKS.size()});
		if (h.ping.value) rows.add(new String[]{"PING", ping(mc)});
		if (h.speed.value) {
			double bps = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20.0;
			rows.add(new String[]{"SPEED", String.format(java.util.Locale.ROOT, "%.1f b/s", bps)});
		}
		if (h.direction.value) {
			String d = mc.player.getHorizontalFacing().asString();
			rows.add(new String[]{"DIR", Character.toUpperCase(d.charAt(0)) + d.substring(1)});
		}
		if (h.clock.value) rows.add(new String[]{"TIME", LocalTime.now().format(CLOCK)});
		if (h.server.value) {
			var se = mc.getCurrentServerEntry();
			rows.add(new String[]{"IP", se != null ? se.address : "singleplayer"});
		}
		if (h.day.value && mc.world != null) {
			rows.add(new String[]{"DAY", String.valueOf(mc.world.getTimeOfDay() / 24000L + 1)});
		}
		if (h.light.value && mc.world != null) {
			rows.add(new String[]{"LIGHT", String.valueOf(mc.world.getLightLevel(mc.player.getBlockPos()))});
		}
		if (h.nether.value) {
			boolean inNether = mc.world != null && mc.world.getRegistryKey().getValue().getPath().equals("the_nether");
			double f = inNether ? 8.0 : 1.0 / 8.0;
			rows.add(new String[]{inNether ? "OVERW" : "NETHER",
				(int) Math.floor(mc.player.getX() * f) + "  " + mc.player.getBlockY() + "  " + (int) Math.floor(mc.player.getZ() * f)});
		}
		if (h.playtime.value) {
			long sec = (System.currentTimeMillis() - dev.legit.client.LegitClient.START) / 1000L;
			rows.add(new String[]{"PLAY", String.format(Locale.ROOT, "%d:%02d:%02d", sec / 3600, sec / 60 % 60, sec % 60)});
		}
		if (h.ram.value) {
			Runtime rt = Runtime.getRuntime();
			long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
			rows.add(new String[]{"RAM", used + "/" + rt.maxMemory() / 1048576L + " MB"});
		}
		if (rows.isEmpty()) return null;

		var tr = mc.textRenderer;
		int labelW = 0, valueW = 0;
		for (String[] r : rows) {
			labelW = Math.max(labelW, tr.getWidth(r[0]));
			valueW = Math.max(valueW, tr.getWidth(r[1]));
		}
		int pad = 6, rowH = 12, gap = 8;
		int w = pad + labelW + gap + valueW + pad + 3;
		int hgt = pad + rows.size() * rowH - 2 + pad - 2;

		Gfx.rrect(c, 0, 0, w, hgt, 5, BG);
		c.fill(2, 5, 4, hgt - 5, Gfx.accentA());
		c.fill(2, hgt / 2, 4, hgt - 5, Gfx.accentB());

		int ty = pad - 1;
		for (String[] r : rows) {
			c.drawText(tr, r[0], pad + 3, ty, Gfx.accentB(), true);
			c.drawText(tr, r[1], pad + 3 + labelW + gap, ty, Gfx.text(), true);
			ty += rowH;
		}
		return new int[]{w, hgt};
	}

	private static String ping(MinecraftClient mc) {
		if (mc.getNetworkHandler() == null) return "-";
		PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
		return e == null ? "-" : e.getLatency() + " ms";
	}

	// ------------------------------------------------------------------ armor

	private static int[] drawArmor(DrawContext c, MinecraftClient mc) {
		var tr = mc.textRenderer;
		int itemH = 20;
		int panelW = 52, panelH = ARMOR.length * itemH + 8;

		boolean any = false;
		for (EquipmentSlot slot : ARMOR) if (!mc.player.getEquippedStack(slot).isEmpty()) any = true;
		if (!any) return null;

		Gfx.rrect(c, 0, 0, panelW, panelH, 5, BG);
		int iy = 4;
		for (EquipmentSlot slot : ARMOR) {
			ItemStack st = mc.player.getEquippedStack(slot);
			if (!st.isEmpty()) {
				c.drawItem(st, 5, iy + 1);
				if (st.isDamageable()) {
					int left = st.getMaxDamage() - st.getDamage();
					int pct = Math.round(left * 100f / st.getMaxDamage());
					int col = Gfx.lerp(0xFFEF4444, 0xFF4ADE80, pct / 100f);
					c.drawText(tr, pct + "%", 25, iy + 5, col, true);
				}
			}
			iy += itemH;
		}
		return new int[]{panelW, panelH};
	}

	// ------------------------------------------------------------------ potion effects

	private static final java.util.Map<Object, Integer> EFFECT_MAX = new java.util.HashMap<>();

	private static int[] drawEffects(DrawContext c, MinecraftClient mc) {
		var effects = mc.player.getStatusEffects();
		if (effects.isEmpty()) {
			EFFECT_MAX.clear();
			return null;
		}
		EFFECT_MAX.keySet().removeIf(k -> effects.stream().noneMatch(x -> x.getEffectType().equals(k)));

		boolean bar = Features.EFFECTS.bar.value;
		var tr = mc.textRenderer;
		int rowH = bar ? 19 : 15;
		int y = 0, maxW = 0;
		for (StatusEffectInstance e : effects) {
			Object key = e.getEffectType();
			if (!e.isInfinite()) EFFECT_MAX.merge(key, e.getDuration(), Math::max);
			int max = Math.max(1, EFFECT_MAX.getOrDefault(key, 1));
			float frac = e.isInfinite() ? 1f : Math.max(0f, Math.min(1f, e.getDuration() / (float) max));

			String name = e.getEffectType().value().getName().getString();
			if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
			String time = e.isInfinite() ? "inf" : formatTicks(e.getDuration());

			int nameW = tr.getWidth(name), timeW = tr.getWidth(time);
			int w = 8 + nameW + 10 + timeW + 6;
			maxW = Math.max(maxW, w);
			boolean good = e.getEffectType().value().isBeneficial();
			int dot = good ? 0xFF4ADE80 : 0xFFEF4444;

			Gfx.rrect(c, 0, y, w, rowH - 2, 4, BG);
			c.fill(3, y + 3, 5, y + 11, dot);
			c.drawText(tr, name, 8, y + 3, Gfx.text(), true);
			boolean low = !e.isInfinite() && e.getDuration() < 200;
			c.drawText(tr, time, 8 + nameW + 10, y + 3, low ? 0xFFEF4444 : Gfx.accentB(), true);
			if (bar) {
				Gfx.rrect(c, 5, y + 13, w - 10, 2, 1, Gfx.OFF);
				Gfx.rrect(c, 5, y + 13, Math.max(2f, (w - 10) * frac), 2, 1, low ? 0xFFEF4444 : dot);
			}
			y += rowH;
		}
		return new int[]{maxW, y};
	}

	private static String formatTicks(int ticks) {
		int total = ticks / 20;
		return String.format(java.util.Locale.ROOT, "%d:%02d", total / 60, total % 60);
	}

	// ------------------------------------------------------------------ keystrokes

	private static int[] drawKeystrokes(DrawContext c, MinecraftClient mc, KeystrokesFeature k, float dt) {
		var o = mc.options;
		int K = 22, G = 2;
		boolean mouse = k.mouse.value, space = k.space.value;

		int totalH = K + G + K;
		if (mouse) totalH += G + K;
		if (space) totalH += G + 14;
		int fullW = 3 * K + 2 * G;

		int y = 0;
		drawKey(c, mc, K + G, y, K, K, "W", null, 0, o.forwardKey.isPressed(), dt);
		y += K + G;
		drawKey(c, mc, 0, y, K, K, "A", null, 1, o.leftKey.isPressed(), dt);
		drawKey(c, mc, K + G, y, K, K, "S", null, 2, o.backKey.isPressed(), dt);
		drawKey(c, mc, 2 * (K + G), y, K, K, "D", null, 3, o.rightKey.isPressed(), dt);
		y += K + G;
		if (mouse) {
			int mw = (fullW - G) / 2;
			drawKey(c, mc, 0, y, mw, K, "LMB", String.valueOf(LEFT_CLICKS.size()), 4, leftDown, dt);
			drawKey(c, mc, mw + G, y, mw, K, "RMB", String.valueOf(RIGHT_CLICKS.size()), 5, rightDown, dt);
			y += K + G;
		}
		if (space) {
			drawKey(c, mc, 0, y, fullW, 14, "SPACE", null, 6, o.jumpKey.isPressed(), dt);
		}
		return new int[]{fullW, totalH};
	}

	private static void drawKey(DrawContext c, MinecraftClient mc, int x, int y, int w, int h,
								String label, String sub, int idx, boolean pressed, float dt) {
		KEY_ANIM[idx] += ((pressed ? 1f : 0f) - KEY_ANIM[idx]) * Gfx.animK(dt, 18f);
		float a = KEY_ANIM[idx];

		int bg = Gfx.lerp(BG, (0xE0 << 24) | (Gfx.accentA() & 0xFFFFFF), a);
		Gfx.rrect(c, x, y, w, h, 4, bg);
		if (a > 0.05f) {
			c.fill(x + 3, y + h - 3, x + w - 3, y + h - 2, Gfx.withAlpha(Gfx.accentB(), a));
		}

		var tr = mc.textRenderer;
		int col = Gfx.lerp(Gfx.textDim(), 0xFFFFFFFF, a);
		int lx = x + (w - tr.getWidth(label)) / 2;
		if (sub == null) {
			c.drawText(tr, label, lx, y + (h - 8) / 2, col, true);
		} else {
			c.drawText(tr, label, lx, y + 3, col, true);
			c.drawText(tr, sub, x + (w - tr.getWidth(sub)) / 2, y + 12, Gfx.lerp(Gfx.textDim(), Gfx.accentB(), a), true);
		}
	}

	// ------------------------------------------------------------------ watermark / module list

	private static int[] drawWatermark(DrawContext c, MinecraftClient mc) {
		var tr = mc.textRenderer;
		var wm = Features.WATERMARK;
		String a = "Black";
		String b = " Client";
		StringBuilder sb = new StringBuilder();
		if (wm.showFps.value) sb.append("  |  ").append(mc.getCurrentFps()).append(" fps");
		if (wm.showPing.value) sb.append("  |  ").append(ping(mc));
		if (wm.showClock.value) sb.append("  |  ").append(LocalTime.now().format(CLOCK));
		String extra = sb.toString();
		int w = tr.getWidth(a) + tr.getWidth(b) + tr.getWidth(extra) + 14;

		Gfx.rrect(c, 0, 0, w, 16, 5, BG);
		Gfx.gradientBar(c, 6, 14, w - 12, 1, 1f);
		int tx = 7;
		c.drawText(tr, a, tx, 4, Gfx.accentA(), true);
		tx += tr.getWidth(a);
		c.drawText(tr, b, tx, 4, Gfx.text(), true);
		tx += tr.getWidth(b);
		if (!extra.isEmpty()) c.drawText(tr, extra, tx, 4, Gfx.textDim(), true);
		return new int[]{w, 16};
	}

	private static final java.util.Map<String, Float> ML_ANIM = new java.util.LinkedHashMap<>();

	private static int[] drawModuleList(DrawContext c, MinecraftClient mc, float dt) {
		var tr = mc.textRenderer;
		java.util.Set<String> on = new java.util.HashSet<>();
		for (Feature f : Features.ALL) {
			if (f.enabled && !f.alwaysOn && f.category != dev.legit.client.feature.Category.CLIENT
				&& f != Features.MODULE_LIST && f != Features.WATERMARK) on.add(f.name);
		}
		for (String n : on) ML_ANIM.putIfAbsent(n, 0f);

		float k = Gfx.animK(dt, 10f);
		var it = ML_ANIM.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			float target = on.contains(en.getKey()) ? 1f : 0f;
			float v = en.getValue() + (target - en.getValue()) * k;
			if (target == 0f && v < 0.03f) {
				it.remove();
				continue;
			}
			en.setValue(v);
		}
		if (ML_ANIM.isEmpty()) return null;

		List<String> names = new ArrayList<>(ML_ANIM.keySet());
		names.sort((p, q) -> tr.getWidth(q) - tr.getWidth(p));

		int maxW = tr.getWidth(names.get(0)) + 10;
		int rowH = 13;
		int y = 0;
		int n = names.size();
		for (int i = 0; i < n; i++) {
			String name = names.get(i);
			float ease = Gfx.easeOut(Math.min(1f, ML_ANIM.get(name)));
			int w = tr.getWidth(name) + 10;
			int x = maxW - w + Math.round((1f - ease) * (w + 8));
			int col = Gfx.lerp(Gfx.accentA(), Gfx.accentB(), n == 1 ? 0f : (float) i / (n - 1));
			Gfx.rrect(c, x, y, w, rowH - 1, 3, Gfx.withAlpha(BG, ease));
			c.fill(x + w - 2, y + 2, x + w, y + rowH - 3, Gfx.withAlpha(col, ease));
			c.drawText(tr, name, x + 4, y + 2, Gfx.withAlpha(col, ease), true);
			y += rowH;
		}
		return new int[]{maxW, y};
	}

	// ------------------------------------------------------------------ status / death point

	private static int[] drawStatus(DrawContext c, MinecraftClient mc) {
		String t;
		int col;
		if (mc.player.isSneaking()) {
			t = "Sneaking";
			col = Gfx.accentB();
		} else if (mc.player.isSprinting()) {
			t = "Sprinting";
			col = Gfx.accentA();
		} else {
			t = "Walking";
			col = Gfx.textDim();
		}
		var tr = mc.textRenderer;
		int w = tr.getWidth(t) + 16;
		Gfx.rrect(c, 0, 0, w, 14, 4, BG);
		c.fill(3, 3, 5, 11, col);
		c.drawText(tr, t, 9, 3, col, true);
		return new int[]{w, 14};
	}

	private static int[] drawDeath(DrawContext c, MinecraftClient mc) {
		if (!dev.legit.client.feature.impl.DeathPointFeature.has) return null;
		int x = dev.legit.client.feature.impl.DeathPointFeature.x;
		int y = dev.legit.client.feature.impl.DeathPointFeature.y;
		int z = dev.legit.client.feature.impl.DeathPointFeature.z;
		String dim = dev.legit.client.feature.impl.DeathPointFeature.dim;

		String l1 = "Last death";
		String l2 = x + "  " + y + "  " + z;
		String l3 = dim.replace('_', ' ');
		if (mc.world != null && mc.world.getRegistryKey().getValue().getPath().equals(dim)) {
			double dist = Math.sqrt(mc.player.squaredDistanceTo(x + 0.5, y, z + 0.5));
			l3 = (int) dist + " m";
		}

		var tr = mc.textRenderer;
		int w = Math.max(tr.getWidth(l1), Math.max(tr.getWidth(l2), tr.getWidth(l3))) + 16;
		int h = 38;
		Gfx.rrect(c, 0, 0, w, h, 5, BG);
		c.fill(2, 5, 4, h - 5, 0xFFEF4444);
		c.drawText(tr, l1, 9, 5, 0xFFEF4444, true);
		c.drawText(tr, l2, 9, 15, Gfx.text(), true);
		c.drawText(tr, l3, 9, 25, Gfx.textDim(), true);
		return new int[]{w, h};
	}

	// ------------------------------------------------------------------ inventory / staff / friends / marks

	private static int[] drawInventory(DrawContext c, MinecraftClient mc) {
		boolean hotbar = Features.INVENTORY_HUD.hotbar.value;
		int rows = hotbar ? 4 : 3;
		int w = 9 * 18 + 8, h = rows * 18 + 8;
		Gfx.rrect(c, 0, 0, w, h, 5, BG);
		var inv = mc.player.getInventory();
		var tr = mc.textRenderer;
		for (int r = 0; r < rows; r++) {
			for (int col = 0; col < 9; col++) {
				// строки 0..2 - основной инвентарь (слоты 9..35), 3 - хотбар (слоты 0..8)
				int slot = r < 3 ? 9 + r * 9 + col : col;
				int x = 4 + col * 18, y = 4 + r * 18;
				c.fill(x, y, x + 17, y + 17, 0x22FFFFFF);
				ItemStack st = inv.getStack(slot);
				if (!st.isEmpty()) {
					c.drawItem(st, x + 1, y + 1);
					c.drawStackOverlay(tr, st, x + 1, y + 1);
				}
			}
		}
		return new int[]{w, h};
	}

	private static int[] drawStaff(DrawContext c, MinecraftClient mc) {
		List<String> list = StaffListFeature.staff;
		if (list.isEmpty()) return null;
		return textList(c, mc, "Staff (" + list.size() + ")", list, 0xFFEF4444);
	}

	private static int[] drawFriends(DrawContext c, MinecraftClient mc) {
		Set<String> online = Friends.onlineCache();
		if (online.isEmpty()) return null;
		List<String> names = new ArrayList<>();
		for (String n : online) names.add(Friends.display(n));
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return textList(c, mc, "Friends (" + names.size() + ")", names, 0xFF4ADE80);
	}

	/** Общий вид маленькой панели: заголовок и строки с цветной точкой. */
	private static int[] textList(DrawContext c, MinecraftClient mc, String title, List<String> lines, int dot) {
		var tr = mc.textRenderer;
		int w = tr.getWidth(title);
		for (String l : lines) w = Math.max(w, tr.getWidth(l) + 8);
		w += 16;
		int h = 20 + lines.size() * 11;
		Gfx.rrect(c, 0, 0, w, h, 5, BG);
		c.fill(2, 5, 4, h - 5, Gfx.accentA());
		c.drawText(tr, title, 9, 5, Gfx.accentB(), true);
		int y = 17;
		for (String l : lines) {
			c.fill(9, y + 3, 12, y + 6, dot);
			c.drawText(tr, l, 16, y, Gfx.text(), true);
			y += 11;
		}
		return new int[]{w, h};
	}

	private static int[] drawMarks(DrawContext c, MinecraftClient mc) {
		var f = Features.WAYPOINTS;
		String dimNow = mc.world == null ? "" : mc.world.getRegistryKey().getValue().getPath();
		double px = mc.player.getX(), pz = mc.player.getZ();

		List<Waypoints.Mark> marks = new ArrayList<>();
		for (Waypoints.Mark m : Waypoints.LIST) {
			if (!m.visible) continue;
			if (f.sameDim.value && !m.dim.equals(dimNow)) continue;
			marks.add(m);
		}
		if (marks.isEmpty()) return null;
		marks.sort((a, b) -> Double.compare(dist2(a, px, pz), dist2(b, px, pz)));
		int max = (int) f.maxShown.value;
		if (marks.size() > max) marks = marks.subList(0, max);

		var tr = mc.textRenderer;
		List<String[]> rows = new ArrayList<>();
		int w = 0;
		for (Waypoints.Mark m : marks) {
			double dx = m.x + 0.5 - px, dz = m.z + 0.5 - pz;
			int dist = (int) Math.sqrt(dx * dx + dz * dz);
			double target = Math.toDegrees(Math.atan2(-dx, dz));
			double diff = net.minecraft.util.math.MathHelper.wrapDegrees(target - mc.player.getYaw());
			String arrow = arrow(diff);
			String left = m.name;
			String right = dist + " m  " + arrow;
			rows.add(new String[]{left, right});
			w = Math.max(w, tr.getWidth(left) + 14 + tr.getWidth(right));
		}
		w += 16;
		int h = 8 + rows.size() * 12;
		Gfx.rrect(c, 0, 0, w, h, 5, BG);
		c.fill(2, 5, 4, h - 5, Gfx.accentB());
		int y = 5;
		for (String[] r : rows) {
			c.drawText(tr, r[0], 9, y, Gfx.text(), true);
			c.drawText(tr, r[1], w - 7 - tr.getWidth(r[1]), y, Gfx.accentB(), true);
			y += 12;
		}
		return new int[]{w, h};
	}

	private static double dist2(Waypoints.Mark m, double px, double pz) {
		double dx = m.x + 0.5 - px, dz = m.z + 0.5 - pz;
		return dx * dx + dz * dz;
	}

	/** Направление на метку относительно взгляда: diff > 0 - справа. */
	private static String arrow(double diff) {
		double a = Math.abs(diff);
		boolean right = diff > 0;
		if (a < 22.5) return "^";
		if (a < 67.5) return right ? "^>" : "<^";
		if (a < 112.5) return right ? ">" : "<";
		if (a < 157.5) return right ? "v>" : "<v";
		return "v";
	}

	// ------------------------------------------------------------------ compass / held item

	private static int[] drawCompass(DrawContext c, MinecraftClient mc) {
		var f = Features.COMPASS;
		var tr = mc.textRenderer;
		int w = 180, h = f.degrees.value ? 28 : 20;
		Gfx.rrect(c, 0, 0, w, h, 5, BG);

		// bearing: 0 = север, 90 = восток (в Minecraft yaw 0 = юг)
		float bearing = (net.minecraft.util.math.MathHelper.wrapDegrees(mc.player.getYaw() + 180f) + 360f) % 360f;
		float ppd = 1.4f;
		int cx = w / 2;
		String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

		int first = (int) Math.floor((bearing - 62f) / 15f);
		int last = (int) Math.ceil((bearing + 62f) / 15f);
		for (int k = first; k <= last; k++) {
			float b = k * 15f;
			int x = cx + Math.round((b - bearing) * ppd);
			if (x < 8 || x > w - 8) continue;
			float fade = 1f - Math.abs(x - cx) / (w / 2f);
			float alpha = Math.min(1f, 0.25f + fade * 1.2f);
			int bn = Math.floorMod(Math.round(b), 360);
			if (bn % 45 == 0) {
				String n = names[bn / 45];
				int col = bn % 90 == 0 ? Gfx.accentB() : Gfx.text();
				c.drawText(tr, n, x - tr.getWidth(n) / 2, 3, Gfx.withAlpha(col, alpha), true);
				c.fill(x, 13, x + 1, 17, Gfx.withAlpha(0xFFFFFFFF, alpha));
			} else {
				c.fill(x, 14, x + 1, 16, Gfx.withAlpha(0xFFFFFFFF, alpha * 0.6f));
			}
		}

		// метки на компасе
		if (f.marks.value && mc.world != null) {
			String dimNow = mc.world.getRegistryKey().getValue().getPath();
			for (Waypoints.Mark m : Waypoints.LIST) {
				if (!m.visible || !m.dim.equals(dimNow)) continue;
				double dx = m.x + 0.5 - mc.player.getX(), dz = m.z + 0.5 - mc.player.getZ();
				float mb = (float) Math.toDegrees(Math.atan2(dx, -dz));
				float d = net.minecraft.util.math.MathHelper.wrapDegrees(mb - bearing);
				if (Math.abs(d) > 58f) continue;
				int x = cx + Math.round(d * ppd);
				c.fill(x - 1, 10, x + 2, 13, Gfx.accentB());
			}
		}

		c.fill(cx - 1, 18, cx + 2, 19, Gfx.accentA());
		c.fill(cx, 17, cx + 1, 18, Gfx.accentA());
		if (f.degrees.value) {
			String t = Math.round(bearing) % 360 + "°";
			c.drawText(tr, t, cx - tr.getWidth(t) / 2, 19, Gfx.textDim(), true);
		}
		return new int[]{w, h};
	}

	private static int[] drawHeld(DrawContext c, MinecraftClient mc) {
		ItemStack st = mc.player.getMainHandStack();
		if (st.isEmpty()) return null;

		var tr = mc.textRenderer;
		String name = st.getName().getString();
		String sub = null;
		int subCol = Gfx.textDim();
		float frac = -1f;
		if (st.isDamageable()) {
			int left = st.getMaxDamage() - st.getDamage();
			frac = left / (float) st.getMaxDamage();
			sub = left + "/" + st.getMaxDamage();
			subCol = Gfx.lerp(0xFFEF4444, 0xFF4ADE80, frac);
		} else if (st.getCount() > 1) {
			sub = "x" + st.getCount();
		}

		int w = Math.max(tr.getWidth(name), sub == null ? 0 : tr.getWidth(sub)) + 36;
		int h = 28;
		Gfx.rrect(c, 0, 0, w, h, 5, BG);
		c.drawItem(st, 5, 4);
		c.drawText(tr, name, 26, 5, Gfx.text(), true);
		if (sub != null) c.drawText(tr, sub, 26, 15, subCol, true);
		if (frac >= 0f) {
			Gfx.rrect(c, 4, h - 5, w - 8, 2, 1, Gfx.OFF);
			Gfx.rrect(c, 4, h - 5, Math.max(2f, (w - 8) * frac), 2, 1, subCol);
		}
		return new int[]{w, h};
	}

	// ------------------------------------------------------------------ PvP

	private static int[] drawTarget(DrawContext c, MinecraftClient mc) {
		LivingEntity t = PvpStats.target;
		if (t == null || t.isRemoved() || !t.isAlive()) return null;
		if (System.currentTimeMillis() - PvpStats.targetTime > 6000) return null;

		var tr = mc.textRenderer;
		String name = t.getName().getString();
		float hp = t.getHealth();
		float max = Math.max(1f, t.getMaxHealth());
		float frac = Math.max(0f, Math.min(1f, hp / max));
		String hpText = String.format(java.util.Locale.ROOT, "%.1f HP", hp);

		int w = Math.max(124, tr.getWidth(name) + tr.getWidth(hpText) + 26);
		int hgt = 38;
		Gfx.rrect(c, 0, 0, w, hgt, 5, BG);
		c.fill(2, 5, 4, hgt - 5, Gfx.accentA());

		c.drawText(tr, name, 9, 5, Gfx.text(), true);
		int hpCol = Gfx.lerp(0xFFEF4444, 0xFF4ADE80, frac);
		c.drawText(tr, hpText, w - tr.getWidth(hpText) - 7, 5, hpCol, true);

		Gfx.rrect(c, 9, 19, w - 16, 5, 2, Gfx.OFF);
		Gfx.rrect(c, 9, 19, Math.max(3f, (w - 16) * frac), 5, 2, hpCol);

		c.drawText(tr, "Armor " + t.getArmor(), 9, 27, Gfx.textDim(), true);
		return new int[]{w, hgt};
	}

	private static int[] drawCombo(DrawContext c, MinecraftClient mc) {
		boolean showCombo = PvpStats.combo > 0;
		boolean showReach = Features.COMBO.reach.value && PvpStats.reach >= 0
			&& System.currentTimeMillis() - PvpStats.lastHitMs < 3000;
		if (!showCombo && !showReach) return null;

		var tr = mc.textRenderer;
		String l1 = showCombo ? "Combo  " + PvpStats.combo : null;
		String l2 = showReach ? String.format(java.util.Locale.ROOT, "Reach  %.2f", PvpStats.reach) : null;

		int w = 0, rows = 0;
		if (l1 != null) { w = Math.max(w, tr.getWidth(l1)); rows++; }
		if (l2 != null) { w = Math.max(w, tr.getWidth(l2)); rows++; }
		w += 16;
		int hgt = 6 + rows * 11 + 2;

		Gfx.rrect(c, 0, 0, w, hgt, 5, BG);
		Gfx.gradientBar(c, 6, hgt - 2, w - 12, 1, 1f);
		int y = 5;
		if (l1 != null) {
			c.drawText(tr, l1, (w - tr.getWidth(l1)) / 2, y, Gfx.accentA(), true);
			y += 11;
		}
		if (l2 != null) {
			c.drawText(tr, l2, (w - tr.getWidth(l2)) / 2, y, Gfx.text(), true);
		}
		return new int[]{w, hgt};
	}

	// предметы создаются лениво: реестры к этому моменту уже готовы
	private static ItemStack[] stacks;

	private static int[] drawItems(DrawContext c, MinecraftClient mc) {
		var f = Features.PVP_ITEMS;
		if (stacks == null) {
			stacks = new ItemStack[]{
				new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.GOLDEN_APPLE),
				new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.END_CRYSTAL), new ItemStack(Items.SPLASH_POTION)
			};
		}
		boolean[] on = {f.totems.value, f.gapples.value, f.pearls.value, f.crystals.value, f.potions.value};
		Item[][] kinds = {
			{Items.TOTEM_OF_UNDYING}, {Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE},
			{Items.ENDER_PEARL}, {Items.END_CRYSTAL}, {Items.SPLASH_POTION}
		};

		var tr = mc.textRenderer;
		int rowH = 20, w = 44, y = 0, rows = 0;
		for (boolean b : on) if (b) rows++;
		if (rows == 0) return null;

		Gfx.rrect(c, 0, 0, w, rows * rowH + 4, 5, BG);
		y = 2;
		for (int i = 0; i < on.length; i++) {
			if (!on[i]) continue;
			int count = count(mc.player.getInventory(), kinds[i]);
			c.drawItem(stacks[i], 4, y + 2);
			int col = count == 0 ? 0xFFEF4444 : Gfx.text();
			c.drawText(tr, String.valueOf(count), 24, y + 6, col, true);
			y += rowH;
		}
		return new int[]{w, rows * rowH + 4};
	}

	private static int count(PlayerInventory inv, Item[] items) {
		int total = 0;
		for (int i = 0; i < inv.size(); i++) {
			ItemStack st = inv.getStack(i);
			if (st.isEmpty()) continue;
			for (Item it : items) {
				if (st.isOf(it)) {
					total += st.getCount();
					break;
				}
			}
		}
		return total;
	}

	// ------------------------------------------------------------------ CPS

	private static void pollClicks(MinecraftClient mc) {
		long handle = mc.getWindow().getHandle();
		long now = System.currentTimeMillis();
		boolean inWorld = mc.currentScreen == null;

		boolean l = inWorld && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = inWorld && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (l && !leftDown) LEFT_CLICKS.addLast(now);
		if (r && !rightDown) RIGHT_CLICKS.addLast(now);
		leftDown = l;
		rightDown = r;

		trim(LEFT_CLICKS, now);
		trim(RIGHT_CLICKS, now);
	}

	private static void trim(Deque<Long> q, long now) {
		while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
	}
}
