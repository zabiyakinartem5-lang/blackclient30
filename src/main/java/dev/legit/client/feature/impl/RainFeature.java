package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;

/** Капли дождя поверх экрана (в игре и в меню). Только визуальный эффект. */
public class RainFeature extends Feature {
	public final BoolSetting screen = add(new BoolSetting("Screen rain", true));
	public final BoolSetting inMenus = add(new BoolSetting("In menus", true));
	public final NumSetting amount = add(new NumSetting("Amount", 120, 20, 300, 10, ""));
	public final NumSetting speed = add(new NumSetting("Speed", 5, 1, 10, 1, ""));
	public final NumSetting length = add(new NumSetting("Length", 8, 3, 20, 1, ""));
	public final NumSetting wind = add(new NumSetting("Wind", 3, -10, 10, 1, ""));

	public RainFeature() {
		super("Rain", "Rain streaks over the screen", Category.VISUAL);
	}
}
