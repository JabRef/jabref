package org.jabref.logic.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionTest {

    @Test
    void unknownVersionAsString() {
        Version version = Version.parse(BuildInfo.UNKNOWN_VERSION);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    void unknownVersionAsNull() {
        Version version = Version.parse(null);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    void unknownVersionAsEmptyString() {
        Version version = Version.parse("");
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    void initVersionFromWrongStringResultsInUnknownVersion() {
        Version version = Version.parse("${version}");
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    void versionOneDigit() {
        String versionText = "1";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    void versionTwoDigits() {
        String versionText = "1.2";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(0, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    void versionThreeDigits() {
        String versionText = "1.2.3";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    void versionOneDigitDevVersion() {
        String versionText = "1dev";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    void versionTwoDigitDevVersion() {
        String versionText = "1.2dev";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(0, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    void versionThreeDigitDevVersion() {
        String versionText = "1.2.3dev";
        Version version = Version.parse(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    void validVersionIsNotNewerThanUnknownVersion() {
        // Reason: unknown version should only happen for developer builds where we don't want an update notification
        Version unknownVersion = Version.parse(BuildInfo.UNKNOWN_VERSION);
        Version validVersion = Version.parse("4.2");
        assertFalse(validVersion.isNewerThan(unknownVersion));
    }

    @Test
    void unknownVersionIsNotNewerThanValidVersion() {
        Version unknownVersion = Version.parse(BuildInfo.UNKNOWN_VERSION);
        Version validVersion = Version.parse("4.2");
        assertFalse(unknownVersion.isNewerThan(validVersion));
    }

    @Test
    void versionNewerThan() {
        Version olderVersion = Version.parse("2.4");
        Version newerVersion = Version.parse("4.2");
        assertTrue(newerVersion.isNewerThan(olderVersion));
    }

    @Test
    void versionNotNewerThan() {
        Version olderVersion = Version.parse("2.4");
        Version newerVersion = Version.parse("4.2");
        assertFalse(olderVersion.isNewerThan(newerVersion));
    }

    @Test
    void versionNotNewerThanSameVersion() {
        Version version1 = Version.parse("4.2");
        Version version2 = Version.parse("4.2");
        assertFalse(version1.isNewerThan(version2));
    }

    @Test
    void versionNewerThanDevTwoDigits() {
        Version older = Version.parse("4.2");
        Version newer = Version.parse("4.3dev");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    void versionNewerThanDevVersion() {
        Version older = Version.parse("1.2dev");
        Version newer = Version.parse("1.2");
        assertTrue(newer.isNewerThan(older));
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    void versionNewerThanDevThreeDigits() {
        Version older = Version.parse("4.2.1");
        Version newer = Version.parse("4.3dev");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    void versionNewerMinor() {
        Version older = Version.parse("4.1");
        Version newer = Version.parse("4.2.1");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    void versionNotNewerMinor() {
        Version older = Version.parse("4.1");
        Version newer = Version.parse("4.2.1");
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    void versionNewerPatch() {
        Version older = Version.parse("4.2.1");
        Version newer = Version.parse("4.2.2");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    void versionNotNewerPatch() {
        Version older = Version.parse("4.2.1");
        Version newer = Version.parse("4.2.2");
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    void versionNewerDevelopmentNumber() {
        Version older = Version.parse("4.2-beta1");
        Version newer = Version.parse("4.2-beta2");
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    void versionNotNewerThanSameVersionWithBeta() {
        Version version1 = Version.parse("4.2-beta2");
        Version version2 = Version.parse("4.2-beta2");
        assertFalse(version2.isNewerThan(version1));
    }

    @Test
    void equalVersionsNotNewer() {
        Version version1 = Version.parse("4.2.2");
        Version version2 = Version.parse("4.2.2");
        assertFalse(version1.isNewerThan(version2));
    }

    @Test
    void changelogOfDevelopmentVersionWithDash() {
        Version version = Version.parse("4.0-dev");
        assertEquals("https://github.com/JabRef/jabref/blob/main/CHANGELOG.md#unreleased", version.getChangelogUrl());
    }

    @Test
    void changelogOfDevelopmentVersionWithoutDash() {
        Version version = Version.parse("3.7dev");
        assertEquals("https://github.com/JabRef/jabref/blob/main/CHANGELOG.md#unreleased", version.getChangelogUrl());
    }

    @Test
    void changelogOfDevelopmentStageSubNumber() {
        Version version1 = Version.parse("4.0");
        Version version2 = Version.parse("4.0-beta");
        Version version3 = Version.parse("4.0-beta2");
        Version version4 = Version.parse("4.0-beta3");
        assertEquals("https://github.com/JabRef/jabref/blob/v4.0/CHANGELOG.md", version1.getChangelogUrl());
        assertEquals("https://github.com/JabRef/jabref/blob/v4.0-beta/CHANGELOG.md", version2.getChangelogUrl());
        assertEquals("https://github.com/JabRef/jabref/blob/v4.0-beta2/CHANGELOG.md", version3.getChangelogUrl());
        assertEquals("https://github.com/JabRef/jabref/blob/v4.0-beta3/CHANGELOG.md", version4.getChangelogUrl());
    }

    @Test
    void changelogWithTwoDigits() {
        Version version = Version.parse("3.4");
        assertEquals("https://github.com/JabRef/jabref/blob/v3.4/CHANGELOG.md", version.getChangelogUrl());
    }

    @Test
    void changelogWithThreeDigits() {
        Version version = Version.parse("3.4.1");
        assertEquals("https://github.com/JabRef/jabref/blob/v3.4.1/CHANGELOG.md", version.getChangelogUrl());
    }

    @Test
    void versionNull() {
        String versionText = null;
        Version version = Version.parse(versionText);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    void versionEmpty() {
        String versionText = "";
        Version version = Version.parse(versionText);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    void betaNewerThanAlpha() {
        Version older = Version.parse("2.7-alpha");
        Version newer = Version.parse("2.7-beta");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    void stableNewerThanBeta() {
        Version older = Version.parse("2.8-alpha");
        Version newer = Version.parse("2.8");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    void alphaShouldBeUpdatedToBeta() {
        Version alpha = Version.parse("2.8-alpha");
        Version beta = Version.parse("2.8-beta");
        assertTrue(alpha.shouldBeUpdatedTo(beta));
    }

    @Test
    void alphaTwoShouldBeUpdatedToAlpha3() {
        Version alpha2 = Version.parse("6.0-alpha.2");
        Version alpha3 = Version.parse("6.0-alpha.3");
        assertTrue(alpha2.shouldBeUpdatedTo(alpha3));
    }

    @Test
    void betaShouldBeUpdatedToStable() {
        Version beta = Version.parse("2.8-beta");
        Version stable = Version.parse("2.8");
        assertTrue(beta.shouldBeUpdatedTo(stable));
    }

    @Test
    void stableShouldNotBeUpdatedToAlpha() {
        Version stable = Version.parse("2.8");
        Version alpha = Version.parse("2.9-alpha");
        assertFalse(stable.shouldBeUpdatedTo(alpha));
    }

    @Test
    void stableShouldNotBeUpdatedToBeta() {
        Version stable = Version.parse("3.8.2");
        Version beta = Version.parse("4.0-beta");
        assertFalse(stable.shouldBeUpdatedTo(beta));
    }

    @Test
    void alphaShouldBeUpdatedToStables() {
        Version alpha = Version.parse("2.8-alpha");
        Version stable = Version.parse("2.8");
        List<Version> availableVersions = Arrays.asList(Version.parse("2.8-beta"), stable);
        assertEquals(Optional.of(stable), alpha.shouldBeUpdatedTo(availableVersions));
    }

    @Test
    void ciSuffixShouldBeRemoved() {
        Version v50ci = Version.parse("5.0-ci.1");
        assertEquals("5.0", v50ci.getFullVersion());
    }

    @Test
    void ciSuffixShouldBeRemovedIfDateIsPresent() {
        Version v50ci = Version.parse("5.0-ci.1--2020-03-06--289142f");
        assertEquals("5.0--2020-03-06--289142f", v50ci.getFullVersion());
    }

    @Test
    @FetcherTest
    @DisabledOnCIServer("GitHub puts a low rate limit on unauthenticated calls")
    void getAllAvailableVersionsReturnsSomething() throws IOException {
        assertNotEquals(List.of(), Version.getAllAvailableVersions());
    }
}
