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
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Gonas3434 Kitchecker.
 * Oben: Kategorien, links dein Kit, rechts Details zum gewaehlten Slot (Anzahl, Verzauberungen).
 * Unten: alle Items. Fehlende Items im Inventar werden rot markiert.
 */
public class KitScreen extends Screen {
    private static final int CELL = 20;
    private static final int TOP = 58;
    private static final int TEXT = 0xFF404040;
    private static final int GRAY = 0xFF707070;

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
    private boolean copying = false;
    private boolean confirmDelete = false;
    private boolean confirmImport = false;
    private String notice = null;
    private String search = "";
    private int page = 0;

    // Layout
    private int px1;
    private int px2;
    private int panelY2;
    private int gridX;
    private int gridY;
    private int kitCardX1;
    private int kitCardX2;
    private int rightCardX1;
    private int rightCardX2;
    private int rightX;
    private int rightW;
    private int topBottom;
    private int itemsTop;
    private int itemsCardBottom;
    private int itemsX;
    private int itemsGridY;
    private int cols;
    private int rows;
    private int perPage;
    private int dlgX1;
    private int dlgY1;
    private int dlgX2;
    private int dlgY2;

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

    private static boolean hasItems(Kit k) {
        for (KitEntry e : k.slots) {
            if (e != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---------- Slot-Positionen ----------

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
        if (copying) {
            initCopying();
            return;
        }

        Kit kit = kit();
        final int m = 10;

        int tabsTotal = 20;
        for (Kit k : KitStore.kits) {
            tabsTotal += textRenderer.getWidth(k.name) + 14 + 4;
        }
        int pw = Math.min(width - 8, Math.max(Math.min(440, width - 16), tabsTotal + 2 * m));
        px1 = (width - pw) / 2;
        px2 = px1 + pw;

        kitCardX1 = px1 + m;
        gridX = kitCardX1 + 6;
        kitCardX2 = gridX + 9 * CELL + 6;
        rightCardX1 = kitCardX2 + 6;
        rightCardX2 = px2 - m;
        rightX = rightCardX1 + 6;
        rightW = rightCardX2 - 6 - rightX;
        gridY = TOP + 20;
        topBottom = TOP + 136;
        itemsTop = topBottom + 6;
        itemsGridY = itemsTop + 36;

        int innerW = pw - 2 * m - 12;
        cols = Math.max(8, Math.min(20, innerW / CELL));
        rows = Math.max(2, Math.min(8, (height - 4 - 34 - 6 - (itemsGridY + 26)) / CELL));
        perPage = cols * rows;
        itemsX = px1 + (pw - cols * CELL) / 2;
        int navY = itemsGridY + rows * CELL + 4;
        itemsCardBottom = navY + 20 + 6;
        panelY2 = itemsCardBottom + 34;

        // Kategorie-Tabs
        int x = (width - tabsTotal) / 2;
        for (int i = 0; i < KitStore.kits.size(); i++) {
            final int idx = i;
            int w = textRenderer.getWidth(KitStore.kits.get(i).name) + 14;
            ButtonWidget tab = ButtonWidget.builder(Text.literal(KitStore.kits.get(i).name), b -> {
                catIndex = idx;
                selected = -1;
                notice = null;
                confirmDelete = false;
                confirmImport = false;
                page = 0;
                clearAndInit();
            }).dimensions(x, 24, w, 20).build();
            tab.active = i != catIndex;
            addDrawableChild(tab);
            x += w + 4;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            adding = true;
            clearAndInit();
        }).dimensions(x, 24, 20, 20).build());

        // Untere Leiste: Import, Kopieren, Loeschen
        boolean nonEmpty = hasItems(kit);
        String importLabel = (confirmImport && nonEmpty) ? "Wirklich überschreiben?" : "Vom Inventar importieren";
        String copyLabel = "Kit kopieren nach...";
        String delLabel = confirmDelete ? "Wirklich löschen?" : "Kategorie löschen";
        int wImp = textRenderer.getWidth(importLabel) + 16;
        int wCopy = textRenderer.getWidth(copyLabel) + 16;
        int wDel = kit.builtin ? 0 : textRenderer.getWidth(delLabel) + 16;
        int barTotal = wImp + 6 + wCopy + (kit.builtin ? 0 : 6 + wDel);
        int bx = (width - barTotal) / 2;
        int barY = panelY2 - 28;

        addDrawableChild(ButtonWidget.builder(Text.literal(importLabel), b -> {
            if (client == null || client.player == null) {
                return;
            }
            if (hasItems(kit) && !confirmImport) {
                confirmImport = true;
            } else {
                KitChecker.importFromPlayer(kit, client.player);
                KitStore.save();
                confirmImport = false;
                selected = -1;
                notice = "Inventar in \"" + kit.name + "\" importiert";
            }
            clearAndInit();
        }).dimensions(bx, barY, wImp, 20).build());
        bx += wImp + 6;

        addDrawableChild(ButtonWidget.builder(Text.literal(copyLabel), b -> {
            copying = true;
            clearAndInit();
        }).dimensions(bx, barY, wCopy, 20).build());
        bx += wCopy + 6;

        if (!kit.builtin) {
            addDrawableChild(ButtonWidget.builder(Text.literal(delLabel), b -> {
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
            }).dimensions(bx, barY, wDel, 20).build());
        }

        // Slot-Buttons (werden in render() im Slot-Look uebermalt)
        for (int i = 0; i < Kit.SLOTS; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
                selected = idx;
                notice = null;
                confirmImport = false;
                clearAndInit();
            }).dimensions(slotX(i), slotY(i), CELL, CELL).build());
        }

        // Details zum gewaehlten Slot
        KitEntry sel = selected >= 0 ? kit.slots[selected] : null;
        if (sel != null) {
            Item item = KitChecker.itemOf(sel.item);
            final int max = Math.max(1, item.getMaxCount());
            int cy = TOP + 28;
            int cx = rightX;

            if (max > 1) {
                TextFieldWidget countField = new TextFieldWidget(textRenderer, rightX + 42, cy, 36, 20, Text.literal("Anzahl"));
                countField.setMaxLength(3);
                countField.setText(String.valueOf(sel.count));
                countField.setTextPredicate(s -> s.isEmpty() || (s.matches("\\d{1,3}") && Integer.parseInt(s) <= max));
                countField.setChangedListener(s -> {
                    if (!s.isEmpty()) {
                        sel.count = Math.max(1, Integer.parseInt(s));
                        KitStore.save();
                    }
                });
                addDrawableChild(countField);
                addDrawableChild(ButtonWidget.builder(Text.literal("Max"), b -> {
                    sel.count = max;
                    KitStore.save();
                    clearAndInit();
                }).dimensions(rightX + 82, cy, 34, 20).build());
                cx = rightX + 120;
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Leeren"), b -> {
                kit.slots[selected] = null;
                KitStore.save();
                clearAndInit();
            }).dimensions(cx, cy, Math.max(40, rightX + rightW - cx), 20).build());

            // Verzauberungen, die auf dieses Item passen
            if (client != null && client.world != null) {
                ItemStack probe = new ItemStack(item);
                Registry<Enchantment> reg = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                List<RegistryEntry.Reference<Enchantment>> list = reg.streamEntries()
                        .filter(en -> probe.isOf(Items.ENCHANTED_BOOK) || en.value().isAcceptableItem(probe))
                        .sorted(Comparator.comparing(en -> en.registryKey().getValue().toString()))
                        .limit(10)
                        .collect(Collectors.toList());
                int colW = (rightW - 2) / 2;
                int ey = TOP + 52;
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
                    }).dimensions(rightX + (n % 2) * (colW + 2), ey + (n / 2) * 15, colW, 14).build());
                }
            }
        }

        // Item-Auswahl unten
        searchField = new TextFieldWidget(textRenderer, itemsX, itemsTop + 14, Math.min(cols * CELL, 240), 18, Text.literal("Suche"));
        searchField.setMaxLength(40);
        searchField.setText(search);
        searchField.setPlaceholder(Text.literal("Item suchen..."));
        searchField.setChangedListener(s -> {
            search = s;
            page = 0;
            refreshPicker();
        });
        addDrawableChild(searchField);

        for (int i = 0; i < perPage; i++) {
            final int cell = i;
            ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> {
                int idx = page * perPage + cell;
                if (idx >= pickerItems.size()) {
                    return;
                }
                if (selected < 0) {
                    notice = "Wähle zuerst oben einen Slot in deinem Kit";
                    return;
                }
                kit.slots[selected] = new KitEntry(Registries.ITEM.getId(pickerItems.get(idx)).toString());
                KitStore.save();
                notice = null;
                clearAndInit();
            }).dimensions(itemsX + (i % cols) * CELL, itemsGridY + (i / cols) * CELL, CELL, CELL).build();
            pickerButtons.add(b);
            addDrawableChild(b);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page = Math.max(0, page - 1);
            refreshPicker();
        }).dimensions(itemsX, navY, 20, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            refreshPicker();
        }).dimensions(itemsX + cols * CELL - 20, navY, 20, 20).build());

        refreshPicker();
    }

    private void initAdding() {
        dlgX1 = width / 2 - 120;
        dlgX2 = width / 2 + 120;
        dlgY1 = height / 2 - 48;
        dlgY2 = height / 2 + 52;

        nameField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 12, 200, 20, Text.literal("Name"));
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
            notice = null;
            adding = false;
            KitStore.save();
            clearAndInit();
        }).dimensions(width / 2 - 100, height / 2 + 16, 98, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Abbrechen"), b -> {
            adding = false;
            clearAndInit();
        }).dimensions(width / 2 + 2, height / 2 + 16, 98, 20).build());
    }

    private void initCopying() {
        final Kit src = kit();
        int n = KitStore.kits.size() - 1;
        int w = 200;
        int h = 56 + n * 24 + 4;
        dlgX1 = width / 2 - w / 2 - 12;
        dlgX2 = width / 2 + w / 2 + 12;
        dlgY1 = height / 2 - h / 2;
        dlgY2 = height / 2 + h / 2 + 12;

        int y = dlgY1 + 28;
        for (int i = 0; i < KitStore.kits.size(); i++) {
            if (i == catIndex) {
                continue;
            }
            final int ti = i;
            Kit target = KitStore.kits.get(i);
            String label = target.name + (hasItems(target) ? " (überschreibt)" : "");
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                Kit t = KitStore.kits.get(ti);
                for (int s = 0; s < Kit.SLOTS; s++) {
                    t.slots[s] = src.slots[s] == null ? null : src.slots[s].copy();
                }
                KitStore.save();
                catIndex = ti;
                selected = -1;
                copying = false;
                notice = "Kit nach \"" + t.name + "\" kopiert";
                clearAndInit();
            }).dimensions(width / 2 - w / 2, y, w, 20).build());
            y += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Abbrechen"), b -> {
            copying = false;
            clearAndInit();
        }).dimensions(width / 2 - w / 2, y + 4, w, 20).build());
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
        int pages = Math.max(1, (pickerItems.size() + perPage - 1) / perPage);
        if (page >= pages) {
            page = pages - 1;
        }
        for (int i = 0; i < pickerButtons.size(); i++) {
            boolean has = page * perPage + i < pickerItems.size();
            pickerButtons.get(i).visible = has;
            pickerButtons.get(i).active = has;
        }
    }

    // ---------- Zeichnen ----------

    /** Graues Minecraft-Inventar-Panel. */
    private void panel(DrawContext ctx, int x1, int y1, int x2, int y2) {
        ctx.fill(x1, y1, x2, y2, 0xFF000000);
        ctx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFFFFFFFF);
        ctx.fill(x1 + 3, y1 + 3, x2 - 1, y2 - 1, 0xFF555555);
        ctx.fill(x1 + 3, y1 + 3, x2 - 3, y2 - 3, 0xFFC6C6C6);
    }

    /** Eingelassener Bereich. */
    private void sunken(DrawContext ctx, int x1, int y1, int x2, int y2) {
        ctx.fill(x1, y1, x2, y2, 0xFF555555);
        ctx.fill(x1 + 1, y1 + 1, x2, y2, 0xFFFFFFFF);
        ctx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFFC6C6C6);
    }

    private void slot(DrawContext ctx, int x, int y, boolean hover) {
        ctx.fill(x, y, x + CELL, y + CELL, 0xFF373737);
        ctx.fill(x + 1, y + 1, x + CELL, y + CELL, 0xFFFFFFFF);
        ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xFF8B8B8B);
        if (hover) {
            ctx.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x80FFFFFF);
        }
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(textRenderer, Text.literal(s), x, y, color, false);
    }

    private void centered(DrawContext ctx, Text t, int cx, int y, int color) {
        ctx.drawText(textRenderer, t, cx - textRenderer.getWidth(t) / 2, y, color, false);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);

        if (adding || copying) {
            panel(ctx, dlgX1, dlgY1, dlgX2, dlgY2);
            return;
        }

        panel(ctx, px1, 4, px2, panelY2);
        sunken(ctx, kitCardX1, TOP, kitCardX2, topBottom);
        sunken(ctx, rightCardX1, TOP, rightCardX2, topBottom);
        sunken(ctx, px1 + 10, itemsTop, px2 - 10, itemsCardBottom);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        if (adding) {
            centered(ctx, Text.literal("Neue Kategorie"), width / 2, dlgY1 + 10, TEXT);
            return;
        }
        if (copying) {
            centered(ctx, Text.literal("Kit \"" + kit().name + "\" kopieren nach:"), width / 2, dlgY1 + 10, TEXT);
            return;
        }

        Kit kit = kit();
        boolean[] missing = (client != null && client.player != null)
                ? KitChecker.findMissing(kit, client.player) : new boolean[Kit.SLOTS];

        centered(ctx, Text.literal("Gonas3434 Kitchecker").formatted(Formatting.BOLD), width / 2, 10, TEXT);

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
        if (notice != null) {
            centered(ctx, Text.literal(notice), width / 2, 47, 0xFF005500);
        } else if (itemCount == 0) {
            centered(ctx, Text.literal("Noch leer - Slot wählen und unten ein Item anklicken, oder vom Inventar importieren"),
                    width / 2, 47, GRAY);
        } else if (missingCount == 0) {
            centered(ctx, Text.literal("Kit komplett - du hast alles"), width / 2, 47, 0xFF007700);
        } else {
            centered(ctx, Text.literal("Es fehlen " + missingCount + " von " + itemCount + " Items"), width / 2, 47, 0xFFAA0000);
        }

        // Ueberschriften
        text(ctx, "Dein Kit", gridX, TOP + 6, TEXT);
        String hint = "Helm > Schuhe, Offhand";
        text(ctx, hint, gridX + 9 * CELL - textRenderer.getWidth(hint), TOP + 6, GRAY);
        text(ctx, "Slot-Details", rightX, TOP + 6, TEXT);
        text(ctx, "Items", itemsX, itemsTop + 4, TEXT);
        if (search.isBlank()) {
            String h2 = "Suche zeigt alle Items";
            text(ctx, h2, itemsX + cols * CELL - textRenderer.getWidth(h2), itemsTop + 4, GRAY);
        }

        List<Text> tooltip = null;

        // Kit-Slots
        for (int i = 0; i < Kit.SLOTS; i++) {
            int sx = slotX(i);
            int sy = slotY(i);
            boolean hover = mouseX >= sx && mouseX < sx + CELL && mouseY >= sy && mouseY < sy + CELL;
            slot(ctx, sx, sy, hover);

            KitEntry entry = kit.slots[i];
            if (entry != null) {
                ItemStack stack = KitChecker.stackOf(entry);
                ctx.drawItem(stack, sx + 2, sy + 2);
                ctx.drawStackOverlay(textRenderer, stack, sx + 2, sy + 2);
                if (missing[i]) {
                    ctx.fill(sx + 1, sy + 1, sx + CELL - 1, sy + CELL - 1, 0x80FF0000);
                }
            }

            if (i == selected) {
                int c = 0xFFFFAA00;
                ctx.fill(sx, sy, sx + CELL, sy + 2, c);
                ctx.fill(sx, sy + CELL - 2, sx + CELL, sy + CELL, c);
                ctx.fill(sx, sy, sx + 2, sy + CELL, c);
                ctx.fill(sx + CELL - 2, sy, sx + CELL, sy + CELL, c);
            }

            if (entry != null && hover) {
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

        // Details-Karte
        KitEntry sel = selected >= 0 ? kit.slots[selected] : null;
        if (selected < 0) {
            drawWrapped(ctx, "Klicke links auf einen Slot, dann unten auf ein Item.", rightX, TOP + 20, rightW);
        } else if (sel == null) {
            drawWrapped(ctx, "Slot ist leer. Klicke unten auf ein Item.", rightX, TOP + 20, rightW);
        } else {
            ctx.drawText(textRenderer, KitChecker.stackOf(sel).getName(), rightX, TOP + 16, TEXT, false);
            if (Math.max(1, KitChecker.itemOf(sel.item).getMaxCount()) > 1) {
                text(ctx, "Anzahl", rightX, TOP + 34, TEXT);
            }
        }

        // Item-Auswahl
        for (int i = 0; i < pickerButtons.size(); i++) {
            int idx = page * perPage + i;
            if (idx >= pickerItems.size()) {
                continue;
            }
            int bx = itemsX + (i % cols) * CELL;
            int by = itemsGridY + (i / cols) * CELL;
            boolean hover = mouseX >= bx && mouseX < bx + CELL && mouseY >= by && mouseY < by + CELL;
            slot(ctx, bx, by, hover);
            Item item = pickerItems.get(idx);
            ctx.drawItem(new ItemStack(item), bx + 2, by + 2);
            if (hover) {
                tooltip = new ArrayList<>();
                tooltip.add(item.getName());
            }
        }
        int pages = Math.max(1, (pickerItems.size() + perPage - 1) / perPage);
        centered(ctx, Text.literal((page + 1) + " / " + pages), itemsX + cols * CELL / 2,
                itemsGridY + rows * CELL + 10, TEXT);

        if (tooltip != null) {
            ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private void drawWrapped(DrawContext ctx, String s, int x, int y, int w) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(s), w);
        int ly = y;
        for (OrderedText line : lines) {
            ctx.drawText(textRenderer, line, x, ly, GRAY, false);
            ly += 10;
        }
    }
}
