package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.concurrent.ThreadLocalRandom;

/** Погодные эффекты на экране и вокруг игрока: снег, дождь, лепестки, угли. Только визуально, на вашем экране. */
public class SnowFeature extends Feature {
	public final NumSetting type = add(new NumSetting("Type", 0, 0, 3, 1, "").labels("Snow", "Rain", "Petals", "Embers"));
	public final NumSetting amount = add(new NumSetting("Amount", 120, 20, 400, 10, ""));
	public final NumSetting speed = add(new NumSetting("Speed", 3, 1, 10, 1, ""));
	public final NumSetting size = add(new NumSetting("Size", 2, 1, 4, 1, ""));
	public final NumSetting wind = add(new NumSetting("Wind", 0, -10, 10, 1, ""));
	public final NumSetting opacity = add(new NumSetting("Opacity", 90, 20, 100, 5, "%"));
	public final BoolSetting screen = add(new BoolSetting("Screen overlay", true));
	public final BoolSetting inMenus = add(new BoolSetting("In menus", true));
	public final BoolSetting world = add(new BoolSetting("World particles", false));

	public SnowFeature() {
		super("Weather FX", "Snow, rain, petals or embers on screen", Category.VISUAL);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (!enabled || !world.value || mc.world == null || mc.player == null) return;
		int t = (int) Math.round(type.value);
		if (t == 1) return; // у дождя своих мировых частиц нет, для него есть модуль Weather

		ParticleEffect p = switch (t) {
			case 2 -> ParticleTypes.CHERRY_LEAVES;
			case 3 -> ParticleTypes.SMALL_FLAME;
			default -> ParticleTypes.SNOWFLAKE;
		};
		double vy = t == 3 ? 0.02 : -0.04;
		double wx = wind.value * 0.004;

		ThreadLocalRandom r = ThreadLocalRandom.current();
		int n = (int) (amount.value / 40.0) + 1;
		for (int i = 0; i < n; i++) {
			double x = mc.player.getX() + (r.nextDouble() - 0.5) * 26.0;
			double y = mc.player.getY() + (t == 3 ? -1.0 : 3.0) + r.nextDouble() * 7.0;
			double z = mc.player.getZ() + (r.nextDouble() - 0.5) * 26.0;
			mc.world.addParticle(p, x, y, z, (r.nextDouble() - 0.5) * 0.02 + wx, vy, (r.nextDouble() - 0.5) * 0.02);
		}
	}
}
