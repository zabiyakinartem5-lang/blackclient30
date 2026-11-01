package dev.legit.client.gui;

import dev.legit.client.Config;
import dev.legit.client.gui.hud.HudElement;
import dev.legit.client.gui.hud.HudElements;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Экран, где блоки HUD перетаскиваются мышью. ПКМ по блоку возвращает его на место. */
public class HudEditor extends Screen {
	private HudElement dragging;
	private float grabX, grabY;
	private boolean snapped;
	/** Последний выбранный блок: его можно двигать стрелками (Shift — по 10 px). */
	private HudElement selected;

	/** Блок, у которого открыта панель индивидуальных настроек (ПКМ). */
	private HudElement editing;
	private int panX, panY, dragSlider = -1;
	private static final int PW = 170, PH = 176;
	private static final String[] SL_NAMES = {"Size", "Background", "Color hue"};
	private static final float[] SL_MIN = {0.5f, 0f, 0f};
	private static final float[] SL_MAX = {2.5f, 1f, 360f};

	private int resetX, resetY, resetW = 70, resetH = 16;

	public HudEditor() {
		super(Text.literal("HUD Editor"));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		Config.save();
		if (client != null) client.setScreen(new ClickGui());
	}

	private float elLeft(HudElement e) {
		return e.left(width);
	}

	private float elTop(HudElement e) {
		return e.top(height);
	}

	private float elW(HudElement e) {
		return Math.max(36f, e.w * e.scale);
	}

	private float elH(HudElement e) {
		return Math.max(14f, e.h * e.scale);
	}

	private boolean inside(HudElement e, double mx, double my) {
		float l = elLeft(e), t = elTop(e);
		return mx >= l && mx <= l + elW(e) && my >= t && my <= t + elH(e);
	}

	@Override
	public void render(DrawContext c, int mouseX, int mouseY, float delta) {
		c.fill(0, 0, width, height, 0x66050510);

		if (snapped && dragging != null) {
			c.fill(width / 2, 0, width / 2 + 1, height, Gfx.withAlpha(Gfx.accentB(), 0.8f));
		}

		HudElement hover = null;
		for (int i = HudElements.ALL.size() - 1; i >= 0; i--) {
			HudElement e = HudElements.ALL.get(i);
			if (e.visible.getAsBoolean() && inside(e, mouseX, mouseY)) {
				hover = e;
				break;
			}
		}

		for (HudElement e : HudElements.ALL) {
			if (!e.visible.getAsBoolean()) continue;
			int l = Math.round(elLeft(e)), t = Math.round(elTop(e));
			int w = Math.round(elW(e)), h = Math.round(elH(e));
			boolean active = e == dragging || e == hover || e == selected;
			int col = active ? Gfx.accentB() : Gfx.withAlpha(Gfx.accentA(), 0.8f);

			c.fill(l, t, l + w, t + h, active ? 0x30FFFFFF : 0x18FFFFFF);
			c.fill(l, t, l + w, t + 1, col);
			c.fill(l, t + h - 1, l + w, t + h, col);
			c.fill(l, t, l + 1, t + h, col);
			c.fill(l + w - 1, t, l + w, t + h, col);

			int ly = t >= 11 ? t - 10 : t + h + 2;
			c.drawText(textRenderer, e.hidden ? e.label + " (hidden)" : e.label, l, ly, col, true);
		}

		String title = "HUD Editor";
		String hint = "LMB - drag  |  RMB - block settings  |  Arrows - nudge (Shift x10)  |  ESC - back";
		c.drawText(textRenderer, title, (width - textRenderer.getWidth(title)) / 2, height / 2 - 14, Gfx.text(), true);
		c.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, height / 2 - 2, Gfx.textDim(), true);

		// кнопка Reset all
		resetX = (width - resetW) / 2;
		resetY = height / 2 + 12;
		boolean rh = mouseX >= resetX && mouseX < resetX + resetW && mouseY >= resetY && mouseY < resetY + resetH;
		Gfx.rrect(c, resetX, resetY, resetW, resetH, 5, rh ? Gfx.accentA() : Gfx.header());
		String rt = "Reset all";
		c.drawText(textRenderer, rt, resetX + (resetW - textRenderer.getWidth(rt)) / 2, resetY + 4, Gfx.text(), true);

