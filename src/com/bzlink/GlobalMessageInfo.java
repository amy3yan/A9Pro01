package com.bzlink;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.xps.utils.StringUtils;
  
public class GlobalMessageInfo
{ 
    private static GlobalMessageInfo instance = null;
    private static MessageSource source =  (MessageSource)SpringContextUtil.getBean("messageSource");
    
    public static GlobalMessageInfo getInstance()
    {
        if(null == instance)
        {
            instance = new GlobalMessageInfo();
        }
        return instance;
    }
    
    private GlobalMessageInfo()
    {
        
    }
    
    public String getInfo(String key, String... objs)
    {
        Locale locale = Locale.CHINESE;
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        String language = StringUtils.getStr(session.getAttribute("language"));
        if(language.equals("English"))
        {
            locale = Locale.ENGLISH;
        }
        return source.getMessage(key, objs, locale);
    }
}
