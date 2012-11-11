package spl.filter;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 08.09.2010
 * Time: 15:03:36
 * To change this template use File | Settings | File Templates.
 */
public class PdfFileFilter implements FileFilter {

    public boolean accept(File file) {
        String path = file.getPath();

        return isMatchingFileFilter(path);
    }

    public boolean accept(String path) {
        if(path == null || path.isEmpty() || !path.contains(".")) return false;

        return isMatchingFileFilter(path);
    }

    private boolean isMatchingFileFilter(String path) {
        String dateiEndung = path.substring(path.lastIndexOf(".") + 1);
        if(dateiEndung.equalsIgnoreCase("pdf")){
            return true;
        }
        else{
            return false;
        }
    }
    
}
