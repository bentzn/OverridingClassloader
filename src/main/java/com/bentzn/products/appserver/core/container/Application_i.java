package com.bentzn.products.appserver.core.container;


/**
 * @author bentzn
 */
public interface Application_i extends Startable_i {

    /**
     * Initialize the application<br>
     * Executed before start
     * 
     * @param a_cServerContainer
     * @throws Exception
     */
    void init(ServerContainer_i a_cServerContainer) throws Exception;

}
