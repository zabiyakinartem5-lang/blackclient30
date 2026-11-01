package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;

/** Полоска компаса сверху: стороны света, градусы и ваши метки. */
public class CompassFeature extends Feature {
	public final BoolSetting marks = add(new BoolSetting("Show marks", true));
	public final BoolSetting degrees = add(new BoolSetting("Show degrees", true));
	public final NumSetting scale = add(new NumSetting("Scale", 1.0, 0.7, 1.6, 0.1, "x"));

	public CompassFeature() {
		super("Compass", "Compass bar with your marks", Category.HUD);
	}
}