		if (editing != null) drawPanel(c, mouseX, mouseY);
	}

	private float slVal(int i) {
		return switch (i) {
			case 0 -> editing.scaleMul;
			case 1 -> editing.bgMul;
			default -> editing.hue;
		};
	}

	private void slSet(int i, float v) {
		v = Math.max(SL_MIN[i], Math.min(SL_MAX[i], v));
		switch (i) {
			case 0 -> editing.scaleMul = Math.round(v * 20f) / 20f;
			case 1 -> editing.bgMul = Math.round(v * 20f) / 20f;
			default -> editing.hue = Math.round(v);
		}
	}

	private int slY(int i) {
		return panY + 22 + i * 24;
	}

	private int tgY(int j) {
		return panY + 98 + j * 16;
	}

	private boolean tgGet(int j) {
		return j == 0 ? editing.outline : j == 1 ? editing.customColor : editing.hidden;
	}

	private void tgFlip(int j) {
		if (j == 0) editing.outline = !editing.outline;
		else if (j == 1) editing.customColor = !editing.customColor;
		else editing.hidden = !editing.hidden;
	}

	private void layoutPanel() {
		float cx = elLeft(editing) + elW(editing) / 2f;
		panX = cx < width / 2f ? width - PW - 10 : 10;
		panY = Math.max(10, (height - PH) / 2);
	}

	private void drawPanel(DrawContext c, int mx, int my) {
		layoutPanel();
		Gfx.rrect(c, panX - 1, panY - 1, PW + 2, PH + 2, 8, 0x50000000);
		Gfx.rrect(c, panX, panY, PW, PH, 7, 0xF0121218);
		c.drawText(textRenderer, editing.label, panX + 8, panY + 6, Gfx.accentA(), true);
		Gfx.gradientBar(c, panX + 8, panY + 17, PW - 16, 1, 1f);

		for (int i = 0; i < 3; i++) {
			int y = slY(i), bx = panX + 8, bw = PW - 16;
			float v = slVal(i);
			String vs = i == 0 ? String.format(java.util.Locale.ROOT, "%.2fx", v)
				: i == 1 ? Math.round(v * 100) + "%" : Math.round(v) + "\u00b0";
			c.drawText(textRenderer, SL_NAMES[i], bx, y, Gfx.textDim(), true);
			c.drawText(textRenderer, vs, bx + bw - textRenderer.getWidth(vs), y, Gfx.text(), true);
			float f = (v - SL_MIN[i]) / (SL_MAX[i] - SL_MIN[i]);
			Gfx.rrect(c, bx, y + 12, bw, 4, 2, Gfx.OFF);
			int fill = i == 2 ? Gfx.hsb(v / 360f, 0.75f, 1f) : Gfx.accentB();
			Gfx.rrect(c, bx, y + 12, Math.max(4f, bw * f), 4, 2, fill);
			Gfx.rrect(c, bx + bw * f - 4, y + 10, 8, 8, 4, 0xFFFFFFFF);
		}
		String[] tn = {"Outline", "Custom color", "Hide block"};
		for (int j = 0; j < 3; j++) {
			int y = tgY(j), bx = panX + 8, bw = PW - 16;
			c.drawText(textRenderer, tn[j], bx, y + 3, Gfx.textDim(), true);
			boolean on = tgGet(j);
			Gfx.rrect(c, bx + bw - 20, y + 2, 20, 10, 5, on ? Gfx.accentB() : Gfx.OFF);
			Gfx.rrect(c, bx + bw - 19 + (on ? 10 : 0), y + 3, 8, 8, 4, 0xFFFFFFFF);
		}
		int by = panY + PH - 24, bw2 = (PW - 24) / 2;
		btn(c, panX + 8, by, bw2, "Reset pos", mx, my);
		btn(c, panX + 16 + bw2, by, bw2, "Reset style", mx, my);
	}

	private void btn(DrawContext c, int x, int y, int w, String t, int mx, int my) {
		boolean h = mx >= x && mx < x + w && my >= y && my < y + 16;
		Gfx.rrect(c, x, y, w, 16, 5, h ? Gfx.accentA() : Gfx.header());
		c.drawText(textRenderer, t, x + (w - textRenderer.getWidth(t)) / 2, y + 4, Gfx.text(), true);
	}

	private boolean panelClick(double mx, double my) {
		if (mx < panX || mx >= panX + PW || my < panY || my >= panY + PH) return false;
		for (int i = 0; i < 3; i++) {
			if (my >= slY(i) + 6 && my < slY(i) + 22) {
				dragSlider = i;
				panelDrag(mx);
				return true;
			}
		}
		for (int j = 0; j < 3; j++) {
			if (my >= tgY(j) && my < tgY(j) + 14) {
				tgFlip(j);
				Config.save();
				return true;
			}
		}
		int by = panY + PH - 24, bw2 = (PW - 24) / 2;
		if (my >= by && my < by + 16) {
			if (mx >= panX + 8 && mx < panX + 8 + bw2) editing.reset();
			else if (mx >= panX + 16 + bw2 && mx < panX + 16 + 2 * bw2) editing.resetStyle();
			Config.save();
		}
		return true;
	}

	private void panelDrag(double mx) {
		int i = dragSlider;
		float f = (float) ((mx - (panX + 8)) / (PW - 16));
		f = Math.max(0f, Math.min(1f, f));
		slSet(i, SL_MIN[i] + (SL_MAX[i] - SL_MIN[i]) * f);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (editing != null && button == 0 && panelClick(mx, my)) return true;
		if (button == 0 && mx >= resetX && mx < resetX + resetW && my >= resetY && my < resetY + resetH) {
			HudElements.resetAll();
			return true;
		}
		for (int i = HudElements.ALL.size() - 1; i >= 0; i--) {
			HudElement e = HudElements.ALL.get(i);
			if (!e.visible.getAsBoolean() || !inside(e, mx, my)) continue;
			if (button == 0) {
				dragging = e;
				selected = e;
				grabX = (float) mx - elLeft(e);
				grabY = (float) my - elTop(e);
			} else if (button == 1) {
				e.reset();
			}
			return true;
		}
		if (button == 0) editing = null;
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragSlider >= 0 && editing != null && button == 0) {
			panelDrag(mx);
			return true;
		}
		if (dragging == null || button != 0) return super.mouseDragged(mx, my, button, dx, dy);

		HudElement e = dragging;
		float ww = e.w * e.scale;
		float hh = e.h * e.scale;
		float newLeft = (float) mx - grabX;
		float newTop = (float) my - grabY;

		// примагничивание к центру экрана по горизонтали
		snapped = false;
		if (Math.abs(newLeft + ww / 2f - width / 2f) < 6f) {
			newLeft = width / 2f - ww / 2f;
			snapped = true;
		}
		// примагничивание к краям экрана
		float edge = 5f;
		if (newLeft < edge) newLeft = 0f;
		else if (width - ww - newLeft < edge) newLeft = width - ww;
		if (newTop < edge) newTop = 0f;
		else if (height - hh - newTop < edge) newTop = height - hh;

		newLeft = Math.max(0f, Math.min(width - ww, newLeft));
		newTop = Math.max(0f, Math.min(height - hh, newTop));

		e.fx = (newLeft + e.anchorOffset()) / width;
		e.fy = newTop / height;
		selected = e;
		return true;
	}

	private void nudge(int dx, int dy) {
		HudElement e = selected;
		if (e == null) return;
		float ww = e.w * e.scale, hh = e.h * e.scale;
		float nl = Math.max(0f, Math.min(width - ww, e.left(width) + dx));
		float nt = Math.max(0f, Math.min(height - hh, e.top(height) + dy));
		e.fx = (nl + e.anchorOffset()) / width;
		e.fy = nt / height;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (dragSlider >= 0) {
			dragSlider = -1;
			Config.save();
		}
		if (dragging != null) {
			dragging = null;
			snapped = false;
			Config.save();
		}
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == dev.legit.client.Keys.gui()) {
			close();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && editing != null) {
			editing = null;
			return true;
		}
		int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
		switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT -> { nudge(-step, 0); return true; }
			case GLFW.GLFW_KEY_RIGHT -> { nudge(step, 0); return true; }
			case GLFW.GLFW_KEY_UP -> { nudge(0, -step); return true; }
			case GLFW.GLFW_KEY_DOWN -> { nudge(0, step); return true; }
			default -> {}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
