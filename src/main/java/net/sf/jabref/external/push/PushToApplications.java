package net.sf.jabref.external.push;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.sf.jabref.openoffice.OpenOfficePanel;

public class PushToApplications {
    public static final List<PushToApplication> applications;
    
    /**
     * Set up the current available choices:
     */
    static {
        //TODO plugins create collection class
        applications = new ArrayList<PushToApplication>();

        PushToApplications.applications.add(new PushToLyx());
        PushToApplications.applications.add(new PushToEmacs());
        PushToApplications.applications.add(new PushToWinEdt());
        PushToApplications.applications.add(new PushToLatexEditor());
        PushToApplications.applications.add(new PushToVim());
        PushToApplications.applications.add(OpenOfficePanel.getInstance());
        PushToApplications.applications.add(new PushToTeXstudio());
        PushToApplications.applications.add(new PushToTexmaker());

        // Finally, sort the entries:
        //Collections.sort(applications, new PushToApplicationComparator());
    }

    /**
     * Comparator for sorting the selection according to name.
     */
    private static class PushToApplicationComparator implements Comparator<PushToApplication> {

        @Override
        public int compare(PushToApplication one, PushToApplication two) {
            return one.getName().compareTo(two.getName());
        }
    }
}
