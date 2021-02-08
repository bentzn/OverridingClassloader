package com.bentzn.products.appserver.core.util.classloader;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.*;

/**
 * @author bentzn
 */
public class OverridingClassloader extends ClassLoader {

    private static final Logger cLogger = LogManager.getLogger(OverridingClassloader.class);

    private File m_cDirProduct;
    private File m_cDirProject;

    private Map<String, JarFileEntry> m_cMapResources = new HashMap<>();
    private Map<String, Class<?>> m_cMapClasses = new HashMap<>();

    /**
     * Will load classes in the constructor
     */
    public OverridingClassloader(ClassLoader a_cParent, File a_cDirProduct, File a_cDirProject) throws Exception {
        super(a_cParent);

        if (cLogger.isDebugEnabled())
            cLogger.debug("Product dir: " + a_cDirProduct + ", project dir: " + a_cDirProject);

        m_cDirProduct = a_cDirProduct;
        m_cDirProject = a_cDirProject;
        loadClassMap();
    }



    private void loadClassMap() throws IOException, ClassNotFoundException {
        // List all classes in all jars in product
        File[] acFilesProduct = m_cDirProduct.listFiles();
        for (File cFile : acFilesProduct) {
            if (FilenameUtils.getExtension(cFile.getName()).equalsIgnoreCase("jar")) {
                Map<String, JarFileEntry> cMap = getEntries(cFile);
                for (String cKey : cMap.keySet()) {
                    m_cMapResources.put(cKey, cMap.get(cKey));
                }
            }
        }

        // replace classes in map with classes from project jars
        File[] acFilesProject = m_cDirProject.listFiles();
        for (File cFile : acFilesProject) {
            if (FilenameUtils.getExtension(cFile.getName()).equalsIgnoreCase("jar")) {
                Map<String, JarFileEntry> cMap = getEntries(cFile);

                // replace classes
                for (String cKey : cMap.keySet()) {
                    JarFileEntry cJarFileEntry = cMap.get(cKey);

                    if (cLogger.isTraceEnabled())
                        cLogger.trace("Adding/Replacing class/resource: " + cKey);

                    JarFileEntry cEntry = m_cMapResources.put(cKey, cJarFileEntry);
                    if (cEntry != null && cLogger.isTraceEnabled())
                        cLogger.trace("Replaced resource: " + cEntry);
                }
            }
        }

        // load all classes in the map
        for (String cKey : m_cMapResources.keySet()) {
            JarFileEntry cEntry = m_cMapResources.get(cKey);
            if (cEntry.isClass())
                m_cMapClasses.put(cEntry.getClassName(), loadClass(cEntry.getClassName()));
        }

        if (cLogger.isDebugEnabled())
            cLogger.debug("Loaded " + m_cMapClasses.size() + " classes");

        // Close jar files
        for (JarFileEntry cJarFileEntry : m_cMapResources.values()) {
            cJarFileEntry.getJarFile().close();
        }
    }



    private Map<String, JarFileEntry> getEntries(File a_cFile) throws IOException {
        if (cLogger.isDebugEnabled())
            cLogger.debug("Reading entries from jar file: " + a_cFile);

        Map<String, JarFileEntry> cMap = new HashMap<>();
        JarFile cJarFile = new JarFile(a_cFile);
        Enumeration<JarEntry> cEntries = cJarFile.entries();
        while (cEntries.hasMoreElements()) {
            JarEntry cJarEntry = cEntries.nextElement();

            if (cJarEntry.isDirectory())
                continue;

            JarFileEntry cJarFileEntry = new JarFileEntry(cJarEntry.getName(), a_cFile, cJarFile, cJarEntry);
            cMap.put(cJarEntry.getName(), cJarFileEntry);

            if (cLogger.isTraceEnabled())
                cLogger.trace("Added entry: " + cJarFileEntry + " to map");
        }

        return cMap;
    }



    public List<Class<?>> getLoadedClasses() {
        return new LinkedList<>(m_cMapClasses.values());
    }



