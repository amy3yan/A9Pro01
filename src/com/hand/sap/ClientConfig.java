/*******************************************************************************
 * @project: BYD-Div5-PX
 * @package: com.hand.agile.sap
 * @file: ClientConfig.java
 * @author: fionn
 * @created: 2016年3月31日
 * @purpose:
 * 
 * @version: 1.0
 * 
 * 
 * Copyright 2016 HAND All rights reserved.
 ******************************************************************************/
package com.hand.sap;

import java.util.HashMap;
import java.util.Map;


/**
 * @author fionn
 *
 */
public class ClientConfig {

    public static String DEFAULT_POOL_NAME = "P01";
    public static String DEFAULT_CLIENT_NAME = "000";
    public static int DEFAULT_MAX_NUM = 10;
    private String username;
    private String password;
    private String lang;
    private String host;
    private String sysnum;
    private String client;
    private String pool;
    private int maxnum;
    /**
     * 
     */
    public ClientConfig() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }
    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    /**
     * @return the sysnum
     */
    public String getSysnum() {
        return sysnum;
    }
    /**
     * @param sysnum the sysnum to set
     */
    public void setSysnum(String sysnum) {
        this.sysnum = sysnum;
    }
    /**
     * @return the client
     */
    public String getClient() {
        return client;
    }
    /**
     * @param client the client to set
     */
    public void setClient(String client) {
        this.client = client;
    }
    /**
     * @return the pool
     */
    public String getPool() {
        return pool;
    }
    /**
     * @param pool the pool to set
     */
    public void setPool(String pool) {
        this.pool = pool;
    }
    /**
     * @return the maxnum
     */
    public int getMaxnum() {
        return maxnum;
    }
    /**
     * @param maxnum the maxnum to set
     */
    public void setMaxnum(int maxnum) {
        this.maxnum = maxnum;
    }
    /**
     * @return the lang
     */
    public String getLang() {
        return lang;
    }
    /**
     * @param lang the lang to set
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // TODO Auto-generated method stub
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("HOST", host);
        map.put("CLIENT", client);
        map.put("POOL", pool);
        map.put("LANG", lang);
        map.put("SYSNUM", sysnum);
        map.put("MAXNUM", maxnum);
        map.put("USERNAME", username);
        map.put("PASSWORD", password);
        return map.toString();
    }
    

}
