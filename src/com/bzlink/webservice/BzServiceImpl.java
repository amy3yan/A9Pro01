/*******************************************************************************
 * @project: A9Pro01
 * @package: com.bzlink.webservice
 * @author: Amy
 * @created: 2018年9月13日
 * @purpose:
 * 
 * @version: 1.0
 * 
 * 
 * Copyright 2018 HAND All rights reserved.
 ******************************************************************************/
package com.bzlink.webservice;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.MTOM;

import org.apache.log4j.Logger;

/**
 * @author Amy
 *
 */
@MTOM
@WebService(endpointInterface = "com.bzlink.webservice.BzService")
public class BzServiceImpl implements BzService {

    private static Logger log = Logger.getLogger(BzServiceImpl.class);
    @Resource
    private WebServiceContext webServiceContext;
    
    
    /**
     * @desc: 
     *
     *
     */
    public BzServiceImpl() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see com.bzlink.webservice.BzService#submitPOChange(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String submitPOChange(String name, String passwd, String xml) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.bzlink.webservice.BzService#submitPRChange(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String submitPRChange(String name, String passwd, String xml) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
