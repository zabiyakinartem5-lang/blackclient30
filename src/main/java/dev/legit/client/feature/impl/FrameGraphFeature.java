package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;

/** График времени кадра: видно просадки FPS. */
public class FrameGraphFeature extends Feature {
	public final NumSetting width = add(new NumSetting("Width", 90, 50, 200, 10, ""));
	public final NumSetting height = add(new NumSetting("Height", 30, 16, 80, 2, ""));
	public final NumSetting limit = add(new NumSetting("Max frame time", 33, 10, 100, 1, "ms"));
	public final BoolSetting showFps = add(new BoolSetting("Show FPS", true));
	public final NumSetting scale = add(new NumSetting("Scale", 1.0, 0.7, 1.6, 0.1, "x"));

	public FrameGraphFeature() {
		super("Frame Graph", "Frame time graph", Category.HUD);
	}
}
