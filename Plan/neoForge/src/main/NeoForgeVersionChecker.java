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
package com.djrapitops.plan.version;

import com.djrapitops.plan.settings.config.PlanConfig;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
// ANGENOMMENER IMPORT: Sie benötigen einen Loader für Modrinth oder CurseForge
import com.djrapitops.plan.version.modrinth.ModrinthVersionInfoLoader;
import net.playeranalytics.plugin.scheduling.RunnableFactory;
import net.playeranalytics.plugin.server.PluginLogger;

// import javax.inject.Inject; // Entfernt
// import javax.inject.Named; // Entfernt
// import javax.inject.Singleton; // Entfernt
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * System for checking if new Version is available when the System initializes.
 * (Checks Modrinth/CurseForge)
 */
// @Singleton // Entfernt - Instanz wird manuell in PlanNeoForge erstellt
public class NeoForgeVersionChecker extends VersionChecker {
    
    // @Inject // Entfernt
    public NeoForgeVersionChecker(
            // @Named("currentVersion") // Entfernt - Sie übergeben die Version direkt
            String currentVersion,
            Locale locale,
            PlanConfig config,
            PluginLogger logger,
            RunnableFactory runnableFactory,
            ErrorLogger errorLogger
    ) {
        super(currentVersion, locale, config, logger, runnableFactory, errorLogger);
    }

    @Override
    protected Optional<List<VersionInfo>> loadVersionInfo() {
        try {
            // Dies MUSS geändert werden. NeoForge-Mods sind auf Modrinth/CurseForge.
            // Ersetzt 'OreVersionInfoLoader'
            return Optional.of(ModrinthVersionInfoLoader.load());
            // Oder: return Optional.of(CurseForgeVersionInfoLoader.load());
        } catch (IOException e) {
            // Fehlermeldung aktualisiert
            logger.warn("Failed to check updates from Modrinth/CurseForge (" + e.getMessage() + "), allow connection or disable update check from Plan config");
            return Optional.empty();
        }
    }
}
