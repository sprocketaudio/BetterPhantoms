package net.sprocketgames.betterphantoms;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

public final class ElytraRepairController {
    private ElytraRepairController() {}

    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getPlayer().level().isClientSide) {
            return;
        }

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!left.is(Items.ELYTRA) || !right.is(Items.PHANTOM_MEMBRANE)) {
            return;
        }

        double efficiency = Mth.clamp(Config.elytraRepairEffectiveness, 0.1D, 1.0D);
        if (efficiency >= 0.999D) {
            return;
        }

        if (!left.isDamaged()) {
            return;
        }

        int vanillaRepairPerMembrane = Math.max(1, left.getMaxDamage() / 4);
        int scaledRepairPerMembrane = Math.max(1, (int) Math.round(vanillaRepairPerMembrane * efficiency));

        ItemStack output = left.copy();
        int materialUsed = 0;
        int operationCost = 0;
        int damage = output.getDamageValue();

        while (damage > 0 && materialUsed < right.getCount()) {
            damage = Math.max(0, damage - scaledRepairPerMembrane);
            output.setDamageValue(damage);
            materialUsed++;
            operationCost++;
        }

        if (materialUsed <= 0) {
            return;
        }

        operationCost += left.getOrDefault(DataComponents.REPAIR_COST, 0);
        operationCost += right.getOrDefault(DataComponents.REPAIR_COST, 0);

        int renameCost = applyRename(output, left, event.getName());
        operationCost += renameCost;

        event.setOutput(output);
        event.setMaterialCost(materialUsed);
        event.setCost(operationCost);
    }

    private static int applyRename(ItemStack output, ItemStack left, @Nullable String desiredName) {
        if (desiredName != null && !StringUtil.isBlank(desiredName)) {
            if (!desiredName.equals(left.getHoverName().getString())) {
                output.set(DataComponents.CUSTOM_NAME, Component.literal(desiredName));
                return 1;
            }
            return 0;
        }

        if (left.has(DataComponents.CUSTOM_NAME)) {
            output.remove(DataComponents.CUSTOM_NAME);
            return 1;
        }

        return 0;
    }
}
