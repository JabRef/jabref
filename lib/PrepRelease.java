import java.io.*;

/**
 * This little program traverses the file structure from the current
 * directory, and removes all CVS directories and all files starting
 * with ".#" or ending with "~", e.g. backup files. This is to make the
 * source tree ready for release.
 */
public class PrepRelease {

    public static void main(String[] args) {
	File start = new File(System.getProperty("user.dir"));
	System.out.println(start.getPath());
	traverse(start);
    }

    private static void traverse(File f) {
	//System.out.println(f.getPath());
	File[] fs = f.listFiles();
	for (int i=0; i<fs.length; i++) {
	    if (fs[i].getName().equals("CVS") || fs[i].getName().equals(".svn")) {
		delete(fs[i]);
		fs[i].delete();
		System.out.println("Deleting dir: "+fs[i].getPath());
	    } else if (fs[i].isDirectory()) {
		traverse(fs[i]);
	    } else if (fs[i].getName().endsWith("~") ||
		       fs[i].getName().startsWith(".#")) {
		System.out.println("Deleting: "+fs[i].getPath());
		fs[i].delete();
	    }
	}
    }

    private static void delete(File f) {
	File[] fs = f.listFiles();
	for (int i=0; i<fs.length; i++) {
	    System.out.println("Deleting: "+fs[i].getPath());
	    if (fs[i].isDirectory())
		delete(fs[i]);
	    fs[i].delete();
	}
    }
}
