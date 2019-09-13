package org.jabref.logic.openoffice;

import java.net.URL;
import java.net.URLClassLoader;

public class ChildFirstClassLoader extends URLClassLoader {

    public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // has the class loaded already?
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            try {
                // find the class from given jar urls 
                loadedClass = findClass(name);
            } catch (ClassNotFoundException e) {
                // Hmmm... class does not exist in the given urls.
                // Let's try finding it in our parent classloader.
                // this'll throw ClassNotFoundException in failure.                  
                loadedClass = super.loadClass(name, resolve);
            }
        }
        if (resolve) { // marked to resolve
            resolveClass(loadedClass);
        }
        return loadedClass;
    }
}