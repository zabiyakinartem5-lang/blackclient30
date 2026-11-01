package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;
import dev.legit.client.gui.Gfx;

/** Цвета и вид интерфейса: два акцента, насыщенность, радуга, прозрачность и закругление углов у ВСЕХ элементов. */
public class ThemeFeature extends Feature {
	public final NumSetting preset = add(new NumSetting("Preset", 0, 0, 7, 1, "")
		.labels("Custom", "Black", "Ocean", "Sunset", "Neon", "Forest", "Blood", "Purple"));
	public final NumSetting hueA = add(new NumSetting("Color 1", 262, 0, 360, 1, "\u00b0").asHue());
	public final NumSetting hueB = add(new NumSetting("Color 2", 188, 0, 360, 1, "\u00b0").asHue());
	public final NumSetting saturation = add(new NumSetting("Saturation", 75, 0, 100, 1, "%"));
	public final NumSetting brightness = add(new NumSetting("Brightness", 96, 40, 100, 1, "%"));
	public final BoolSetting rainbow = add(new BoolSetting("Rainbow", false));
	public final NumSetting rainbowSpeed = add(new NumSetting("Rainbow speed", 3, 1, 10, 1, ""));
	public final BoolSetting gradient = add(new BoolSetting("Gradient accents", true));
	public final NumSetting panelHue = add(new NumSetting("Panel hue", 240, 0, 360, 1, "\u00b0").asHue());
	public final NumSetting panelSat = add(new NumSetting("Panel saturation", 25, 0, 100, 1, "%"));
	public final NumSetting panelBright = add(new NumSetting("Panel brightness", 9, 0, 40, 1, "%"));
	public final NumSetting textHue = add(new NumSetting("Text hue", 240, 0, 360, 1, "\u00b0").asHue());
	public final NumSetting textSat = add(new NumSetting("Text saturation", 4, 0, 100, 1, "%"));
	public final NumSetting textBright = add(new NumSetting("Text brightness", 96, 50, 100, 1, "%"));
	public final NumSetting opacity = add(new NumSetting("Menu opacity", 94, 20, 100, 1, "%"));
	public final NumSetting hudOpacity = add(new NumSetting("HUD opacity", 69, 0, 100, 1, "%"));
	public final NumSetting radius = add(new NumSetting("Corner radius", 8, 0, 16, 1, ""));
	public final BoolSetting animations = add(new BoolSetting("Animations", true));
	public final NumSetting animSpeed = add(new NumSetting("Animation speed", 1.0, 0.5, 2.0, 0.1, "x"));
	public final BoolSetting toasts = add(new BoolSetting("Module toasts", true));

	public ThemeFeature() {
		super("Theme", "Colors, transparency and rounded corners", Category.CLIENT);
		noBind();
		this.enabled = true;
		this.anim = 1f;
		this.alwaysOn = true;
	}

	// name, hueA, hueB, sat, bright, panelHue, panelSat, panelBright
	private static final double[][] PRESETS = {
		{0, 0, 0, 88, 0, 0, 6},
		{200, 170, 75, 96, 220, 30, 9},
		{15, 335, 80, 98, 10, 25, 9},
		{300, 160, 85, 100, 270, 30, 7},
		{140, 90, 65, 90, 150, 20, 8},
		{0, 350, 85, 90, 0, 30, 8},
		{262, 188, 75, 96, 240, 25, 9},
	};
	private int lastPreset = -1;

	@Override
	public void onTick(net.minecraft.client.MinecraftClient mc) {
		int p = (int) preset.value;
		if (lastPreset < 0) {
			lastPreset = p;
			return;
		}
		if (p == lastPreset) return;
		lastPreset = p;
		if (p <= 0 || p > PRESETS.length) return;
		double[] d = PRESETS[p - 1];
		hueA.set(d[0]);
		hueB.set(d[1]);
		saturation.set(d[2]);
		brightness.set(d[3]);
		panelHue.set(d[4]);
		panelSat.set(d[5]);
		panelBright.set(d[6]);
	}

	/** Цвет панелей: множитель яркости относительно базовой. */
	public int panelRgb(float mul) {
		float b = Math.min(1f, (float) panelBright.value / 100f * mul);
		return Gfx.hsb((float) (panelHue.value / 360.0), (float) panelSat.value / 100f, b) & 0xFFFFFF;
	}

	/** Цвет текста с множителем яркости (0.65 = приглушённый). */
	public int textColor(float mul) {
		float b = Math.min(1f, (float) textBright.value / 100f * mul);
		return Gfx.hsb((float) (textHue.value / 360.0), (float) textSat.value / 100f, b);
	}

	private float shift() {
		if (!rainbow.value) return 0f;
		double t = (System.currentTimeMillis() % 100_000L) / 1000.0;
		return (float) ((t * rainbowSpeed.value * 12.0) % 360.0);
	}

	public int colorA() {
		float h = (float) ((hueA.value + shift()) / 360.0);
		return Gfx.hsb(h, (float) saturation.value / 100f, (float) brightness.value / 100f);
	}

	public int colorB() {
		if (!gradient.value) return colorA();
		float h = (float) ((hueB.value + shift()) / 360.0);
		return Gfx.hsb(h, (float) saturation.value / 100f, (float) brightness.value / 100f);
	}

	public int panelAlpha() {
		return Math.max(0, Math.min(255, (int) (opacity.value / 100.0 * 255.0)));
	}

	public int hudAlpha() {
		return Math.max(0, Math.min(255, (int) (hudOpacity.value / 100.0 * 255.0)));
	}

	/** Множитель радиуса скругления: 8 = как задумано, 0 = прямые углы. */
	public float radiusScale() {
		return (float) (radius.value / 8.0);
	}
}
