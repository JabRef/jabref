package net.sf.jabref.logic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents the Application Version with the major and minor number, the full Version String and if it's a developer version
 */
public class Version {

    private static final Log LOGGER = LogFactory.getLog(Version.class);

    private final static Pattern VERSION_PATTERN = Pattern.compile("(?<major>\\d+)(\\.(?<minor>\\d+))?(\\.(?<patch>\\d+))?(?<stage>-alpha|-beta)?(?<dev>-?dev)?.*");

    public static final String JABREF_DOWNLOAD_URL = "https://downloads.jabref.org";
    private static final String JABREF_GITHUB_RELEASES = "https://api.github.com/repos/JabRef/JabRef/releases";


    private String fullVersion = BuildInfo.UNKNOWN_VERSION;
    private int major = -1;
    private int minor = -1;
    private int patch = -1;
    private DevelopmentStage developmentStage = DevelopmentStage.UNKNOWN;
    private boolean isDevelopmentVersion;

    /**
     * @param version must be in form of following pattern: {@code (\d+)(\.(\d+))?(\.(\d+))?(-alpha|-beta)?(-?dev)?} (e.g., 3.3; 3.4-dev)
     */
    public Version(String version) {
        if ((version == null) || "".equals(version) || version.equals(BuildInfo.UNKNOWN_VERSION)
                || "${version}".equals(version)) {
            return;
        }

        this.fullVersion = version;
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            try {
                this.major = Integer.parseInt(matcher.group("major"));

                String minorString = matcher.group("minor");
                this.minor = minorString == null ? 0 : Integer.parseInt(minorString);

                String patchString = matcher.group("patch");
                this.patch = patchString == null ? 0 : Integer.parseInt(patchString);

                String versionStageString = matcher.group("stage");
                this.developmentStage = DevelopmentStage.getStage(versionStageString == null ? DevelopmentStage.STABLE.stage : versionStageString);
                this.isDevelopmentVersion = matcher.group("dev") != null;
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid version string used: " + version, e);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid version pattern is used", e);
            }
        } else {
            LOGGER.warn("Version could not be recognized by the pattern");
        }
    }

    /**
     * Grabs all the available releases from the GitHub repository
     *
     * @throws IOException
     */
    public static List<Version> getAllAvailableVersions() throws IOException {
        URLConnection connection = new URL(JABREF_GITHUB_RELEASES).openConnection();
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        List<Version> versions = new ArrayList<>();
        JSONArray objects = new JSONArray(rd.readLine());
        for (int i = 0; i < objects.length(); i++) {
            JSONObject jsonObject = objects.getJSONObject(i);
            Version version = new Version(jsonObject.getString("tag_name").replaceFirst("v", ""));
            versions.add(version);
        }
        return versions;
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

        if (this.getMajor() > otherVersion.getMajor()) {
            return true;
        } else if (this.getMajor() == otherVersion.getMajor()) {
            if (this.getMinor() > otherVersion.getMinor()) {
                return true;
            } else if (this.getMinor() == otherVersion.getMinor()) {
                if (this.getPatch() > otherVersion.getPatch()) {
                    return true;
                } else if (this.getPatch() == otherVersion.getPatch()) {
                    if (this.developmentStage.ordinal() > otherVersion.developmentStage.ordinal()) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public DevelopmentStage getDevelopmentStage() {
        return developmentStage;
    }

    /**
     * @return The link to the changelog on GitHub to this specific version
     * (https://github.com/JabRef/jabref/blob/vX.X/CHANGELOG.md)
     */
    public String getChangelogUrl() {
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
        // needs to be ordered from the unstablest stage to the stablest one
        UNKNOWN(""),
        ALPHA("-alpha"),
        BETA("-beta"),
        STABLE("");


        private final String stage;

        DevelopmentStage(String stage) {
            this.stage = stage;
        }

        public static DevelopmentStage getStage(String stage) {
            if (stage == null) {
                LOGGER.warn("Unknown development stage");
                return UNKNOWN;
            } else if (stage.equals(STABLE.stage)) {
                return STABLE;
            } else if (stage.equals(ALPHA.stage)) {
                return ALPHA;
            } else if (stage.equals(BETA.stage)) {
                return BETA;
            }
            LOGGER.warn("Unknown development stage");
            return UNKNOWN;
        }
    }

}
