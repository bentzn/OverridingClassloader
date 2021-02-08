package com.bentzn.products.appserver.core.util.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author bentzn
 */
public class JarFileEntry {
    
    
    private static final Logger cLogger = LogManager.getLogger(JarFileEntry.class);

    private String entryName;
    private String className;
    private File file;
    private JarFile jarFile;
    private JarEntry jarEntry;
    private boolean isClass;


    public JarFileEntry(String entryName, File file, JarFile jarFile, JarEntry jarEntry) {
        this.entryName = entryName;
        this.file = file;
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
        
        isClass = entryName.endsWith(".class");
        if(isClass)
            className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
    }


    public String getEntryName() {
        return entryName;
    }


    public String getClassName() {
        return className;
    }


    public JarFile getJarFile() {
        return jarFile;
    }


    public JarEntry getJarEntry() {
        return jarEntry;
    }

    
    public boolean isClass() {
        return isClass;
    }
    
    
    public URL getUrl(){
        try {
            return new URL("jar:file:" + file.getAbsolutePath() + "!/" + entryName);
        }
        catch (MalformedURLException e) {
            cLogger.warn("", e);
        }
        
        return null;
    }


    @Override
    public String toString() {
        return "JarFileEntry [className=" + className + ", file=" + file + ", jarFile=" + jarFile
                + ", jarEntry=" + jarEntry + ", isClass=" + isClass + ", getUrl()=" + getUrl()
                + "]";
    }

    
    


}
