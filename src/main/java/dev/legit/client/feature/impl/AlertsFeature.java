package dev.legit.client.feature.impl;

import dev.legit.client.feature.Category;
import dev.legit.client.feature.Feature;
import dev.legit.client.feature.setting.BoolSetting;
import dev.legit.client.feature.setting.NumSetting;
import dev.legit.client.gui.Toasts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Предупреждения: мало здоровья, почти сломана броня, голод. Всплывающие уведомления и красная пульсация по краям. */
public class AlertsFeature extends Feature {
	public final BoolSetting lowHealth = add(new BoolSetting("Low health", true));
	public final NumSetting healthHearts = add(new NumSetting("Health limit", 5, 2, 10, 1, " hearts"));
	public final BoolSetting lowArmor = add(new BoolSetting("Low armor", true));
	public final BoolSetting lowFood = add(new BoolSetting("Low food", true));
	public final BoolSetting pulse = add(new BoolSetting("Red screen pulse", true));

	private static final EquipmentSlot[] ARMOR = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private final long[] last = new long[3];
	private int timer;

	public AlertsFeature() {
		super("Alerts", "Warnings for low health, armor and food", Category.HUD);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (!enabled || mc.player == null) return;
		if (++timer < 10) return;
		timer = 0;
		long now = System.currentTimeMillis();

		float hp = mc.player.getHealth();
		if (lowHealth.value && hp > 0 && hp <= healthHearts.value * 2 && now - last[0] > 12_000) {
			last[0] = now;
			Toasts.push("Мало здоровья!", 0xFFEF4444);
		}

		if (lowArmor.value && now - last[1] > 20_000) {
			for (EquipmentSlot slot : ARMOR) {
				ItemStack st = mc.player.getEquippedStack(slot);
				if (st.isEmpty() || !st.isDamageable()) continue;
				int left = st.getMaxDamage() - st.getDamage();
				if (left * 100 / st.getMaxDamage() <= 12) {
					last[1] = now;
					Toasts.push("Броня почти сломана", 0xFFF59E0B);
					break;
				}
			}
		}

		if (lowFood.value && mc.player.getHungerManager().getFoodLevel() <= 6 && now - last[2] > 30_000) {
			last[2] = now;
			Toasts.push("Вы проголодались", 0xFFF59E0B);
		}
	}
}