    @Override
    protected Class<?> findClass(String a_cName) throws ClassNotFoundException {
        if (cLogger.isTraceEnabled())
            cLogger.trace("Find class: " + a_cName);

        Class<?> cClass = m_cMapClasses.get(a_cName);
        if (cClass != null) {
            if (cLogger.isTraceEnabled())
                cLogger.trace("Returning previously loaded class: " + a_cName);
            return cClass;
        }

        JarFileEntry cJarFileEntry = m_cMapResources.get(a_cName.replace('.', '/') + ".class");

        if (cJarFileEntry == null) {
            if (cLogger.isTraceEnabled())
                cLogger.trace("No jar file entry with this name: " + a_cName);

            throw new ClassNotFoundException(a_cName);
        }

        return loadClassData(cJarFileEntry);
    }



    @Override
    protected URL findResource(String name) {
        if (cLogger.isTraceEnabled())
            cLogger.trace("Find resource: " + name);

        URL cUrl = getParent().getResource(name);
        if (cUrl != null) {
            if (cLogger.isTraceEnabled())
                cLogger.trace("Found resource (parent): " + name + ", URL: " + cUrl);

            return cUrl;
        }

        JarFileEntry cEntry = m_cMapResources.get(name);
        if (cEntry != null) {
            cUrl = cEntry.getUrl();
            if (cLogger.isTraceEnabled())
                cLogger.trace("Found resource: " + name + ", URL: " + cUrl);

            return cUrl;
        }

        return null;
    }



    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (cLogger.isTraceEnabled())
            cLogger.trace("Find resources: " + name);

        Enumeration<URL> cUrls = getParent().getResources(name);
        if (cUrls != null && cUrls.hasMoreElements()) {
            if (cLogger.isTraceEnabled()) {
                URL cFirstUrl = null;
                if (cUrls.hasMoreElements())
                    cFirstUrl = cUrls.nextElement();

                cLogger.trace("Found resources (parent): " + name + ", first URL: " + cFirstUrl);
            }

            return cUrls;
        }

        Vector<URL> cVct = new Vector<URL>();

        JarFileEntry cEntry = m_cMapResources.get(name);
        if (cEntry != null) {
            cVct.add(cEntry.getUrl());
            if (cLogger.isTraceEnabled())
                cLogger.trace("Found resource(s): " + name + ", URLs: " + cEntry.getUrl());
        }

        return cVct.elements();
    }



    private Class<?> loadClassData(JarFileEntry a_cJarFileEntry) {
        // Read the Jar File entry and define the class...
        if (cLogger.isTraceEnabled())
            cLogger.trace("Loading class data: " + a_cJarFileEntry.toString());

        Class<?> cClass = null;
        try {
            InputStream cIs = a_cJarFileEntry.getJarFile().getInputStream(a_cJarFileEntry.getJarEntry());
            ByteArrayOutputStream cBos = new ByteArrayOutputStream();
            copyStreams(cIs, cBos);
            byte[] anBytes = cBos.toByteArray();
            cClass = defineClass(a_cJarFileEntry.getClassName(), anBytes, 0, anBytes.length);
        }
        catch (IOException ex) {
            cLogger.warn("", ex);
        }

        m_cMapClasses.put(a_cJarFileEntry.getClassName(), cClass);

        if (cLogger.isTraceEnabled())
            cLogger.trace("Loaded class data: " + a_cJarFileEntry.toString());

        return cClass;
    }



    private void copyStreams(InputStream a_cIn, OutputStream a_cOut) throws IOException {
        byte[] anBuf = new byte[1024];
        int nLen = 0;
        while ((nLen = a_cIn.read(anBuf)) >= 0) {
            a_cOut.write(anBuf, 0, nLen);
        }
    }



    @Override
    protected Class<?> loadClass(String a_cName, boolean a_bResolve) throws ClassNotFoundException {
        if (cLogger.isTraceEnabled())
            cLogger.trace("Loading class: " + a_cName);

        Class<?> cClass = null;
        try {
            cClass = getParent().loadClass(a_cName);
            return cClass;
        }
        catch (Exception e) {
            // ignore this
        }

        cClass = findClass(a_cName);
        if (cClass != null)
            resolveClass(cClass);

        return cClass;
    }
}