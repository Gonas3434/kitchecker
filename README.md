# Kit Checker (Fabric, Minecraft 1.21.11)

Taste **K** (einstellbar unter Steuerung > "Kit Checker") öffnet das GUI.

- Oben: Kategorien Duels, 2v2 Duels, Clan Duels, Rtpqueue. Mit **+** eigene Kategorien anlegen (z. B. Elytra-Snipe).
- **Bearbeiten**: Slot anklicken, rechts Item wählen (Suche zeigt alle Items), Anzahl mit - / +, Verzauberungen durch Anklicken hochschalten (bis zum Maximallevel, dann aus).
- Ansicht: Was dir im Inventar fehlt, wird rot markiert. Der Mod zeigt nur an, er verschiebt nichts.
- Gespeichert wird in `config/kitchecker.json`.

Geprüft wird: Item, Mindestanzahl und Verzauberungen (egal in welchem Slot). Tränke/Pfeile werden nur nach Item-Art verglichen, nicht nach Effekt.

## Jar bauen

**Variante A (ohne Installation): GitHub**
1. Neues Repository auf github.com anlegen, den gesamten Inhalt dieses Ordners hochladen (inkl. Ordner `.github`).
2. Tab **Actions** > "build" läuft automatisch. Danach unter dem Lauf bei **Artifacts** `kitchecker-jar` herunterladen.
3. Die Jar (nicht die `-sources`) in den `mods`-Ordner legen. Fabric Loader + Fabric API für 1.21.11 müssen installiert sein.

**Variante B (lokal)**
1. Java 21 und IntelliJ IDEA installieren.
2. Auf https://fabricmc.net/develop/template ein Projekt generieren (Mod-ID `kitchecker`, Minecraft 1.21.11, Yarn, Java).
3. Dort den Ordner `src` durch den `src` aus diesem Projekt ersetzen, danach `./gradlew build` (Windows: `gradlew.bat build`). Die Jar liegt in `build/libs/`.
