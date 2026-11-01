package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.NumSetting;
import net.minecraft.client.MinecraftClient;

/** Меняет погоду только на вашем экране: ясно, дождь или гроза. Сервер и другие игроки ничего не замечают. */
public class WeatherFeature extends Feature {
	public final NumSetting mode = add(new NumSetting("Weather", 0, 0, 2, 1, "").labels("Clear", "Rain", "Thunder"));
	public final NumSetting intensity = add(new NumSetting("Intensity", 100, 10, 100, 5, "%"));

	public WeatherFeature() {
		super("Weather", "Client-side clear / rain / thunder", Category.VISUAL);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (!enabled || mc.world == null) return;
		int m = (int) Math.round(mode.value);
		float k = (float) (intensity.value / 100.0);
		mc.world.setRainGradient(m == 0 ? 0f : k);
		mc.world.setThunderGradient(m == 2 ? k : 0f);
	}
}
