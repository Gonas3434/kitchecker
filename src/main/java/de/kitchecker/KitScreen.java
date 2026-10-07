package de.kitchecker;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Gonas3434 Kitchecker - alles auf einer Seite:
 * oben Kategorien, links dein Kit, rechts alle Items, darunter Anzahl und Verzauberungen.
 * Fehlende Items im Inventar werden rot markiert.
 */
public class KitScreen extends Screen {
    private static final int CELL = 20;
    private static final int PER_PAGE = 40;

    private static final int PURPLE = 0xFF7C3AED;
    private static final int CYAN = 0xFF22D3EE;

    private static final List<Item> CURATED = List.of(
            Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_AXE, Items.DIAMOND_AXE,
            Items.MACE, Items.TRIDENT, Items.BOW, Items.CROSSBOW,
            Items.SHIELD, Items.ELYTRA, Items.FISHING_ROD, Items.FLINT_AND_STEEL,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
            Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL, Items.OBSIDIAN, Items.RESPAWN_ANCHOR,
            Items.GLOWSTONE, Items.ENDER_PEARL, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT, Items.COOKED_BEEF, Items.COBWEB, Items.WIND_CHARGE,
            Items.FIREWORK_ROCKET, Items.ARROW, Items.SPECTRAL_ARROW, Items.POTION,
            Items.SPLASH_POTION, Items.LINGERING_POTION, Items.WATER_BUCKET, Items.LAVA_BUCKET,
            Items.ENCHANTED_BOOK, Items.EXPERIENCE_BOTTLE, Items.TNT, Items.ENDER_CHEST,
            Items.SHULKER_BOX, Items.PISTON, Items.STICKY_PISTON, Items.REDSTONE_BLOCK,
            Items.OBSERVER, Items.ANVIL, Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE);

    private int catIndex = 0;
    private int selected = -1;
    private boolean adding = false;
    private boolean confirmDelete = false;
    private String search = "";
    private int page = 0;

    private int gridX;
    private int gridY;
    private int pickX;
    private int pickY;
    private int panelX1;
    private int panelX2;
    private TextFieldWidget searchField;
    private TextFieldWidget nameField;
    private final List<ButtonWidget> pickerButtons = new ArrayList<>();
    private List<Item> pickerItems = new ArrayList<>();

    public KitScreen() {
        super(Text.literal("Gonas3434 Kitchecker"));
    }

    private Kit kit() {
        return KitStore.kits.get(catIndex);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------- Layout ----------

    private int col(int i) {
        if (i == 40) {
            return 4;
        }
        if (i >= 36) {
            return 39 - i;
        }
        if (i >= 9) {
            return (i - 9) % 9;
        }
        return i;
    }

    private int row(int i) {
        if (i >= 36) {
            return 0;
        }
        if (i >= 9) {
            return 1 + (i - 9) / 9;
        }
        return 4;
    }

    private int slotX(int i) {
        return gridX + col(i) * CELL;
    }

    private int slotY(int i) {
        int r = row(i);
        return gridY + r * CELL + (r >= 1 ? 4 : 0) + (r == 4 ? 4 : 0);
    }

    // ---------- Aufbau ----------

    @Override
    protected void init() {
        pickerButtons.clear();
        if (catIndex >= KitStore.kits.size()) {
            catIndex = 0;
        }
        if (adding) {
            initAdding();
            return;
        }

        Kit kit = kit();
        int totalW = 9 * CELL + 30 + 8 * CELL;
        gridX = (width - totalW) / 2;
        gridY = 80;
        pickX = gridX + 9 * CELL + 30;
        pickY = gridY;

        // Kategorie-Tabs
        int tabsTotal = 20;
        for (Kit k : KitStore.kits) {
            tabsTotal += textRenderer.getWidth(k.name) + 14 + 4;
        }
        int x = (width - tabsTotal) / 2;
        panelX1 = Math.min(gridX - 22, x - 14);
        panelX2 = Math.max(gridX + totalW + 22, x + tabsTotal + 14);

        for (int i = 0; i < KitStore.kits.size(); i++) {
            final int idx = i;
            int w = textRenderer.getWidth(KitStore.kits.get(i).name) + 14;
            ButtonWidget tab = ButtonWidget.builder(Text.literal(KitStore.kits.get(i).name), b -> {
                catIndex = idx;
                selected = -1;
                confirmDelete = false;
                clearAndInit();
            }).dimensions(x, 28, w, 20).build();
            tab.active = i != catIndex;
            addDrawableChild(tab);
            x += w + 4;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            adding = true;
            clearAndInit();
        }).dimensions(x, 28, 20, 20).build());

