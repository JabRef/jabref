package net.sf.jabref.logic.util.version;


import net.sf.jabref.logic.util.BuildInfo;
import net.sf.jabref.logic.util.Version;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionTest {

    @Test
    public void unknownVersionAsString() {
        Version version = new Version(BuildInfo.UNKNOWN_VERSION);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void unknownVersionAsNull() {
        Version version = new Version(null);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void unknownVersionAsEmptyString() {
        Version version = new Version("");
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void initVersionFromWrongStringResultsInUnknownVersion() {
        Version version = new Version("${version}");
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void versionOneDigit() {
        String versionText = "1";
        Version version = new Version(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    public void versionTwoDigits() {
        String versionText = "1.2";
        Version version = new Version(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(0, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    public void versionThreeDigits() {
        String versionText = "1.2.3";
        Version version = new Version(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertFalse(version.isDevelopmentVersion());
    }

    @Test
    public void versionOneDigitDevVersion() {
        String versionText = "1dev";
        Version version = new Version(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    public void versionTwoDigitDevVersion() {
        String versionText = "1.2dev";
        Version version = new Version(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(0, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    public void versionThreeDigitDevVersion() {
        String versionText = "1.2.3dev";
        Version version = new Version(versionText);
        assertEquals(versionText, version.getFullVersion());
        assertEquals(1, version.getMajor());
        assertEquals(2, version.getMinor());
        assertEquals(3, version.getPatch());
        assertTrue(version.isDevelopmentVersion());
    }

    @Test
    public void validVersionIsNotNewerThanUnknownVersion() {
        // Reason: unknown version should only happen for developer builds where we don't want an update notification
        Version unknownVersion = new Version(BuildInfo.UNKNOWN_VERSION);
        Version validVersion = new Version("4.2");
        assertFalse(validVersion.isNewerThan(unknownVersion));
    }

    @Test
    public void unknownVersionIsNotNewerThanvalidVersion() {
        Version unknownVersion = new Version(BuildInfo.UNKNOWN_VERSION);
        Version validVersion = new Version("4.2");
        assertFalse(unknownVersion.isNewerThan(validVersion));
    }

    @Test
    public void versionNewerThan() {
        Version olderVersion = new Version("2.4");
        Version newerVersion = new Version("4.2");
        assertTrue(newerVersion.isNewerThan(olderVersion));
    }

    @Test
    public void versionNotNewerThanSameVersion() {
        Version version1 = new Version("4.2");
        Version version2 = new Version("4.2");
        assertFalse(version1.isNewerThan(version2));
    }

    @Test
    public void versionNewerThanDevTwoDigits() {
        Version older = new Version("4.2");
        Version newer = new Version("4.3dev");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNewerThanDevThreeDigits() {
        Version older = new Version("4.2.1");
        Version newer = new Version("4.3dev");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNewerPatch() {
        Version older = new Version("4.2.1");
        Version newer = new Version("4.2.2");
        assertTrue(newer.isNewerThan(older));
    }

    @Test
    public void versionNotNewerPatch() {
        Version older = new Version("4.2.1");
        Version newer = new Version("4.2.2");
        assertFalse(older.isNewerThan(newer));
    }

    @Test
    public void equalVersionsNotNewer() {
        Version version1 = new Version("4.2.2");
        Version version2 = new Version("4.2.2");
        assertFalse(version1.isNewerThan(version2));
    }

    @Test
    public void changelogWithTwoDigits(){
        Version version = new Version("3.4");
        assertEquals("https://github.com/JabRef/jabref/blob/v3.4/CHANGELOG.md", version.getChangelogUrl());
    }

    @Test
    public void changelogWithThreeDigits(){
        Version version = new Version("3.4.1");
        assertEquals("https://github.com/JabRef/jabref/blob/v3.4.1/CHANGELOG.md", version.getChangelogUrl());
    }

    @Test
    public void versionNull() {
        String versionText = null;
        Version version = new Version(versionText);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

    @Test
    public void versionEmpty() {
        String versionText = "";
        Version version = new Version(versionText);
        assertEquals(BuildInfo.UNKNOWN_VERSION, version.getFullVersion());
    }

}
