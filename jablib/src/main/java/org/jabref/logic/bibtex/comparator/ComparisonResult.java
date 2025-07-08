package org.jabref.logic.bibtex.comparator;

public enum ComparisonResult {
    LEFT_BETTER(-1),
    RIGHT_BETTER(1),
    UNDETERMINED(0);

    private final int result;

    ComparisonResult(int result) {
        this.result = result;
    }

    public int getResult() {
        return this.result;
    }

    public static ComparisonResult fromInt(int i) {
        return switch (i) {
            case 1 ->
                    RIGHT_BETTER;
            case -1 ->
                    LEFT_BETTER;
            default ->
                    UNDETERMINED;
        };
    }
}
