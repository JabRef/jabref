package net.sf.jabref.plugin;

import net.sf.jabref.net.URLDownload;

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

    public static void checkInstalledVersion(String filename) {
        String[] nav = getNameAndVersion(filename);
        VersionNumber vn = new VersionNumber(nav[1]);
        System.out.println("Name: '"+nav[0]+"'");
        System.out.println("Version: '"+nav[1]+"'");
        List<VersionNumber> versions = getInstalledVersions(nav[0]);
        System.out.println("Installed versions:");
        for (Iterator<VersionNumber> stringIterator = versions.iterator(); stringIterator.hasNext();) {

            System.out.println(stringIterator.next().toString());
        }
        boolean hasSame = versions.size() > 0 && (vn.compareTo(versions.get(0)) == 0);
        boolean hasNewer = versions.size() > 0 && (vn.compareTo(versions.get(0)) > 0);
        System.out.println("Has same? "+hasSame);
        System.out.println("Has newer? "+hasNewer);
    }

    /**
     * Copy a plugin to the user plugin directory. Does not check whether the plugin
     * already exists.
     * @param source The local or remote location to copy the plugin from.
     * @return true if the install was successful
     */
    public static boolean installPlugin(JFrame frame, URL source, String destFileName) {
        if (destFileName == null)
            destFileName = source.getFile();
        System.out.println("Destination name: '"+destFileName+"'");
        File destFile = new File(PluginCore.userPluginDir, destFileName);
        URLDownload ud = new URLDownload(frame, source, destFile);

        try {
            ud.download();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
                System.out.println(":: "+s);
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
