package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.NumSetting;

/** Предмет в руке: название, прочность или количество. */
public class HeldItemFeature extends Feature {
	public final NumSetting scale = add(new NumSetting("Scale", 1.0, 0.7, 1.6, 0.1, "x"));

	public HeldItemFeature() {
		super("Held Item", "Item in your hand with durability", Category.HUD);
	}
}
