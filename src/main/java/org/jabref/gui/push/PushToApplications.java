package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;

public class PushToApplications {

    private final List<PushToApplication> applications;


    public PushToApplications() {
    /**
     * Set up the current available choices:
     */

        applications = new ArrayList<>();

        applications.add(new PushToEmacs());
        applications.add(new PushToLyx());
        applications.add(new PushToTexmaker());
        applications.add(new PushToTeXstudio());
        applications.add(new PushToVim());
        applications.add(new PushToWinEdt());
    }

    public List<PushToApplication> getApplications() {
        return applications;
    }
}
