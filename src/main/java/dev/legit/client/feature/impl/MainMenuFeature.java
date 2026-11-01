package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;

/** Настройки собственного главного меню. Цвета, прозрачность и скругление берутся из Client → Theme. */
public class MainMenuFeature extends Feature {
	public final NumSetting position = add(new NumSetting("Position", 2, 0, 2, 1, "").labels("Left", "Center", "Right"));
	public final NumSetting offsetX = add(new NumSetting("Offset X", 0, -300, 300, 5, ""));
	public final NumSetting offsetY = add(new NumSetting("Offset Y", 0, -200, 200, 5, ""));
	public final BoolSetting showTitle = add(new BoolSetting("Show title", true));
	public final NumSetting titleSize = add(new NumSetting("Title size", 2.0, 1.0, 4.0, 0.25, "x"));
	public final BoolSetting welcome = add(new BoolSetting("Welcome text", true));
	public final BoolSetting footer = add(new BoolSetting("Footer info", true));
	public final NumSetting bgMode = add(new NumSetting("Background", 0, 0, 2, 1, "").labels("Gradient", "Solid", "Custom hue"));
	public final NumSetting bgHue = add(new NumSetting("Background hue", 250, 0, 360, 1, "\u00b0").asHue());
	public final NumSetting orbSize = add(new NumSetting("Orb size", 100, 40, 160, 10, "%"));
	public final NumSetting orbGlow = add(new NumSetting("Orb brightness", 7, 2, 20, 1, ""));
	public final NumSetting btnStyle = add(new NumSetting("Button style", 0, 0, 2, 1, "").labels("Filled", "Outline", "Minimal"));
	public final NumSetting btnSpacing = add(new NumSetting("Button spacing", 6, 2, 18, 1, ""));
	public final NumSetting btnRadius = add(new NumSetting("Button radius", 8, 0, 16, 1, ""));
	public final NumSetting btnOpacity = add(new NumSetting("Button opacity", 90, 20, 100, 5, "%"));
	public final NumSetting textAlign = add(new NumSetting("Button text", 0, 0, 1, 1, "").labels("Left", "Center"));
	public final BoolSetting accentBar = add(new BoolSetting("Accent bar", true));
	public final BoolSetting hoverGlow = add(new BoolSetting("Hover glow", true));
	public final BoolSetting entrance = add(new BoolSetting("Entrance animation", true));
	public final BoolSetting orbs = add(new BoolSetting("Glow orbs", true));
	public final BoolSetting stars = add(new BoolSetting("Starry background", true));
	public final BoolSetting playerCard = add(new BoolSetting("Player card", true));
	public final BoolSetting clock = add(new BoolSetting("Clock", true));
	public final NumSetting darkness = add(new NumSetting("Darkness", 55, 0, 90, 5, "%"));
	public final NumSetting orbSpeed = add(new NumSetting("Orb speed", 3, 1, 10, 1, ""));
	public final NumSetting width = add(new NumSetting("Menu width", 210, 150, 280, 10, ""));
	public final NumSetting buttonHeight = add(new NumSetting("Button height", 24, 18, 34, 1, ""));

	public MainMenuFeature() {
		super("Main Menu", "Custom title screen (disable to get vanilla)", Category.CLIENT);
		noBind();
		this.enabled = true;
		this.anim = 1f;
	}
}
