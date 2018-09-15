package com.bzlink.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 系统相关的常量类
 * 
 * @author Amy
 */
@Component
public class SysConf
{  
    @Value("#{system.agile_url}")
    public String url;
    
    @Value("#{system.agile_user}")
    public String userName;
    
    @Value("#{system.agile_passd}")
    public String password;
    
    @Value("#{system.AGILE_SESSION_PREFIX}")
    public String AGILE_SESSION_PREFIX;
    
}