package com.bentzn.products.appserver.core.util.classloader;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import com.bentzn.products.appserver.core.annotations.PublishedService;
import com.bentzn.products.appserver.core.container.Application_i;

/**
 * @author bentzn
 */
public class TestOverridingClassloader {

    public static void main(String[] args) throws Exception {
        new TestOverridingClassloader().testClassloader();
    }


    @Test
    public void testClassloader() throws Exception {
        File cFileProduct = new File("./src/test/java/com/bentzn/products/appserver/core/util/classloader/product");
        if (!cFileProduct.exists())
            throw new RuntimeException("Product jar does not exist");

        File cFileProject = new File("./src/test/java/com/bentzn/products/appserver/core/util/classloader/project");
        if (!cFileProject.exists())
            throw new RuntimeException("Project jar does not exist");

        OverridingClassloader cClassloader = new OverridingClassloader(
                TestOverridingClassloader.class.getClassLoader(), cFileProduct, cFileProject);
        List<Class<?>> cLst = cClassloader.getLoadedClasses();
        assertTrue(cLst.size() > 0);

        boolean bApplicationStarted = false;
        for (Class<?> cClass : cLst) {
            // System.out.println(cClass);
            try {
                Object cInstance = cClass.newInstance();
                if (cInstance instanceof Application_i) {
                    ((Application_i) cInstance).start();
                    bApplicationStarted = true;
                }
                else if (cClass.getAnnotation(PublishedService.class) != null) {
                    // System.out.println(">>>" + cClass);
                }
            }
            catch (Throwable e) {
                // nothing
            }
        }

        assertTrue(bApplicationStarted);

        // load a resource
        URL cUrl = cClassloader.getResource("META-INF/some_setup.xml");

        assertTrue(cUrl != null);
        assertTrue(cUrl.toString().contains("classloading_project"));
    }
}
