package de.kitchecker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Laden und Speichern aller Kits (config/kitchecker.json). */
public final class KitStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("kitchecker");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] BUILTIN = {"Duels", "2v2 Duels", "Clan Duels", "Rtpqueue"};

    public static final List<Kit> kits = new ArrayList<>();

    private KitStore() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("kitchecker.json");
    }

    public static void load() {
        List<Kit> loaded = new ArrayList<>();
        try {
            Path f = file();
            if (Files.exists(f)) {
                try (Reader r = Files.newBufferedReader(f)) {
                    Kit[] arr = GSON.fromJson(r, Kit[].class);
                    if (arr != null) {
                        loaded.addAll(Arrays.asList(arr));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Konnte Kits nicht laden", e);
        }

        kits.clear();
        for (String name : BUILTIN) {
            Kit found = null;
            for (Kit k : loaded) {
                if (k != null && k.builtin && name.equals(k.name)) {
                    found = k;
                    break;
                }
            }
            kits.add(found != null ? found : new Kit(name, true));
        }
        for (Kit k : loaded) {
            if (k != null && !k.builtin && k.name != null) {
                kits.add(k);
            }
        }
        for (Kit k : kits) {
            if (k.slots == null) {
                k.slots = new KitEntry[Kit.SLOTS];
            } else if (k.slots.length != Kit.SLOTS) {
                k.slots = Arrays.copyOf(k.slots, Kit.SLOTS);
            }
            for (KitEntry e : k.slots) {
                if (e != null && e.enchants == null) {
                    e.enchants = new java.util.LinkedHashMap<>();
                }
            }
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), GSON.toJson(kits));
        } catch (Exception e) {
            LOGGER.error("Konnte Kits nicht speichern", e);
        }
    }
}
