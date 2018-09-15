/**
 * Copyright 2018 AMY. All Rights Reserved.
 * <p>
 * THE CODE IS MADE AVAILABLE "AS IS" WITHOUT WARRANTY OR SUPPORT OF ANY KIND. 
 * AMY OFFERS NO EXPRESS OR IMPLIED WARRANTIES AND SPECIFICALLY 
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, 
 * OR WARRANTY OF NON-INFRINGEMENT. IN NO EVENT SHALL AMY OR ITS 
 * LICENSORS BE LIABLE FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING LOST PROFITS OR SAVINGS) WHETHER BASED ON CONTRACT, TORT 
 * OR ANY OTHER LEGAL THEORY, EVEN IF AMY OR ITS LICENSORS HAVE BEEN 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * <p>
 */
package com.bzlink.app;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.xps.agile.PXConstant;

/**
 * 
 *
 * @author Amy
 */
public class AConstant extends PXConstant {
	
	private static Logger log = Logger.getLogger(AConstant.class);
	
	private static Map<String, String> mailconfig;
	public static String QUERY_PATH = get("query_path");
	
	public static String ITEM_SITE_SOURCER = get("item_site_sourcer_id");
	
	private static String get(String key) {
		loadConfig(new File("config/app.properties"));
		return getValue(key);
	}

	 public static Map<String, String> getMailConfig(){
	    	if(mailconfig == null) {
	    		mailconfig = new HashMap<>();
	    		mailconfig.put("server", MAIL_SERVER);
	    		mailconfig.put("user", MAIL_USER);
	    		mailconfig.put("password", MAIL_PASSWD);
	    		mailconfig.put("from", MAIL_FROM);
	    		mailconfig.put("to", get("sourcer_mail_to"));
	    	}
	    	return mailconfig;
	    }
}
