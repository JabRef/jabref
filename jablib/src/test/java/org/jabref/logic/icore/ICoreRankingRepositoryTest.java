package org.jabref.logic.icore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ICoreRankingRepositoryTest {
    @Test
    public void testExactMatchSERA() {
        ICoreRankingRepository repo = new ICoreRankingRepository();
        assertEquals("C", repo.getRankingFor("SERA").orElse("Not Found"));
    }

    @Test
    public void testExactMatchICSE() {
        ICoreRankingRepository repo = new ICoreRankingRepository();
        assertEquals("A*", repo.getRankingFor("ICSE").orElse("Not Found"));
    }

    @Test
    public void testFuzzyMatchName() {
        ICoreRankingRepository repo = new ICoreRankingRepository();
        String name = "ACIS Conference on Software Engineering Research";
        assertTrue(repo.getRankingFor(name).isPresent());
    }

    @Test
    public void testMissingValue() {
        ICoreRankingRepository repo = new ICoreRankingRepository();
        assertTrue(repo.getRankingFor("NON_EXISTENT").isEmpty());
    }
    @Test
    public void test1(){
        ICoreRankingRepository repo = new ICoreRankingRepository();
        String name="International Conference on Software Engineering";
        assertTrue(repo.getRankingFor(name).isPresent());
    }
    @Test
    public void printAllAcronyms() {
        ICoreRankingRepository repo = new ICoreRankingRepository();
        repo.acronymToRank.forEach((key, value) -> System.out.println(key + " => " + value));
    }

}
