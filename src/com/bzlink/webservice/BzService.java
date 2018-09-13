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

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * @author Amy
 *
 */
@WebService
public interface BzService {

    @WebMethod
    public String submitPOChange(String name, String passwd, String xml) throws Exception;
    @WebMethod
    public String submitPRChange(String name, String passwd, String xml) throws Exception;
}
