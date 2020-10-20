package org.jabref.gui.keyboard;

public class EmacsKeyPreferences {
    private final boolean useEmacsKeyBindings;
    private final boolean rebindCA;
    private final boolean rebindCF;
    private final boolean rebindCN;
    private final boolean rebindAU;

    public EmacsKeyPreferences(boolean useEmacsKeyBindings,
                               boolean rebindCA,
                               boolean rebindCF,
                               boolean rebindCN,
                               boolean rebindAU) {
        this.useEmacsKeyBindings = useEmacsKeyBindings;
        this.rebindCA = rebindCA;
        this.rebindCF = rebindCF;
        this.rebindCN = rebindCN;
        this.rebindAU = rebindAU;
    }

    public boolean useEmacsKeyBindings() {
        return useEmacsKeyBindings;
    }

    public boolean shouldRebindCA() {
        return rebindCA;
    }

    public boolean shouldRebindCF() {
        return rebindCF;
    }

    public boolean shouldRebindCN() {
        return rebindCN;
    }

    public boolean shouldRebindAU() {
        return rebindAU;
    }
}
