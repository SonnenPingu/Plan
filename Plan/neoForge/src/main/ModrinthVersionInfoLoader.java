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
package com.djrapitops.plan.version.modrinth; // Paket geändert

import com.djrapitops.plan.version.VersionInfo;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for loading version information from Modrinth, a mod repository.
 */
public class ModrinthVersionInfoLoader {
    // Annahme: Der Projekt-Slug auf Modrinth ist 'plan'
    private static final String MODRINTH_VERSIONS_URL = "https://api.modrinth.com/v2/project/plan/version";
    // Modrinth-Seiten-URL für den Changelog
    private static final String MODRINTH_CHANGELOG_URL = "https://modrinth.com/mod/plan/version/%s";

    private ModrinthVersionInfoLoader() {
        /* Static method class */
    }

    /**
     * Loads version information from Modrinth, using its Web API.
     *
     * @return List of VersionInfo, newest version first.
     * @throws IOException If API can not be accessed.
     */
    public static List<VersionInfo> load() throws IOException {
        List<VersionInfo> versionInfo = new ArrayList<>();

        // Keine Session für Modrinth v2 API (Lesen) erforderlich

        List<ModrinthVersionDto> versions = loadModrinthVersions();
        versions.forEach(i -> {
            // Modrinth verwendet 'version_type' ("release", "beta", "alpha")
            boolean isRelease = "release".equalsIgnoreCase(i.version_type);
            String versionString = i.version_number;

            // Finde die primäre Datei für den Download-Link
            String download = i.files.stream()
                    .filter(f -> f.primary)
                    .map(f -> f.url)
                    .findFirst()
                    .orElse(i.files.isEmpty() ? null : i.files.get(0).url); // Fallback auf die erste Datei

            String changeLog = String.format(MODRINTH_CHANGELOG_URL, i.version_number);

            if (download != null) {
                versionInfo.add(new VersionInfo(isRelease, versionString, download, changeLog));
            }
        });

        Collections.sort(versionInfo);
        return versionInfo;
    }

    private static List<ModrinthVersionDto> loadModrinthVersions() throws IOException {
        URL url = new URL(MODRINTH_VERSIONS_URL);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            // Keine Autorisierung erforderlich
            connection.setRequestProperty("User-Agent", "Player Analytics Update Checker");
            connection.connect();
            try (InputStream in = connection.getInputStream()) {
                // Modrinth gibt direkt ein Array zurück
                JsonArray versions = JsonParser.parseString(readInputFully(in)).getAsJsonArray();

                return new Gson().getAdapter(new TypeToken<List<ModrinthVersionDto>>() {}).fromJsonTree(versions);
            }
        } finally {
            connection.disconnect();
        }
    }

    // newOreSession() wird entfernt, da sie nicht benötigt wird

    private static String readInputFully(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    // --- Private DTOs (Data Transfer Objects) zur Abbildung der Modrinth-JSON-Antwort ---

    private static class ModrinthVersionDto {
        String name;
        String version_number;
        String version_type; // "release", "beta", "alpha"
        List<ModrinthFileDto> files;
    }

    private static class ModrinthFileDto {
        String url;
        boolean primary;
    }
}
