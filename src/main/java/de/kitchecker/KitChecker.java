package de.kitchecker;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Vergleicht ein gespeichertes Kit mit dem echten Inventar. Nur Anzeige, keine Aktionen. */
public final class KitChecker {
    private KitChecker() {
    }

    public static Item itemOf(String id) {
        Identifier identifier = Identifier.tryParse(id == null ? "" : id);
        return identifier == null ? Items.AIR : Registries.ITEM.get(identifier);
    }

    public static ItemStack stackOf(KitEntry e) {
        Item item = itemOf(e.item);
        int count = Math.max(1, Math.min(e.count, Math.max(1, item.getMaxCount())));
        ItemStack stack = new ItemStack(item, count);
        if (e.enchants != null && !e.enchants.isEmpty()) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    /** missing[i] == true, wenn der Eintrag in Slot i des Kits im Inventar fehlt. */
    public static boolean[] findMissing(Kit kit, ClientPlayerEntity player) {
        boolean[] missing = new boolean[Kit.SLOTS];

        List<ItemStack> have = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            have.add(player.getInventory().getStack(i));
        }
        have.add(player.getEquippedStack(EquipmentSlot.HEAD));
        have.add(player.getEquippedStack(EquipmentSlot.CHEST));
        have.add(player.getEquippedStack(EquipmentSlot.LEGS));
        have.add(player.getEquippedStack(EquipmentSlot.FEET));
        have.add(player.getOffHandStack());

        boolean[] used = new boolean[have.size()];

        for (int i = 0; i < Kit.SLOTS; i++) {
            KitEntry entry = kit.slots[i];
            if (entry == null) {
                continue;
            }
            Item item = itemOf(entry.item);
            boolean found = false;
            for (int j = 0; j < have.size(); j++) {
                ItemStack s = have.get(j);
                if (used[j] || s.isEmpty() || !s.isOf(item) || s.getCount() < entry.count) {
                    continue;
                }
                if (enchantsOk(s, entry)) {
                    used[j] = true;
                    found = true;
                    break;
                }
            }
            missing[i] = !found;
        }
        return missing;
    }

    private static boolean enchantsOk(ItemStack stack, KitEntry entry) {
        if (entry.enchants == null) {
            return true;
        }
        for (var e : entry.enchants.entrySet()) {
            if (levelOf(stack, e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static int levelOf(ItemStack stack, String id) {
        int a = levelIn(stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), id);
        int b = levelIn(stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), id);
        return Math.max(a, b);
    }

    private static int levelIn(ItemEnchantmentsComponent component, String id) {
        for (RegistryEntry<Enchantment> en : component.getEnchantments()) {
            String key = en.getKey().map(k -> k.getValue().toString()).orElse("");
            if (key.equals(id)) {
                return component.getLevel(en);
            }
        }
        return 0;
    }
}
