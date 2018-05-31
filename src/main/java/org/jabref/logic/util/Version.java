package org.jabref.logic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the Application Version with the major and minor number, the full Version String and if it's a developer
 * version
 */
public class Version {

    public static final String JABREF_DOWNLOAD_URL = "https://downloads.jabref.org";
    private static final Logger LOGGER = LoggerFactory.getLogger(Version.class);

    private static final Version UNKNOWN_VERSION = new Version();

    private final static Pattern VERSION_PATTERN = Pattern.compile("(?<major>\\d+)(\\.(?<minor>\\d+))?(\\.(?<patch>\\d+))?(?<stage>-alpha|-beta)?(?<dev>-?dev)?.*");
    private static final String JABREF_GITHUB_RELEASES = "https://api.github.com/repos/JabRef/JabRef/releases";


    private String fullVersion = BuildInfo.UNKNOWN_VERSION;
    private int major = -1;
    private int minor = -1;
    private int patch = -1;
    private DevelopmentStage developmentStage = DevelopmentStage.UNKNOWN;
    private boolean isDevelopmentVersion;

    /**
     * Dummy constructor to create a local object (and  {@link Version#UNKNOWN_VERSION})
     */
    private Version() {
    }

    /**
     * @param version must be in form of following pattern: {@code (\d+)(\.(\d+))?(\.(\d+))?(-alpha|-beta)?(-?dev)?}
     *                (e.g., 3.3; 3.4-dev)
     * @return the parsed version or {@link Version#UNKNOWN_VERSION} if an error occurred
     */
    public static Version parse(String version) {
        if ((version == null) || "".equals(version) || version.equals(BuildInfo.UNKNOWN_VERSION)
                || "${version}".equals(version)) {
            return UNKNOWN_VERSION;
        }

        Version parsedVersion = new Version();

        parsedVersion.fullVersion = version;
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            try {
                parsedVersion.major = Integer.parseInt(matcher.group("major"));

                String minorString = matcher.group("minor");
                parsedVersion.minor = minorString == null ? 0 : Integer.parseInt(minorString);

                String patchString = matcher.group("patch");
                parsedVersion.patch = patchString == null ? 0 : Integer.parseInt(patchString);

                String versionStageString = matcher.group("stage");
                parsedVersion.developmentStage = versionStageString == null ? DevelopmentStage.STABLE : DevelopmentStage.parse(versionStageString);
                parsedVersion.isDevelopmentVersion = matcher.group("dev") != null;
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid version string used: " + version, e);
                return UNKNOWN_VERSION;
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid version pattern is used", e);
                return UNKNOWN_VERSION;
            }
        } else {
            LOGGER.warn("Version could not be recognized by the pattern");
            return UNKNOWN_VERSION;
        }
        return parsedVersion;
    }

    /**
     * Grabs all the available releases from the GitHub repository
     */
    public static List<Version> getAllAvailableVersions() throws IOException {
        URLConnection connection = new URL(JABREF_GITHUB_RELEASES).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

            List<Version> versions = new ArrayList<>();
            JSONArray objects = new JSONArray(rd.readLine());
            for (int i = 0; i < objects.length(); i++) {
                JSONObject jsonObject = objects.getJSONObject(i);
                Version version = Version.parse(jsonObject.getString("tag_name").replaceFirst("v", ""));
                versions.add(version);
            }
            return versions;
        }
    }

    /**
     * @return true if this version is newer than the passed one
     */
    public boolean isNewerThan(Version otherVersion) {
        Objects.requireNonNull(otherVersion);
        if (Objects.equals(this, otherVersion)) {
            return false;
        } else if (this.getFullVersion().equals(BuildInfo.UNKNOWN_VERSION)) {
            return false;
        } else if (otherVersion.getFullVersion().equals(BuildInfo.UNKNOWN_VERSION)) {
            return false;
        }

        // compare the majors
        if (this.getMajor() > otherVersion.getMajor()) {
            return true;
        } else if (this.getMajor() == otherVersion.getMajor()) {
            // if the majors are equal compare the minors
            if (this.getMinor() > otherVersion.getMinor()) {
                return true;
            } else if (this.getMinor() == otherVersion.getMinor()) {
                // if the minors are equal compare the patch numbers
                if (this.getPatch() > otherVersion.getPatch()) {
                    return true;
                } else if (this.getPatch() == otherVersion.getPatch()) {
                    // if the patch numbers are equal compare the development stages
                    if (this.developmentStage.isMoreStableThan(otherVersion.developmentStage)) {
                        return true;
                    } else if (this.developmentStage == otherVersion.developmentStage) {
                        // if the stage is equal check if this version is in development and the other is not
                        return !this.isDevelopmentVersion && otherVersion.isDevelopmentVersion;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Checks if this version should be updated to one of the given ones.
     * Ignoring the other Version if this one is Stable and the other one is not.
     *
     * @return The version this one should be updated to, or an empty Optional
     */
    public Optional<Version> shouldBeUpdatedTo(List<Version> availableVersions) {
        Optional<Version> newerVersion = Optional.empty();
        for (Version version : availableVersions) {
            if (this.shouldBeUpdatedTo(version)
                    && (!newerVersion.isPresent() || version.isNewerThan(newerVersion.get()))) {
                newerVersion = Optional.of(version);
            }
        }
        return newerVersion;
    }

    /**
     * Checks if this version should be updated to the given one.
     * Ignoring the other Version if this one is Stable and the other one is not.
     *
     * @return True if this version should be updated to the given one
     */
    public boolean shouldBeUpdatedTo(Version otherVersion) {
        // ignoring the other version if it is not stable, except if this version itself is not stable
        if (developmentStage == Version.DevelopmentStage.STABLE
                && otherVersion.developmentStage != Version.DevelopmentStage.STABLE) {
            return false;
        }

        // check if the other version is newer than given one
        return otherVersion.isNewerThan(this);
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
     * @return The link to the changelog on GitHub to this specific version (https://github.com/JabRef/jabref/blob/vX.X/CHANGELOG.md)
     */
    public String getChangelogUrl() {
        if (isDevelopmentVersion) {
            return "https://github.com/JabRef/jabref/blob/master/CHANGELOG.md#unreleased";
        } else {
            StringBuilder changelogLink = new StringBuilder()
                    .append("https://github.com/JabRef/jabref/blob/v")
                    .append(this.getMajor())
                    .append(".")
                    .append(this.getMinor());

            if (this.getPatch() != 0) {
                changelogLink
                        .append(".")
                        .append(this.getPatch());
            }

            changelogLink
                    .append(this.developmentStage.stage)
                    .append("/CHANGELOG.md");

            return changelogLink.toString();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Version)) {
            return false;
        }

        // till all the information are stripped from the fullversion this should suffice
        return this.getFullVersion().equals(((Version) other).getFullVersion());
    }

    @Override
    public int hashCode() {
        return getFullVersion().hashCode();
    }

    @Override
    public String toString() {
        return this.getFullVersion();
    }

    public enum DevelopmentStage {
        UNKNOWN("", 0),
        ALPHA("-alpha", 1),
        BETA("-beta", 2),
        STABLE("", 3);

        /**
         * describes how stable this stage is, the higher the better
         */
        private final int stability;
        private final String stage;

        DevelopmentStage(String stage, int stability) {
            this.stage = stage;
            this.stability = stability;
        }

        public static DevelopmentStage parse(String stage) {
            if (stage == null) {
                LOGGER.warn("The stage cannot be null");
                return UNKNOWN;
            } else if (stage.equals(STABLE.stage)) {
                return STABLE;
            } else if (stage.equals(ALPHA.stage)) {
                return ALPHA;
            } else if (stage.equals(BETA.stage)) {
                return BETA;
            }
            LOGGER.warn("Unknown development stage: " + stage);
            return UNKNOWN;
        }

        /**
         * @return true if this stage is more stable than the {@code otherStage}
         */
        public boolean isMoreStableThan(DevelopmentStage otherStage) {
            return this.stability > otherStage.stability;
        }
    }
}
