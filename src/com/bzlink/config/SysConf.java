package com.bzlink.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 系统相关的常量类
 * 
 * @author fionn
 * @version [版本号, 2015年11月28日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
@Component
public class SysConf
{  
    @Value("#{system.agile_url}")
    public String plmURL;
    
    @Value("#{system.agile_user}")
    public String userName;
    
    @Value("#{system.agile_password}")
    public String password;
    
    @Value("#{system.AGILE_SESSION_PREFIX}")
    public String AGILE_SESSION_PREFIX;
    
    
    /**
     * Datasheet文件标识的属性BASE ID
     */
    @Value("#{system.field_id}")
    public String DATASHEET_FIELD_ID;
    
    /**
     * Datasheet文件表示的属性值
     */
    @Value("#{system.field_value}")
    public String DATASHEET_FLAG;
    
    @Value("#{system.log_sql}")
    public String LOG_SQL;
    
    @Value("#{system.orcad_pool}")
    public String orcadPool;
}