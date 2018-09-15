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
package com.bzlink;

import com.xps.agile.PXConstant;
import com.xps.agile.PXLogger;

/**
 * 
 *
 * @author Amy
 */
public class XConstant extends PXConstant {

	private static PXLogger log = PXLogger.getLogger(XConstant.class);
	
	
	
	public static void main(String[] args) {
		log.log(XConstant.AGILE_URL);
	}
	
	private static String get(String key) {
		
		return getValue(key);
	}
	
}
