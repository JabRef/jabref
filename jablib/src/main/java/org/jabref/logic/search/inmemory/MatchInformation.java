package org.jabref.logic.search.inmemory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MatchInformation {

    private Boolean result;
    private Set<PartialResult> partialResults = new HashSet<>();

    public MatchInformation() {
    }

    public MatchInformation(Boolean result) {
        this.result = result;
    }

    public MatchInformation(Boolean result, PartialResult... partialResults) {
        this.result = result;
        Collections.addAll(this.partialResults, partialResults);
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    public Set<PartialResult> getPartialResults() {
        return partialResults;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MatchInformation that = (MatchInformation) o;
        return Objects.equals(result, that.result) && Objects.equals(partialResults, that.partialResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, partialResults);
    }

    @Override
    public String toString() {
        return "MatchInformation{" +
                "result=" + result +
                ", partialResults=" + partialResults +
                '}';
    }

    public record PartialResult(boolean isTrue, String subquery) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PartialResult that = (PartialResult) o;
            return isTrue == that.isTrue && Objects.equals(subquery, that.subquery);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isTrue, subquery);
        }

        @Override
        public String toString() {
            return "PartialResult{" +
                    "isTrue=" + isTrue +
                    ", subquery='" + subquery + '\'' +
                    '}';
        }
    }
}

