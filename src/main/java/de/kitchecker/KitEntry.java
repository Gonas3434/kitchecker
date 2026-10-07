package de.kitchecker;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ein Item im Kit: Item-ID, Anzahl und gewünschte Verzauberungen (ID -> Level). */
public class KitEntry {
    public String item;
    public int count = 1;
    public Map<String, Integer> enchants = new LinkedHashMap<>();

    public KitEntry() {
    }

    public KitEntry(String item) {
        this.item = item;
    }

    public KitEntry copy() {
        KitEntry e = new KitEntry(item);
        e.count = count;
        if (enchants != null) {
            e.enchants.putAll(enchants);
        }
        return e;
    }
}
