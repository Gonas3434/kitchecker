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

    /** Kopiert das echte Inventar (Hotbar, Inventar, Ruestung, Offhand) in das Kit. */
    public static void importFromPlayer(Kit kit, ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            kit.slots[i] = fromStack(player.getInventory().getStack(i));
        }
        kit.slots[36] = fromStack(player.getEquippedStack(EquipmentSlot.FEET));
        kit.slots[37] = fromStack(player.getEquippedStack(EquipmentSlot.LEGS));
        kit.slots[38] = fromStack(player.getEquippedStack(EquipmentSlot.CHEST));
        kit.slots[39] = fromStack(player.getEquippedStack(EquipmentSlot.HEAD));
        kit.slots[40] = fromStack(player.getOffHandStack());
    }

    private static KitEntry fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        KitEntry e = new KitEntry(Registries.ITEM.getId(stack.getItem()).toString());
        e.count = Math.max(1, stack.getCount());
        addEnchants(e, stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        addEnchants(e, stack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        return e;
    }

    private static void addEnchants(KitEntry e, ItemEnchantmentsComponent component) {
        for (RegistryEntry<Enchantment> en : component.getEnchantments()) {
            en.getKey().ifPresent(k -> e.enchants.put(k.getValue().toString(), component.getLevel(en)));
        }
    }
}
