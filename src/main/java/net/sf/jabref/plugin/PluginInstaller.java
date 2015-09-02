/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.plugin;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.regex.Pattern;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;

import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.registry.ManifestProcessingException;
import org.java.plugin.registry.ManifestInfo;

/**
 *
 */
public class PluginInstaller {

    private static final String PLUGIN_XML_FILE = "plugin.xml";
    private static final int
            SUCCESS = 0;
    private static final int UNABLE_TO_CREATE_DIR = 1;
    private static final int UNABLE_TO_COPY_FILE = 2;

    private static final int
            NO_VERSIONS_INSTALLED = 0;
    private static final int NEWER_VERSION_INSTALLED = 1;
    private static final int SAME_VERSION_INSTALLED = 2;
    private static final int OLDER_VERSION_INSTALLED = 3;
    public static final int UNCONVENTIONAL_FILENAME = 4;
    private static final int UNKNOWN_VERSION = 5;

    public static final int
            NOT_LOADED = 0;
    private static final int LOADED = 1;
    private static final int BAD = 2;


    public static void installPlugin(JabRefFrame frame, File file, String targetFileName) {
        String fileName = targetFileName != null ? targetFileName : file.getName();
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                JOptionPane.showMessageDialog(frame, Globals.lang("Unable to create plugin directory")
                        + " (" + PluginCore.userPluginDir.getPath() + ").", Globals.lang("Plugin installer"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        int status = PluginInstaller.checkInstalledVersion(file);
        int result;
        switch (status) {
        case NO_VERSIONS_INSTALLED:
            result = PluginInstaller.copyPlugin(frame, file, fileName);
            if (result == PluginInstaller.SUCCESS) {
                JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installed successfully. You must restart JabRef to load the new plugin."),
                        Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
            } else {
                String reason;
                if (result == PluginInstaller.UNABLE_TO_COPY_FILE) {
                    reason = Globals.lang("Unable to copy file");
                } else {
                    reason = Globals.lang("Unable to create user plugin directory")
                            + " (" + PluginCore.userPluginDir.getPath() + ").";
                }
                JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.") + " " + reason,
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
            result = PluginInstaller.copyPlugin(frame, file, fileName);
            if (result == PluginInstaller.SUCCESS) {
                int answer = JOptionPane.showConfirmDialog(frame,
                        Globals.lang("One or more older versions of this plugin is installed. Delete old versions?"),
                        Globals.lang("Plugin installer"), JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    boolean success = PluginInstaller.deleteOlderVersions(file);
                    if (success) {
                        JOptionPane.showMessageDialog(frame,
                                Globals.lang("Old versions deleted successfully."),
                                Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                Globals.lang("Old plugin versions will be deleted next time JabRef starts up."),
                                Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
            else {
                String reason;
                if (result == PluginInstaller.UNABLE_TO_COPY_FILE) {
                    reason = Globals.lang("Unable to copy file");
                } else {
                    reason = Globals.lang("Unable to create user plugin directory")
                            + " (" + PluginCore.userPluginDir.getPath() + ").";
                }
                JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.") + " " + reason,
                        Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
            }
            break;
        //case UNKNOWN_VERSION:
        //    JOptionPane.showMessageDialog(frame, Globals.lang("Could not determine version of "));
        //    break;
        case UNKNOWN_VERSION:
            JLabel lab = new JLabel("<html>" + Globals.lang("Unable to determine plugin name and "
                    + "version. This may not be a valid JabRef plugin.")
                    + "<br>" + Globals.lang("Install anyway?") + "</html>");
            int answer = JOptionPane.showConfirmDialog(frame, lab,
                    Globals.lang("Plugin installer"), JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                result = PluginInstaller.copyPlugin(frame, file, fileName);
                if (result == PluginInstaller.SUCCESS) {
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installed successfully. You must restart JabRef to load the new plugin."),
                            Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    String reason;
                    if (result == PluginInstaller.UNABLE_TO_COPY_FILE) {
                        reason = Globals.lang("Unable to copy file");
                    } else {
                        reason = Globals.lang("Unable to create user plugin directory")
                                + " (" + PluginCore.userPluginDir.getPath() + ").";
                    }
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.") + " " + reason,
                            Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                }

            }
            break;
        }
    }

    /**
     * Check the status of the named plugin - whether an older, the same or a
     * newer version is already installed.
     * @param f The plugin file.
     * @return an integer indicating the status
     */
    private static int checkInstalledVersion(File f) {
        String[] nav = PluginInstaller.getNameAndVersion(f);
        if (nav == null) {
            return PluginInstaller.UNKNOWN_VERSION;
        }

        VersionNumber vn = new VersionNumber(nav[1]);
        Map<VersionNumber, File> versions = PluginInstaller.getInstalledVersions(nav[0]);

        if (versions.size() == 0) {
            return PluginInstaller.NO_VERSIONS_INSTALLED;
        }
        VersionNumber thenum = versions.keySet().iterator().next();
        boolean hasSame = vn.compareTo(thenum) == 0;
        boolean hasNewer = vn.compareTo(thenum) > 0;

        if (hasNewer) {
            return PluginInstaller.NEWER_VERSION_INSTALLED;
        }
        if (hasSame) {
            return PluginInstaller.SAME_VERSION_INSTALLED;
        }

        return PluginInstaller.OLDER_VERSION_INSTALLED;
    }

    /**
     * Delete the given plugin.
     * @param plugin Name and version information for the plugin to delete.
     * @return true if deletion is successful, false otherwise.
     */
    public static boolean deletePlugin(NameAndVersion plugin) {
        /*String file = buildFileName(plugin.name,
                plugin.version.equals(VersionNumber.ZERO) ? null : plugin.version.toString());*/
        return PluginInstaller.deletePluginFile(plugin.file);
    }

    private static boolean deleteOlderVersions(File f) {
        String[] nav = PluginInstaller.getNameAndVersion(f);
        if (nav == null) {
            return false;
        }
        boolean success = true;
        VersionNumber num = new VersionNumber(nav[1]);
        Map<VersionNumber, File> versions = PluginInstaller.getInstalledVersions(nav[0]);
        for (VersionNumber versionNumber : versions.keySet()) {
            if (num.compareTo(versionNumber) < 0) {
                String vnString = versionNumber.equals(VersionNumber.ZERO) ? null : versionNumber.toString();
                File file = versions.get(versionNumber);//buildFileName(nav[0], vnString);
                success = PluginInstaller.deletePluginFile(file);//file).delete() && success;
            }
        }
        return success;
    }

    /**
     * This method deletes a plugin file. If deletion fails - typically happens
     * on Windows due to file locking - the file is scheduled for deletion on
     * the next startup.
     *
     * @param f The file to delete.
     * @return true if deletion was successful, false if scheduled for later.
     */
    private static boolean deletePluginFile(File f) {
        boolean success = f.delete();
        if (success) {
            return true;
        } else {
            PluginInstaller.schedulePluginForDeletion(f.getPath());
            return false;
        }
    }

    /**
     * Copy a plugin to the user plugin directory. Does not check whether the plugin
     * already exists.
     * @param source The local or remote location to copy the plugin from.
     * @return true if the install was successful
     */
    public static int copyPlugin(JFrame frame, URL source, String destFileName) {
        if (destFileName == null) {
            destFileName = source.getFile();
        }
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                return PluginInstaller.UNABLE_TO_CREATE_DIR;
            }
        }
        File destFile = new File(PluginCore.userPluginDir, destFileName);
        URLDownload ud = URLDownload.buildMonitoredDownload(frame, source);

        try {
            ud.downloadToFile(destFile);
            return PluginInstaller.SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
            return PluginInstaller.UNABLE_TO_COPY_FILE;
        }
    }

    private static int copyPlugin(JFrame frame, File source, String destFileName) {
        if (destFileName == null) {
            destFileName = source.getName();
        }
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                return PluginInstaller.UNABLE_TO_CREATE_DIR;
            }
        }
        File destFile = new File(PluginCore.userPluginDir, destFileName);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(source));
            out = new BufferedOutputStream(new FileOutputStream(destFile));
            byte[] buf = new byte[1024];
            int count;
            while ((count = in.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, count);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return PluginInstaller.UNABLE_TO_COPY_FILE;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // UNABLE_TO_COPY_FILE;
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                    // UNABLE_TO_COPY_FILE;
                }
            }
        }
        return PluginInstaller.SUCCESS;
    }

