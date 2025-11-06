/*
 * This file is part of Player Analytics (Plan).
 *
 * Plan is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License v3 as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Plan is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan;

import com.djrapitops.plan.commands.PlanCommand;
import com.djrapitops.plan.commands.neoforge.NeoForgeCommand; // NEU: Muss erstellt werden
import com.djrapitops.plan.commands.use.ColorScheme;
import com.djrapitops.plan.commands.use.Subcommand;
import com.djrapitops.plan.exceptions.EnableException;
import com.djrapitops.plan.gathering.ServerShutdownSave;
import com.djrapitops.plan.modules.PlatformAbstractionLayerModule;
import com.djrapitops.plan.modules.SystemObjectProvidingModule;
import com.djrapitops.plan.modules.neoforge.NeoForgeServerPropertiesModule; // NEU
import com.djrapitops.plan.modules.neoforge.NeoForgeTaskModule; // NEU
import com.djrapitops.plan.settings.Settings;
import com.djrapitops.plan.settings.config.Config;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.settings.locale.lang.PluginLang;
import com.djrapitops.plan.settings.theme.PlanColorScheme;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.playeranalytics.plugin.PlatformAbstractionLayer;
import net.playeranalytics.plugin.neoforge.NeoForgePlatform; // NEU: PAL-Implementierung
import net.playeranalytics.plugin.scheduling.RunnableFactory;
import net.playeranalytics.plugin.server.PluginLogger;
import org.bstats.neoforge.Metrics; // NEU: bStats für NeoForge
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Ersetzt @Plugin("plan")
@Mod("plan")
public class PlanNeoForge implements PlanPlugin {

    // Ersetzt PluginContainer
    private final ModContainer modContainer;
    private final File dataFolder;
    private final Metrics metrics;

    // Diese Systeme werden jetzt manuell im Konstruktor erstellt
    private final PlanSystem system;
    private final PlanCommand planCommand;
    private final NeoForgeServerShutdownSave serverShutdownSave; // Die portierte Klasse
    private final PlatformAbstractionLayer abstractionLayer;
    private final PluginLogger logger;
    private final RunnableFactory runnableFactory;

    private Locale locale;

    // Ersetzt den @Inject-Konstruktor und onLoad()
    public PlanNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        this.modContainer = modContainer;
        Path dataPath = FMLPaths.CONFIGDIR.get().resolve("plan");
        this.dataFolder = dataPath.toFile();

        // 1. PAL und Logger initialisieren (von onLoad)
        // Sie müssen eine NeoForgePlatform-Klasse als Teil Ihrer PAL-Implementierung erstellen
        this.abstractionLayer = new NeoForgePlatform(this, dataPath, modContainer);
        this.logger = abstractionLayer.getPluginLogger();
        this.runnableFactory = abstractionLayer.getRunnableFactory();

        // 2. bStats initialisieren
        int pluginId = 3086;
        this.metrics = new Metrics(modContainer, pluginId);

        // 3. Manuelle Dependency Injection (ersetzt DaggerPlanSpongeComponent.makeComponent())
        try {
            // Dies ist die Logik aus der vorherigen Antwort (PlanNeoForge als Dagger-Ersatz)
            // Plattform-spezifische Module erstellen
            NeoForgeTaskModule taskModule = new NeoForgeTaskModule(abstractionLayer);
            NeoForgeServerPropertiesModule serverPropertiesModule = new NeoForgeServerPropertiesModule();
            // TODO: Andere Module portieren und hier registrieren (z.B. NeoForge.EVENT_BUS.register(serverPropertiesModule))

            // Gemeinsame Module
            SystemObjectProvidingModule systemModule = new SystemObjectProvidingModule();
            PlatformAbstractionLayerModule palModule = new PlatformAbstractionLayerModule(abstractionLayer);
            // TODO: Andere gemeinsame Module (FiltersModule, PlaceholderModule)

            // Kern-Abhängigkeiten erstellen
            Config config = palModule.provideConfig();
            Settings settings = palModule.provideSettings(config);
            ErrorLogger errorLogger = systemModule.provideErrorLogger(settings, logger);
            
            // TODO: DBSystem mit *allen* seinen Abhängigkeiten erstellen
            // Dies wird der komplexeste Konstruktoraufruf sein
            DBSystem dbSystem = new DBSystem(logger, /* ... viele weitere Abhängigkeiten ... */);

            // PlanSystem erstellen
            // TODO: PlanSystem mit *allen* seinen Abhängigkeiten erstellen
            this.system = new PlanSystem(
                    dbSystem,
                    settings,
                    errorLogger,
                    taskModule.provideScheduler()
                    /* ... viele weitere Abhängigkeiten ... */
            );

            // Befehl-Logik erstellen
            this.planCommand = new PlanCommand(system, abstractionLayer);

            // Shutdown-Listener erstellen
            this.serverShutdownSave = new NeoForgeServerShutdownSave(
                    palModule.provideLocale(settings),
                    dbSystem,
                    logger,
                    errorLogger
            );

            // (von onLoad)
            system.enableForCommands();

        } catch (Exception e) {
            // (von catchStartupErrors)
            handleStartupError(e);
            // Erneutes Werfen, um das Laden des Mods zu stoppen
            throw new RuntimeException("Plan-Initialisierung fehlgeschlagen", e);
        }

        // 4. Event-Listener registrieren
        // Mod-Bus für ladezeit-Events (z.B. Befehle)
        modEventBus.addListener(this::onRegisterCommands);

        // NeoForge-Bus für Spiel-Events (Start, Stopp, Spieler-Events)
        NeoForge.EVENT_BUS.register(this); // Registriert @SubscribeEvent-Methoden in dieser Klasse
        NeoForge.EVENT_BUS.register(serverShutdownSave);
        // TODO: Registrieren Sie hier Ihre portierten Player-Listener
        // NeoForge.EVENT_BUS.register(new NeoForgePlayerListener(...));
    }

    // Ersetzt onServerStart
    @SubscribeEvent
    public void onServerStart(ServerStartingEvent event) {
        // (von onEnable)
        try {
            if (system == null) {
                // Fehler ist bereits im Konstruktor aufgetreten
                return;
            }

            this.locale = system.getLocaleSystem().getLocale();

            system.enableOtherThanCommands();

            // bStats-Metriken registrieren
            // Sie müssen BStatsSponge zu BStatsNeoForge portieren
            new BStatsNeoForge(
                    metrics, system.getDatabaseSystem().getDatabase()
            ).registerMetrics();

            logger.info(locale.getString(PluginLang.ENABLED));
            system.getProcessing().submitNonCritical(() -> system.getListenerSystem().callEnableEvent(this));
        } catch (Exception e) {
            handleStartupError(e);
        }
    }

    // Ersetzt onServerStop
    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        // (von onDisable)
        storeSessionsOnShutdown();
        cancelAllTasks();
        if (system != null) system.disable();

        logger.info(Locale.getStringNullSafe(locale, PluginLang.DISABLED));
    }

    // Ersetzt onRegisterCommand
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // (von onRegisterCommand)
        Subcommand command = this.planCommand.build();

        // Sie müssen eine Wrapper-Klasse 'NeoForgeCommand' erstellen,
        // die Ihren 'Subcommand' an NeoForges Brigadier-System anpasst.
        NeoForgeCommand.register(event.getDispatcher(), command, this);
    }

    // Ersetzt catchStartupErrors
    private void handleStartupError(Exception e) {
        try {
            if (e instanceof AbstractMethodError) {
                logger.error("Plugin ran into AbstractMethodError - Server restart is required. Likely cause is updating the jar without a restart.");
            } else if (e instanceof EnableException) {
                logger.error("----------------------------------------");
                logger.error("Error: " + e.getMessage());
                logger.error("----------------------------------------");
                logger.error("Plugin Failed to Initialize Correctly. If this issue is caused by config settings you can use /plan reload");
            } else {
                String version = abstractionLayer.getPluginInformation().getVersion();
                logger.error("Plugin Failed to Initialize Correctly. If this issue is caused by config settings you can use /plan reload", e);
                logger.error("This error should be reported at https://github.com/plan-player-analytics/Plan/issues");
            }
        } catch (Exception loggingError) {
            // Falls der Logger selbst fehlschlägt
            e.printStackTrace();
            loggingError.printStackTrace();
        }
        
        // Versuchen Sie, einen sauberen Tei-Shutdown durchzuführen
        onDisablePartial();
    }
    
    // Teil von onDisable, der bei einem Fehler aufgerufen werden kann
    private void onDisablePartial() {
        storeSessionsOnShutdown(); // Ist null-sicher
        cancelAllTasks(); // Ist null-sicher
        if (system != null) system.disable();
    }

    // (unverändert von PlanSponge)
    private void storeSessionsOnShutdown() {
        if (serverShutdownSave != null) {
            Optional<Future<?>> complete = serverShutdownSave.performSave();
            if (complete.isPresent()) {
                try {
                    complete.get().get(4, TimeUnit.SECONDS); // wait for completion for 4s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    logger.error("Failed to save sessions to database on shutdown: " + e.getCause().getMessage());
                } catch (TimeoutException e) {
                    logger.info(Locale.getStringNullSafe(locale, PluginLang.DISABLED_UNSAVED_SESSIONS_TIMEOUT));
                }
            }
        }
    }

    // (unverändert von PlanSponge, außer null-Check)
    public void cancelAllTasks() {
        if (runnableFactory != null) {
            runnableFactory.cancelAllKnownTasks();
        }
    }

    // --- Implementierung der PlanPlugin-Schnittstelle ---

    @Override
    public InputStream getResource(String resource) {
        // Der Classloader ist in NeoForge/FML anders
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public ColorScheme getColorScheme() {
        return PlanColorScheme.create(system.getConfigSystem().getConfig(), logger);
    }

    @Override
    public void registerCommand(Subcommand command) {
        // NOOP: Wird über onRegisterCommands gehandhabt
    }

    @Override
    public PlanSystem getSystem() {
        return system;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    // --- NeoForge-spezifische Getter ---

    public ModContainer getModContainer() {
        return modContainer;
    }

    public PlatformAbstractionLayer getAbstractionLayer() {
        return abstractionLayer;
    }
}
