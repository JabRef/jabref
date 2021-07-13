package org.jabref.logic.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionTest {

    @Test
    public void unknownVersionAsString() {
        Version version = Version.parse(BuildInfo.UNKNOWN_VERSION);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void unknownVersionAsNull() {
        Version version = Version.parse(null);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void unknownVersionAsEmptyString() {
        Version version = Version.parse("");
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void initVersionFromWrongStringResultsInUnknownVersion() {
        Version version = Version.parse("${version}");
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void versionOneDigit() {
        String versionText = "1";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    public void versionTwoDigits() {
        String versionText = "1.2";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(0, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    public void versionThreeDigits() {
        String versionText = "1.2.3";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    public void versionOneDigitDevVersion() {
        String versionText = "1dev";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    public void versionTwoDigitDevVersion() {
        String versionText = "1.2dev";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(0, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    public void versionThreeDigitDevVersion() {
        String versionText = "1.2.3dev";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    public void validVersionIsNotNewerThanUnknownVersion() {
        // Reason: unknown version should only happen for developer builds where we don't want an update notification
        Version unknownVersion = Version.parse(BuildInfo.UNKNOWN_VERSION);
        Version validVersion = Version.parse("4.2");
        assertFalse(validVersion.isNewerThan(unknownVersion));
    }

    @Test
    public void unknownVersionIsNotNewerThanValidVersion() {
        Version unknownVersion = Version.parse(BuildInfo.UNKNOWN_VERSION);
        Version validVersion = Version.parse("4.2");
        assertFalse(unknownVersion.isNewerThan(validVersion));
    }

    @Test
    public void versionNewerThan() {
        Version olderVersion = Version.parse("2.4");
        Version newerVersion = Version.parse("4.2");
        assertTrue(newerVersion.isNewerThan(olderVersion));
    }

    @Test
    public void versionNotNewerThan() {
        Version olderVersion = Version.parse("2.4");
        Version newerVersion = Version.parse("4.2");
        assertFalse(olderVersion.isNewerThan(newerVersion));
    }

    @Test
    public void versionNotNewerThanSameVersion() {
        Version version1 = Version.parse("4.2");
        Version version2 = Version.parse("4.2");
        assertFalse(version1.isNewerThan(version2));
    }

    @Test
    public void versionNewerThanDevTwoDigits() {
        Version older = Version.parse("4.2");
        Version newer = Version.parse("4.3dev");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNewerThanDevVersion() {
        Version older = Version.parse("1.2dev");
        Version newer = Version.parse("1.2");
        assertTrue(newer.isNewerThan(older));
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    public void versionNewerThanDevThreeDigits() {
        Version older = Version.parse("4.2.1");
        Version newer = Version.parse("4.3dev");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNewerMinor() {
        Version older = Version.parse("4.1");
        Version newer = Version.parse("4.2.1");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNotNewerMinor() {
        Version older = Version.parse("4.1");
        Version newer = Version.parse("4.2.1");
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    public void versionNewerPatch() {
        Version older = Version.parse("4.2.1");
        Version newer = Version.parse("4.2.2");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNotNewerPatch() {
        Version older = Version.parse("4.2.1");
        Version newer = Version.parse("4.2.2");
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    public void equalVersionsNotNewer() {
        Version version1 = Version.parse("4.2.2");
        Version version2 = Version.parse("4.2.2");
        assertFalse(version1.isNewerThan(version2));
    }

    @Test
    public void changelogOfDevelopmentVersionWithDash() {
        Version version = Version.parse("4.0-dev");
        assertEquals("https://github.com/JabRef/jabref/blob/master/CHANGELOG.md#unreleased", version.getChangelogUrl());
    }

    @Test
    public void changelogOfDevelopmentVersionWithoutDash() {
        Version version = Version.parse("3.7dev");
        assertEquals("https://github.com/JabRef/jabref/blob/master/CHANGELOG.md#unreleased", version.getChangelogUrl());
    }

    @Test
    public void changelogWithTwoDigits() {
        Version version = Version.parse("3.4");
        assertEquals("https://github.com/JabRef/jabref/blob/v3.4/CHANGELOG.md", version.getChangelogUrl());
    }

    @Test
    public void changelogWithThreeDigits() {
        Version version = Version.parse("3.4.1");
        assertEquals("https://github.com/JabRef/jabref/blob/v3.4.1/CHANGELOG.md", version.getChangelogUrl());
    }

    @Test
    public void versionNull() {
        String versionText = null;
        Version version = Version.parse(versionText);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void versionEmpty() {
        String versionText = "";
        Version version = Version.parse(versionText);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void betaNewerThanAlpha() {
        Version older = Version.parse("2.7-alpha");
        Version newer = Version.parse("2.7-beta");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void stableNewerThanBeta() {
        Version older = Version.parse("2.8-alpha");
        Version newer = Version.parse("2.8");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void alphaShouldBeUpdatedToBeta() {
        Version alpha = Version.parse("2.8-alpha");
        Version beta = Version.parse("2.8-beta");
        assertTrue(alpha.shouldBeUpdatedTo(beta));
    }

    @Test
    public void betaShouldBeUpdatedToStable() {
        Version beta = Version.parse("2.8-beta");
        Version stable = Version.parse("2.8");
        assertTrue(beta.shouldBeUpdatedTo(stable));
    }

    @Test
    public void stableShouldNotBeUpdatedToAlpha() {
        Version stable = Version.parse("2.8");
        Version alpha = Version.parse("2.9-alpha");
        assertFalse(stable.shouldBeUpdatedTo(alpha));
    }

    @Test
    public void stableShouldNotBeUpdatedToBeta() {
        Version stable = Version.parse("3.8.2");
        Version beta = Version.parse("4.0-beta");
        assertFalse(stable.shouldBeUpdatedTo(beta));
    }

    @Test
    public void alphaShouldBeUpdatedToStables() {
        Version alpha = Version.parse("2.8-alpha");
        Version stable = Version.parse("2.8");
        List<Version> availableVersions = Arrays.asList(Version.parse("2.8-beta"), stable);
        assertEquals(Optional.of(stable), alpha.shouldBeUpdatedTo(availableVersions));
    }

    @Test
    public void ciSuffixShouldBeRemoved() {
        Version v50ci = Version.parse("5.0-ci.1");
        assertEquals("5.0", v50ci.getFullVersion());
    }

    @Test
    public void ciSuffixShouldBeRemovedIfDateIsPresent() {
        Version v50ci = Version.parse("5.0-ci.1--2020-03-06--289142f");
        assertEquals("5.0--2020-03-06--289142f", v50ci.getFullVersion());
    }

    @Test
    @FetcherTest
    @DisabledOnCIServer("GitHub puts a low rate limit on unauthenticated calls")
    public void getAllAvailableVersionsReturnsSomething() throws Exception {
        assertNotEquals(Collections.emptyList(), Version.getAllAvailableVersions());
    }
}