    /**
     * Based on a plugin name, find all versions that are already present
     * in the user plugin directory.
     * @param pluginName The name of the plugin.
     * @return A map of versions already present, linking to the file containing each.
     */
    private static Map<VersionNumber, File> getInstalledVersions(final String pluginName) {

        String[] files = PluginCore.userPluginDir.list(new FilenameFilter() {

            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".jar");
            }
        });
        Map<VersionNumber, File> versions = new TreeMap<VersionNumber, File>();
        for (String file : files) {
            File f = new File(PluginCore.userPluginDir, file);
            String[] nav = PluginInstaller.getNameAndVersion(f);
            if (nav != null) {
                if (nav[0].equals(pluginName)) {
                    VersionNumber vn = new VersionNumber(nav[1]);
                    versions.put(vn, f);
                }
            }

        }

        return versions;
    }

    /**
     * Add the given filename to the list of plugins to be deleted on the next
     * JabRef startup.
     *
     * @param filename The path to the file to delete.
     */
    private static void schedulePluginForDeletion(String filename) {
        String[] oldValues = Globals.prefs.getStringArray(JabRefPreferences.DELETE_PLUGINS);
        String[] newValues = oldValues == null ? new String[1] : new String[oldValues.length + 1];
        if (oldValues != null) {
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
        }
        newValues[newValues.length - 1] = filename;
        Globals.prefs.putStringArray(JabRefPreferences.DELETE_PLUGINS, newValues);
    }

    /**
     * Delete the given files. Refuses to delete files outside the user plugin directory.
     * This method throws no errors is the files don't exist or deletion failed.
     * @param filenames An array of names of the files to be deleted.
     */
    public static void deletePluginsOnStartup(String[] filenames) {
        for (String s : filenames) {
            File f = new File(s);
            if (f.getParentFile().equals(PluginCore.userPluginDir)) {
                //if (s.startsWith(PluginCore.userPluginDir.getPath())) {
                boolean success = f.delete();
            } else {
                System.out.println("File outside of user plugin dir: " + s);
            }
        }
    }


    static Pattern pluginFilePattern = Pattern.compile("(.*)-([\\d\\.]+).jar");
    static Pattern pluginFilePatternNoVersion = Pattern.compile("(.*).jar");


    /**
     * Look inside a jar file, find the plugin.xml file, and use it to determine the name
     * and version of the plugin.
     *
     * @param f The file to investigate.
     * @return A string array containing the plugin name in the first element and
     *   the version number in the second, or null if the filename couldn't be
     *   interpreted.
     *
     */
    private static String[] getNameAndVersion(File f) {

        try {
            File temp = PluginInstaller.unpackPluginXML(f);
            if (temp == null)
             {
                return null; // Couldn't find the plugin.xml file
            }
            ManifestInfo mi = PluginCore.getManager().getRegistry().
                    readManifestInfo(temp.toURI().toURL());
            temp.delete();
            return new String[] {mi.getId(), mi.getVersion().toString()};
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (ManifestProcessingException e) {
            return null; // Couldn't make sense of the plugin.xml
        }

    }

    /**
     * Take the name of a jar file and extract the plugin.xml file, if possible,
     * to a temporary file.
     * @param f The jar file to extract from.
     * @return a temporary file to which the plugin.xml file has been copied.
     */
    private static File unpackPluginXML(File f) {
        InputStream in = null;
        OutputStream out = null;

        try {
            JarFile jar = new JarFile(f);
            ZipEntry entry = jar.getEntry(PluginInstaller.PLUGIN_XML_FILE);
            if (entry == null) {
                return null;
            }
            File dest = File.createTempFile("jabref_plugin", ".xml");
            dest.deleteOnExit();

            in = new BufferedInputStream(jar.getInputStream(entry));
            out = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[2048];
            for (;;) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0) {
                    break;
                }
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            return dest;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Build a list of installed plugins.
     * @return a list of plugin names and version numbers.
     */
    public static EventList<NameAndVersion> findInstalledPlugins() {
        EventList<NameAndVersion> plugins = new BasicEventList<NameAndVersion>();
        if (!PluginCore.userPluginDir.exists()) {
            return plugins;
        }
        String[] files = PluginCore.userPluginDir.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        HashMap<String, PluginDescriptor> urls = new HashMap<String, PluginDescriptor>();
        Collection<PluginDescriptor> descriptors =
                PluginCore.getManager().getRegistry().getPluginDescriptors();
        for (PluginDescriptor desc : descriptors) {
            if ((desc.getPluginClassName() == null) || !desc.getPluginClassName()
                    .equals("net.sf.jabref.plugin.core.JabRefPlugin")) {
                urls.put(desc.getId(), desc);
            }
        }

        for (String file1 : files) {
            File file = new File(PluginCore.userPluginDir, file1);
            String[] nav = PluginInstaller.getNameAndVersion(file);
            if (nav != null) {
                VersionNumber vn = nav[1] != null ? new VersionNumber(nav[1]) : null;
                NameAndVersion nameAndVersion = new NameAndVersion(nav[0], vn, true,
                        file);
                for (Iterator<String> it = urls.keySet().iterator(); it.hasNext();) {
                    String loc = it.next();
                    if (loc.contains(nav[0])) {
                        PluginDescriptor desc = urls.get(loc);
                        //System.out.println("Accounted for: "+desc.getId()+" "+desc.getVersion().toString());
                        if (!PluginCore.getManager().isPluginEnabled(urls.get(loc))) {
                            nameAndVersion.setStatus(PluginInstaller.BAD);
                        } else {
                            nameAndVersion.setStatus(PluginInstaller.LOADED);
                        }
                        it.remove();
                    }
                }
                plugins.add(nameAndVersion);
            }
        }

        for (String url : urls.keySet()) {
            PluginDescriptor desc = urls.get(url);
            File location = new File(desc.getLocation().getFile());
            if (location.getPath().contains(PluginCore.userPluginDir.getPath()))
             {
                continue; // This must be a loaded user dir plugin that's been deleted.
            }
            //System.out.println("File: "+desc.getLocation().getFile());
            NameAndVersion nameAndVersion = new NameAndVersion(desc.getId(),
                    new VersionNumber(desc.getVersion().toString()), false,
                    location);
            if (!PluginCore.getManager().isPluginEnabled(urls.get(url))) {
                nameAndVersion.setStatus(PluginInstaller.BAD);
            } else {
                nameAndVersion.setStatus(PluginInstaller.LOADED);
            }
            plugins.add(nameAndVersion);
        }
        return plugins;
    }


    public static class NameAndVersion implements Comparable<NameAndVersion> {

        final String name;
        final VersionNumber version;
        int status = 0;
        final boolean inUserDirectory;
        final File file;


        public NameAndVersion(String name, VersionNumber version, boolean inUserDirectory,
                File file) {
            this.name = name;
            this.version = version;
            this.inUserDirectory = inUserDirectory;
            this.file = file;
        }

        @Override
        public int compareTo(NameAndVersion oth) {
            if (!name.equals(oth.name)) {
                return name.compareTo(oth.name);
            } else {
                if (version == null) {
                    return 1;
                } else if (oth.version == null) {
                    return -1;
                } else {
                    return version.compareTo(oth.version);
                }
            }
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }

    static class VersionNumber implements Comparable<VersionNumber> {

        public static final VersionNumber ZERO = new VersionNumber("0");
        final List<Integer> digits;


        public VersionNumber(String number) {
            digits = new ArrayList<Integer>();
            String[] elms = number.split("\\.");
            for (String elm : elms) {
                try {
                    int num = Integer.parseInt(elm);
                    digits.add(num);
                } catch (NumberFormatException ex) {
                    // Do nothing
                }

            }
        }

        @Override
        public int compareTo(VersionNumber oth) {
            for (int i = 0; i < Math.min(digits.size(), oth.digits.size()); i++) {
                if (!digits.get(i).equals(oth.digits.get(i))) {
                    return oth.digits.get(i) - digits.get(i);
                }
            }
            // All digits equal so far, and only one of the numbers has more digits.
            // The one with digits remaining is the newest one:
            return oth.digits.size() - digits.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Iterator<Integer> integerIterator = digits.iterator(); integerIterator.hasNext();) {
                sb.append(integerIterator.next());
                if (integerIterator.hasNext()) {
                    sb.append(".");
                }
            }
            return sb.toString();
        }

        public boolean equals(VersionNumber o) {
            return compareTo(o) == 0;
        }
    }
}
