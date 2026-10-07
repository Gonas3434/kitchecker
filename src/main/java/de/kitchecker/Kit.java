package de.kitchecker;

/**
 * Ein Kit einer Kategorie. Slots: 0-8 Hotbar, 9-35 Inventar,
 * 36 Schuhe, 37 Hose, 38 Brust, 39 Helm, 40 Offhand.
 */
public class Kit {
    public static final int SLOTS = 41;

    public String name;
    public boolean builtin;
    public KitEntry[] slots = new KitEntry[SLOTS];

    public Kit() {
    }

    public Kit(String name, boolean builtin) {
        this.name = name;
        this.builtin = builtin;
    }
}
