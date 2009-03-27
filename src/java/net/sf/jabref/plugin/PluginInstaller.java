package net.sf.jabref.plugin;

import net.sf.jabref.net.URLDownload;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

/**
 *
 */
public class PluginInstaller {

    public static final int
        SUCCESS = 0,
        UNABLE_TO_CREATE_DIR = 1,
        UNABLE_TO_COPY_FILE = 2;

    public static final int
        NO_VERSIONS_INSTALLED = 0,
        NEWER_VERSION_INSTALLED = 1,
        SAME_VERSION_INSTALLED = 2,
        OLDER_VERSION_INSTALLED = 3;

    public static void installPlugin(JabRefFrame frame, URL source) {
        String fileName = (new File(source.getFile())).getName();
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                JOptionPane.showMessageDialog(frame, Globals.lang("Unable to create plugin directory")
                    +" ("+PluginCore.userPluginDir.getPath()+").", Globals.lang("Plugin installer"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        int status = checkInstalledVersion(fileName);
        int result;
        switch (status) {
            case NO_VERSIONS_INSTALLED:
                result = copyPlugin(frame, source, fileName);
                if (result == SUCCESS)
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installed successfully. You must restart JabRef to load the new plugin."),
                            Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                else {
                    String reason;
                    if (result == UNABLE_TO_COPY_FILE)
                        reason = Globals.lang("Unable to copy file");
                    else
                        reason = Globals.lang("Unable to create user plugin directory")
                            +" ("+PluginCore.userPluginDir.getPath()+").";
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.")+" "+reason,
                            Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                }
                break;
            case SAME_VERSION_INSTALLED:
                JOptionPane.showMessageDialog(frame, Globals.lang("The same version of this plugin is already installed."),
                        Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                break;
            case NEWER_VERSION_INSTALLED:
                JOptionPane.showMessageDialog(frame, Globals.lang("A newer version of this plugin is already installed."),
                        Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                break;
            case OLDER_VERSION_INSTALLED:
                result = copyPlugin(frame, source, fileName);
                if (result == SUCCESS) {
                    int answer = JOptionPane.showConfirmDialog(frame,
                            Globals.lang("One or more older versions of this plugin is installed. Delete old versions?"),
                            Globals.lang("Plugin installer"), JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        System.out.println("implement delete old");
                    }
                }
                else {
                    String reason;
                    if (result == UNABLE_TO_COPY_FILE)
                        reason = Globals.lang("Unable to copy file");
                    else
                        reason = Globals.lang("Unable to create user plugin directory")
                            +" ("+PluginCore.userPluginDir.getPath()+").";
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.")+" "+reason,
                            Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                }
        }
    }

    /**
     * Check the status of the named plugin - whether an older, the same or a
     * newer version is already installed.
     * @param filename The filename of the plugin.
     * @return an integer indicating the status
     */
    public static int checkInstalledVersion(String filename) {
        String[] nav = getNameAndVersion(filename);
        VersionNumber vn = new VersionNumber(nav[1]);
        List<VersionNumber> versions = getInstalledVersions(nav[0]);
        for (Iterator<VersionNumber> stringIterator = versions.iterator(); stringIterator.hasNext();) {

            System.out.println(stringIterator.next().toString());
        }
        boolean hasSame = versions.size() > 0 && (vn.compareTo(versions.get(0)) == 0);
        boolean hasNewer = versions.size() > 0 && (vn.compareTo(versions.get(0)) > 0);

        if (versions.size() == 0) {
            return NO_VERSIONS_INSTALLED;
        }
        if (hasNewer)
            return NEWER_VERSION_INSTALLED;
        if (hasSame)
            return SAME_VERSION_INSTALLED;

        return OLDER_VERSION_INSTALLED;
    }

    /**
     * Copy a plugin to the user plugin directory. Does not check whether the plugin
     * already exists.
     * @param source The local or remote location to copy the plugin from.
     * @return true if the install was successful
     */
    public static int copyPlugin(JFrame frame, URL source, String destFileName) {
        if (destFileName == null)
            destFileName = source.getFile();
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                return UNABLE_TO_CREATE_DIR;
            }
        }
        File destFile = new File(PluginCore.userPluginDir, destFileName);
        URLDownload ud = new URLDownload(frame, source, destFile);

        try {
            ud.download();
            return SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
            return UNABLE_TO_COPY_FILE;
        }
    }



    /**
     * Based on a plugin name, find all versions that are already present
     * in the user plugin directory.
     * @param pluginName The name of the plugin.
     * @return A list of versions already present.
     */
    public static List<VersionNumber> getInstalledVersions(final String pluginName) {

        String[] files = PluginCore.userPluginDir.list(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.startsWith(pluginName) && s.endsWith(".jar");
            }
        });
        List<VersionNumber> versions = new ArrayList<VersionNumber>();
        for (int i = 0; i < files.length; i++) {
            String file = files[i];
            String[] nav = getNameAndVersion(file);
            if (nav != null)
                versions.add(new VersionNumber(nav[1]));
        }
        Collections.sort(versions);
        return versions;
    }


    static Pattern pluginFilePattern = Pattern.compile("(.*)-([\\d\\.]+).jar");

    /**
     * Try to split up a plugin file name in order to find the plugin name and
     * the version number. The file name is expected to be on the format
     * [plugin name]-[version number].jar
     *
     * @param filename The plugin file name.
     * @return A string array containing the plugin name in the first element and
     *   the version number in the second, or null if the filename couldn't be
     *   interpreted.
     *
     */
    public static String[] getNameAndVersion(String filename) {
        Matcher m = pluginFilePattern.matcher(filename);
        if (m.matches()) {
            return new String[] {m.group(1), m.group(2)};
        }
        else
            return null;
    }

    /**
     * Make a File pointing to a file with the correct name in the user
     * plugin directory.
     * @param name The plugin name.
     * @param version The plugin version.
     * @return the correct File.
     */
    public static String buildFileName(String name, String version) {
        return PluginCore.userPluginDir+"/"+name+"-"+version+".jar";
    }

    static class VersionNumber implements Comparable {
        List<Integer> digits;
        public VersionNumber(String number) {
            digits = new ArrayList<Integer>();
            String[] elms = number.split("\\.");
            for (int i = 0; i < elms.length; i++) {
                try {
                    int num = Integer.parseInt(elms[i]);
                    digits.add(num);
                } catch (NumberFormatException ex) {
                    // Do nothing
                }

            }
        }

        public int compareTo(Object o) {
            VersionNumber oth = (VersionNumber)o;
           
            for (int i=0; i<Math.min(digits.size(), oth.digits.size()); i++) {
                if (digits.get(i) != oth.digits.get(i))
                    return oth.digits.get(i)-digits.get(i);
            }
            // All digits equal so far, and only one of the numbers has more digits.
            // The one with digits remaining is the newest one:
            return oth.digits.size()-digits.size();
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Iterator<Integer> integerIterator = digits.iterator(); integerIterator.hasNext();) {
                sb.append(integerIterator.next());
                if (integerIterator.hasNext())
                    sb.append(".");
            }
            return sb.toString();
        }
    }
}
