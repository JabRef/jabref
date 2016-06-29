/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

/**
 * Represents the Application Version with the major and minor number, the full Version String and if it's a developer version
 */
public class Version {

    public final static String JABREF_DOWNLOAD_URL = "http://www.fosshub.com/JabRef.html";
    private static final Log LOGGER = LogFactory.getLog(Version.class);
    private final static String JABREF_GITHUB_URL = "https://api.github.com/repos/JabRef/jabref/releases/latest";

    private String fullVersion = BuildInfo.UNKNOWN_VERSION;
    private int major = 0;
    private int minor = 0;
    private int patch = 0;
    private boolean isDevelopmentVersion = false;

    /**
     * @param version must be in form of X.X (e.g., 3.3; 3.4dev)
     */
    public Version(String version) {
        if ((version == null) || "".equals(version) || version.equals(BuildInfo.UNKNOWN_VERSION)) {
            return;
        }

        String[] versionParts = version.split("dev");
        String[] versionNumbers = versionParts[0].split(Pattern.quote("."));
        try {
            this.major = Integer.parseInt(versionNumbers[0]);
            this.minor = versionNumbers.length >= 2 ? Integer.parseInt(versionNumbers[1]) : 0;
            this.patch = versionNumbers.length >= 3 ? Integer.parseInt(versionNumbers[2]) : 0;
            this.fullVersion = version;
            this.isDevelopmentVersion = version.contains("dev");
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid version string used: " + version, exception);
        }
    }

    /**
     * Grabs the latest release version from the JabRef GitHub repository
     */
    public static Version getLatestVersion() throws IOException {
        URLConnection connection = new URL(JABREF_GITHUB_URL).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        JSONObject obj = new JSONObject(rd.readLine());
        return new Version(obj.getString("tag_name").replaceFirst("v", ""));
    }

    /**
     * @return true iff this version is newer than the passed one
     */
    public boolean isNewerThan(Version otherVersion) {
        Objects.requireNonNull(otherVersion);
        if (Objects.equals(this, otherVersion)) {
            return false;
        } else if (this.getFullVersion().equals(BuildInfo.UNKNOWN_VERSION)) {
            return false;
        } else if (otherVersion.getFullVersion().equals(BuildInfo.UNKNOWN_VERSION)) {
            return false;
        } else if (this.getMajor() > otherVersion.getMajor()) {
            return true;
        } else if (this.getMajor() == otherVersion.getMajor()) {
            if (this.getMinor() > otherVersion.getMinor()) {
                return true;
            } else if ((this.getMinor() == otherVersion.getMinor()) && (this.getPatch() > otherVersion.getPatch())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isDevelopmentVersion() {
        return isDevelopmentVersion;
    }

    /**
     * @return The link to the changelog on github to this specific version
     * (https://github.com/JabRef/jabref/blob/vX.X/CHANGELOG.md)
     */
    public String getChangelogUrl() {
        String version = this.getMajor() + "." + this.getMinor() + (this.getPatch() != 0 ? "." + this.getPatch() : "");
        return "https://github.com/JabRef/jabref/blob/v" + version + "/CHANGELOG.md";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Version)) {
            return false;
        }

        Version otherVersion = (Version) other;
        // till all the information are stripped from the fullverison this should suffice
        return this.getFullVersion().equals(otherVersion.getFullVersion());
    }

    @Override
    public int hashCode() {
        return getFullVersion().hashCode();
    }

    @Override
    public String toString() {
        return this.getFullVersion();
    }

}
