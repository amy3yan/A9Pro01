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
package com.bzlink.utils;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IQuery;
import com.bzlink.app.AConstant;
import com.xps.agile.utils.AgileUtils;

/**
 * 
 *
 * @author Amy
 */
public class BzHelper {
	
	private static Logger log = Logger.getLogger(BzHelper.class);
	
	public static List<Map<String, Object>> getItemsWithoutSiteSourcer(IAgileSession session) throws Exception{
		IQuery query = (IQuery) session.getObject(IQuery.OBJECT_TYPE, AConstant.QUERY_PATH);
		return AgileUtils.queryResults(query, new Object[] {});
	}

}
