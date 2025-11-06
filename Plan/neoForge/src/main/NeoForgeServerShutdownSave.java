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

import com.djrapitops.plan.gathering.ServerShutdownSave;
import com.djrapitops.plan.gathering.afk.AFKTracker;
// Import des NeoForge-spezifischen Listeners (angenommener Pfad)
import com.djrapitops.plan.gathering.listeners.neoforge.NeoForgeAFKListener;
import com.djrapitops.plan.settings.locale.Locale;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.utilities.logging.ErrorLogger;
import net.playeranalytics.plugin.server.PluginLogger;
// NeoForge Event-Importe
import net.neoforged.neoforge.eventbus.api.EventPriority;
import net.neoforged.neoforge.eventbus.api.SubscribeEvent;
import net.neoforged.neoforge.server.event.ServerStoppingEvent;

import java.util.Optional;

/**
 * ServerShutdownSave implementation for NeoForge
 *
 * @author AuroraLS3
 */
// @Singleton und @Inject entfernt, da NeoForge kein DI-Framework wie Sponge (Guice) verwendet.
// Diese Klasse muss manuell instanziiert und im Event-Bus registriert werden.
public class NeoForgeServerShutdownSave extends ServerShutdownSave {

    private boolean shuttingDown = false;

    public NeoForgeServerShutdownSave(
            Locale locale,
            DBSystem dbSystem,
            PluginLogger logger,
            ErrorLogger errorLogger
    ) {
        super(locale, dbSystem, logger, errorLogger);
    }

    @Override
    protected boolean checkServerShuttingDownStatus() {
        return shuttingDown;
    }

    // Ersetzt @Listener(order = Order.PRE)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerShutdown(ServerStoppingEvent event) {
        shuttingDown = true;
    }

    @Override
    public Optional<AFKTracker> getAfkTracker() {
        // Muss auf die NeoForge-Implementierung des AFKListeners verweisen
        return Optional.ofNullable(NeoForgeAFKListener.getAfkTracker());
    }
}
