package org.jabref.logic.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DevelopmentStageTest {

    @Test
    public void checkStabilityOrder() {
        assertTrue(Version.DevelopmentStage.ALPHA.isMoreStableThan(Version.DevelopmentStage.UNKNOWN));
        assertTrue(Version.DevelopmentStage.BETA.isMoreStableThan(Version.DevelopmentStage.ALPHA));
        assertTrue(Version.DevelopmentStage.STABLE.isMoreStableThan(Version.DevelopmentStage.BETA));

        assertEquals("It seems that the development stages have been changed, please adjust the test",
                Version.DevelopmentStage.values().length, 4);
    }

    @Test
    public void parseStages() {
        assertEquals(Version.DevelopmentStage.parse("-alpha"), Version.DevelopmentStage.ALPHA);
        assertEquals(Version.DevelopmentStage.parse("-beta"), Version.DevelopmentStage.BETA);
        assertEquals(Version.DevelopmentStage.parse(""), Version.DevelopmentStage.STABLE);
    }

    @Test
    public void parseNull() {
        assertEquals(Version.DevelopmentStage.parse(null), Version.DevelopmentStage.UNKNOWN);
    }

    @Test
    public void parseUnknownString() {
        assertEquals(Version.DevelopmentStage.parse("asdf"), Version.DevelopmentStage.UNKNOWN);
    }

}