        // Kategorie loeschen (nur eigene)
        if (!kit.builtin) {
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(confirmDelete ? "Wirklich löschen?" : "Kategorie löschen"), b -> {
                        if (!confirmDelete) {
                            confirmDelete = true;
                        } else {
                            KitStore.kits.remove(catIndex);
                            catIndex = 0;
                            selected = -1;
                            confirmDelete = false;
                            KitStore.save();
                        }
                        clearAndInit();
                    }).dimensions(width / 2 - 55, height - 28, 110, 20).build());
        }

        // Slot-Buttons
        for (int i = 0; i < Kit.SLOTS; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
                selected = idx;
                clearAndInit();
            }).dimensions(slotX(i), slotY(i), CELL, CELL).build());
        }

        // Steuerung fuer den gewaehlten Slot
        KitEntry sel = selected >= 0 ? kit.slots[selected] : null;
        int ctrlY = gridY + 118;
        if (sel != null) {
            Item item = KitChecker.itemOf(sel.item);
            final int max = Math.max(1, item.getMaxCount());
            addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
                sel.count = Math.max(1, sel.count - 1);
                KitStore.save();
                clearAndInit();
            }).dimensions(gridX, ctrlY + 12, 20, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
                sel.count = Math.min(max, sel.count + 1);
                KitStore.save();
                clearAndInit();
            }).dimensions(gridX + 60, ctrlY + 12, 20, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Slot leeren"), b -> {
                kit.slots[selected] = null;
                KitStore.save();
                clearAndInit();
            }).dimensions(gridX + 90, ctrlY + 12, 80, 20).build());

            // Verzauberungen, die auf dieses Item passen
            if (client != null && client.world != null) {
                ItemStack probe = new ItemStack(item);
                Registry<Enchantment> reg = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                List<RegistryEntry.Reference<Enchantment>> list = reg.streamEntries()
                        .filter(en -> probe.isOf(Items.ENCHANTED_BOOK) || en.value().isAcceptableItem(probe))
                        .sorted(Comparator.comparing(en -> en.registryKey().getValue().toString()))
                        .collect(Collectors.toList());
                int ey = ctrlY + 38;
                for (int n = 0; n < list.size(); n++) {
                    RegistryEntry.Reference<Enchantment> en = list.get(n);
                    final String id = en.registryKey().getValue().toString();
                    final int maxLvl = en.value().getMaxLevel();
                    final int lvl = sel.enchants.getOrDefault(id, 0);
                    MutableText label = Text.translatable("enchantment." + id.replace(':', '.'));
                    if (lvl > 0) {
                        label.append(" " + lvl).formatted(Formatting.GREEN);
                    } else {
                        label.formatted(Formatting.GRAY);
                    }
                    addDrawableChild(ButtonWidget.builder(label, b -> {
                        int next = lvl + 1 > maxLvl ? 0 : lvl + 1;
                        if (next == 0) {
                            sel.enchants.remove(id);
                        } else {
                            sel.enchants.put(id, next);
                        }
                        KitStore.save();
                        clearAndInit();
                    }).dimensions(gridX + (n % 3) * 125, ey + (n / 3) * 17, 123, 16).build());
                }
            }
        }

        // Item-Auswahl rechts (immer sichtbar)
        searchField = new TextFieldWidget(textRenderer, pickX, pickY, 8 * CELL, 18, Text.literal("Suche"));
        searchField.setMaxLength(40);
        searchField.setText(search);
        searchField.setPlaceholder(Text.literal("Item suchen..."));
        searchField.setChangedListener(s -> {
            search = s;
            page = 0;
            refreshPicker();
        });
        addDrawableChild(searchField);

        for (int i = 0; i < PER_PAGE; i++) {
            final int cell = i;
            ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> {
                int idx = page * PER_PAGE + cell;
                if (selected >= 0 && idx < pickerItems.size()) {
                    kit.slots[selected] = new KitEntry(Registries.ITEM.getId(pickerItems.get(idx)).toString());
                    KitStore.save();
                    clearAndInit();
                }
            }).dimensions(pickX + (i % 8) * CELL, pickY + 24 + (i / 8) * CELL, CELL, CELL).build();
            pickerButtons.add(b);
            addDrawableChild(b);
        }
        int navY = pickY + 24 + 5 * CELL + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = Math.max(0, page - 1);
            refreshPicker();
        }).dimensions(pickX, navY, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            refreshPicker();
        }).dimensions(pickX + 8 * CELL - 20, navY, 20, 20).build());

        refreshPicker();
    }

    private void initAdding() {
        nameField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20, Text.literal("Name"));
        nameField.setMaxLength(24);
        nameField.setPlaceholder(Text.literal("z. B. Elytra-Snipe"));
        addDrawableChild(nameField);
        setInitialFocus(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Erstellen"), b -> {
            String name = nameField.getText().trim();
            boolean exists = false;
            for (Kit k : KitStore.kits) {
                if (k.name.equalsIgnoreCase(name)) {
                    exists = true;
                }
            }
            if (name.isEmpty() || exists) {
                return;
            }
            KitStore.kits.add(new Kit(name, false));
            catIndex = KitStore.kits.size() - 1;
            selected = -1;
            adding = false;
            KitStore.save();
            clearAndInit();
        }).dimensions(width / 2 - 100, height / 2 + 16, 98, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Abbrechen"), b -> {
            adding = false;
            clearAndInit();
        }).dimensions(width / 2 + 2, height / 2 + 16, 98, 20).build());
    }

    private void refreshPicker() {
        List<Item> result = new ArrayList<>();
        String q = search.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            result.addAll(CURATED);
        } else {
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                String path = Registries.ITEM.getId(item).getPath();
                String name = item.getName().getString().toLowerCase(Locale.ROOT);
                if (path.contains(q) || name.contains(q)) {
                    result.add(item);
                }
            }
        }
        pickerItems = result;
        int pages = Math.max(1, (pickerItems.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) {
            page = pages - 1;
        }
        for (int i = 0; i < pickerButtons.size(); i++) {
            boolean has = page * PER_PAGE + i < pickerItems.size();
            pickerButtons.get(i).visible = has;
            pickerButtons.get(i).active = has;
        }
    }

    // ---------- Zeichnen ----------

    private void center(DrawContext ctx, Text text, int cx, int y, int color) {
        ctx.drawText(textRenderer, text, cx - textRenderer.getWidth(text) / 2, y, color, true);
    }

    private void frame(DrawContext ctx, int x1, int y1, int x2, int y2, int fill, int border) {
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, border);
        ctx.fill(x1, y1, x2, y2, fill);
    }

    /** Hintergrund-Design: Panel, Titelleiste, Karten. Wird hinter den Buttons gezeichnet. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);

        if (adding) {
            int x1 = width / 2 - 120;
            int x2 = width / 2 + 120;
            int y1 = height / 2 - 44;
            int y2 = height / 2 + 48;
            frame(ctx, x1 - 2, y1 - 2, x2 + 2, y2 + 2, 0xF0101018, PURPLE);
            ctx.fill(x1, y1, x2, y1 + 2, CYAN);
            return;
        }

        int x1 = panelX1;
        int x2 = panelX2;
        int y1 = 2;
        int y2 = height - 34;
        int mid = (x1 + x2) / 2;

        // Aeusserer Rahmen (lila + cyan) und Koerper
        ctx.fill(x1 - 3, y1 - 1, x2 + 3, y2 + 3, PURPLE);
        ctx.fill(x1 - 2, y1, x2 + 2, y2 + 2, CYAN);
        ctx.fill(x1 - 1, y1 + 1, x2 + 1, y2 + 1, 0xFF0B0B14);
        ctx.fill(x1, y1 + 2, x2, y2, 0xF0101018);

        // Titelleiste mit zweifarbiger Akzentlinie
        ctx.fill(x1, y1 + 2, x2, y1 + 24, 0xFF1B1030);
        ctx.fill(x1, y1 + 24, mid, y1 + 26, PURPLE);
        ctx.fill(mid, y1 + 24, x2, y1 + 26, CYAN);

        Text title = Text.literal("Gonas3434").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal(" Kitchecker").formatted(Formatting.AQUA, Formatting.BOLD));
        center(ctx, title, width / 2, y1 + 9, 0xFFFFFFFF);

        // Karten: Kit links, Items rechts, Details unten
        int cardTop = gridY - 17;
        int kitRight = gridX + 9 * CELL + 8;
        int pickLeft = pickX - 8;
        int pickRight = pickX + 8 * CELL + 8;
        int pickBottom = pickY + 24 + 5 * CELL + 28;
        frame(ctx, gridX - 8, cardTop, kitRight, gridY + 5 * CELL + 12, 0xFF171726, 0xFF2E2E48);
        frame(ctx, pickLeft, cardTop, pickRight, pickBottom, 0xFF171726, 0xFF2E2E48);
        frame(ctx, gridX - 8, gridY + 5 * CELL + 18, pickRight, y2 - 6, 0xFF171726, 0xFF2E2E48);

        // Farbige Kopfstreifen der Karten
        ctx.fill(gridX - 8, cardTop, kitRight, cardTop + 2, PURPLE);
        ctx.fill(pickLeft, cardTop, pickRight, cardTop + 2, CYAN);
        ctx.fill(gridX - 8, gridY + 5 * CELL + 18, pickRight, gridY + 5 * CELL + 20, 0xFF3B82F6);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        if (adding) {
            center(ctx, Text.literal("Neue Kategorie").formatted(Formatting.AQUA, Formatting.BOLD),
                    width / 2, height / 2 - 36, 0xFFFFFFFF);
            return;
        }

        Kit kit = kit();
        boolean[] missing = (client != null && client.player != null)
                ? KitChecker.findMissing(kit, client.player) : new boolean[Kit.SLOTS];

        // Statuszeile
        int missingCount = 0;
        int itemCount = 0;
        for (int i = 0; i < Kit.SLOTS; i++) {
            if (kit.slots[i] != null) {
                itemCount++;
                if (missing[i]) {
                    missingCount++;
                }
            }
        }
        if (itemCount == 0) {
            center(ctx, Text.literal("Noch kein Kit - wähle einen Slot und dann rechts ein Item"),
                    width / 2, 54, 0xFFAAAAAA);
        } else if (missingCount == 0) {
            center(ctx, Text.literal("✔ Kit komplett - du hast alles!"), width / 2, 54, 0xFF55FF55);
        } else {
            center(ctx, Text.literal("✘ Es fehlen " + missingCount + " von " + itemCount + " Items"),
                    width / 2, 54, 0xFFFF5555);
        }

        // Kartenueberschriften
        ctx.drawText(textRenderer, Text.literal("Dein Kit").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD),
                gridX, gridY - 12, 0xFFFFFFFF, true);
        String hint = "Helm > Schuhe, Offhand";
        ctx.drawText(textRenderer, Text.literal(hint), gridX + 9 * CELL - textRenderer.getWidth(hint),
                gridY - 12, 0xFF777799, false);
        ctx.drawText(textRenderer, Text.literal("Items").formatted(Formatting.AQUA, Formatting.BOLD),
                pickX, gridY - 12, 0xFFFFFFFF, true);

        List<Text> tooltip = null;

        for (int i = 0; i < Kit.SLOTS; i++) {
            int sx = slotX(i);
            int sy = slotY(i);

            KitEntry entry = kit.slots[i];
            if (entry != null) {
                ItemStack stack = KitChecker.stackOf(entry);
                ctx.drawItem(stack, sx + 2, sy + 2);
                ctx.drawStackOverlay(textRenderer, stack, sx + 2, sy + 2);
                if (missing[i]) {
                    ctx.fill(sx, sy, sx + CELL, sy + CELL, 0x80FF0000);
                }
            }

            if (i == selected) {
                ctx.fill(sx, sy, sx + CELL, sy + 1, CYAN);
                ctx.fill(sx, sy + CELL - 1, sx + CELL, sy + CELL, CYAN);
                ctx.fill(sx, sy, sx + 1, sy + CELL, CYAN);
                ctx.fill(sx + CELL - 1, sy, sx + CELL, sy + CELL, CYAN);
            }

            if (entry != null && mouseX >= sx && mouseX < sx + CELL && mouseY >= sy && mouseY < sy + CELL) {
                tooltip = new ArrayList<>();
                tooltip.add(KitChecker.stackOf(entry).getName());
                if (entry.count > 1) {
                    tooltip.add(Text.literal("Anzahl: " + entry.count).formatted(Formatting.GRAY));
                }
                for (var e : entry.enchants.entrySet()) {
                    tooltip.add(Text.translatable("enchantment." + e.getKey().replace(':', '.'))
                            .append(" " + e.getValue()).formatted(Formatting.AQUA));
                }
                if (missing[i]) {
                    tooltip.add(Text.literal("Fehlt im Inventar!").formatted(Formatting.RED));
                }
            }
        }

        // Details zum gewaehlten Slot
        int ctrlY = gridY + 118;
        KitEntry sel = selected >= 0 ? kit.slots[selected] : null;
        if (selected < 0) {
            ctx.drawText(textRenderer, Text.literal("Klicke oben links auf einen Slot, dann rechts auf ein Item."),
                    gridX, ctrlY, 0xFFFFFF55, false);
        } else if (sel == null) {
            ctx.drawText(textRenderer, Text.literal("Slot ist leer - klicke rechts auf ein Item."),
                    gridX, ctrlY, 0xFFFFFF55, false);
        } else {
            ctx.drawText(textRenderer, KitChecker.stackOf(sel).getName(), gridX, ctrlY, 0xFFFFFFFF, true);
            ctx.drawText(textRenderer, Text.literal(String.valueOf(sel.count)),
                    gridX + 40 - textRenderer.getWidth(String.valueOf(sel.count)) / 2, ctrlY + 18, 0xFFFFFFFF, true);
        }

        // Item-Auswahl: Items ueber die Buttons zeichnen
        for (int i = 0; i < pickerButtons.size(); i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= pickerItems.size()) {
                continue;
            }
            int bx = pickX + (i % 8) * CELL;
            int by = pickY + 24 + (i / 8) * CELL;
            Item item = pickerItems.get(idx);
            ctx.drawItem(new ItemStack(item), bx + 2, by + 2);
            if (mouseX >= bx && mouseX < bx + CELL && mouseY >= by && mouseY < by + CELL) {
                tooltip = new ArrayList<>();
                tooltip.add(item.getName());
            }
        }
        int pages = Math.max(1, (pickerItems.size() + PER_PAGE - 1) / PER_PAGE);
        center(ctx, Text.literal((page + 1) + " / " + pages), pickX + 4 * CELL, pickY + 24 + 5 * CELL + 10, 0xFFFFFFFF);

        if (tooltip != null) {
            ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }
}
