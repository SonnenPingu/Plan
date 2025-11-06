
🏁 Todo: Plan Sponge zu NeoForge Portierung
1. Setup & Vorbereitung
 * [ ] Neues Modul plan-neoforge im Build-System (z.B. Gradle) anlegen.
 * [ ] NeoForge 1.21.1 API als Abhängigkeit hinzufügen.
 * [ ] plan-sponge Quellcode als Blaupause in das plan-neoforge Verzeichnis kopieren.
 * [ ] Alle Sponge-Abhängigkeiten in der Build-Datei durch NeoForge-Abhängigkeiten ersetzen.
2. Kern-Implementierung (Übersetzung)
 * [ ] Haupt-Mod-Klasse (PlanNeoForge.java) erstellen und den Mod-Lifecycle (Konstruktor, FMLCommonSetupEvent etc.) implementieren.
 * [ ] NeoForge Event-Bus für die Listener-Klasse registrieren.
 * [ ] Eigene API-Events (z.B. PlanSpongeEnableEvent zu PlanNeoForgeEnableEvent) auf die NeoForge Event-API (extends Event) umschreiben.
 * [ ] Befehls-Registrierung von Sponge auf das NeoForge-System (RegisterCommandsEvent) übersetzen.
 * [ ] Alle Abstraktionsklassen übersetzen:
   * [ ] SpongePlayer -> NeoForgePlayer
   * [ ] SpongeServer -> NeoForgeServer
   * [ ] SpongeTask -> NeoForgeTask (Plan's Scheduling-System an NeoForge anbinden)
3. Event-Listener (Übersetzung)
 * [ ] SpongeListener zu NeoForgeListener umschreiben.
 * [ ] Alle Sponge-Events (@Listener) durch NeoForge-Äquivalente (@SubscribeEvent) ersetzen.
   * [ ] ServerSideConnectionEvent.Join -> PlayerEvent.PlayerLoggedInEvent
   * [ ] ServerSideConnectionEvent.Disconnect -> PlayerEvent.PlayerLoggedOutEvent
   * [ ] ChangeBlockEvent.Break -> BlockEvent.Break
   * [ ] ChangeBlockEvent.Place -> BlockEvent.Place
   * [ ] ... (alle weiteren Events, die Plan nutzt, wie Chat, World-Load etc.)
4. Abschluss & Test
 * [ ] Alle verbleibenden import org.spongepowered... im plan-neoforge Modul finden und entfernen/ersetzen.
 * [ ] Das Modul erfolgreich kompilieren (./gradlew build).
 * [ ] Die kompilierte JAR-Datei auf einem dedizierten NeoForge 1.21.1 Test-Server laden.
 * [ ] Iterativer Prozess (Debuggen): Server starten, Funktionalität testen (Login, Web-UI, Befehle), Abstürze und Fehler analysieren, Code anpassen und neu kompilieren.

