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
package com.bzlink.services;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.agile.api.IAgileSession;

/**
 * 
 *
 * @author Amy
 */
@Service
public interface SourcerService {
	
	/**
	 * Import item's sourcer which without sourcer, and create change to release them.
	 * @param session
	 * @param records
	 * @return
	 * @throws Exception
	 *
	 * @author Amy
	 */
	public String updateItemSiteSourcers(IAgileSession session, List<Map<String, String>> records) throws Exception;

	/**
	 * when sourcer changed, update the item with change.
	 * @param session
	 * @param records
	 * @return
	 * @throws Exception
	 *
	 * @author Amy
	 */
	public String updateItemSiteWhenSourcersChanged(IAgileSession session, List<Map<String, String>> records) throws Exception;
	
	/**
	 * read datas from excel inputstream.
	 * @param excel
	 * @return
	 * @throws Exception
	 *
	 * @author Amy
	 */
	public List<Map<String, Object>> readDatas(InputStream excel) throws Exception;
}
