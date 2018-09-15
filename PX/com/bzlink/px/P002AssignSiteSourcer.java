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
package com.bzlink.px;

import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.IAgileSession;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.ItemConstants;
import com.agile.px.ActionResult;
import com.agile.px.EventConstants;
import com.agile.px.IChangeAppObserverEventInfo;
import com.xps.agile.px.BaseObjectPXAction;
import com.xps.agile.utils.CellUtils;

/**
 * 
 *
 * @author Amy
 */
public class P002AssignSiteSourcer extends BaseObjectPXAction {
	
	private static Logger log = Logger.getLogger(P002AssignSiteSourcer.class);
	private static final String c_site = "1200";
	private static final int site_sourcer = 1001;

	/* (non-Javadoc)
	 * @see com.xps.agile.px.BaseObjectPXAction#doAction(com.agile.api.IAgileSession, com.agile.api.IDataObject)
	 */
	@Override
	protected ActionResult doAction(IAgileSession session, IDataObject agileobj) {
		// TODO Auto-generated method stub
		ActionResult result = new ActionResult(ActionResult.NORESULT, "");
		
		try {
			IChangeAppObserverEventInfo info = (IChangeAppObserverEventInfo) eventInfo;
			if(EventConstants.WORKFLOW_CHANGE_APPROVER_OR_OBSERVER_ACTION_ADD == info.getAction()) return result; 
			IChange change = (IChange) agileobj;
			IUser reviewer = (IUser)info.getApprovers()[0].getReviewer();
			log.debug(change + " assign the sourcer " + reviewer + " for site 1200.");
			ITable aitbl = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
			ITwoWayIterator aiit = aitbl.getReferentIterator();
			while(aiit.hasNext()) {
				IItem item = (IItem) aiit.next();
				assignSiteSourcer(item, reviewer, c_site);
			}
			result = new ActionResult(ActionResult.STRING, " Assign " + reviewer + " as the sourcer for 1200");
		}catch (Exception e) {
			result = new ActionResult(ActionResult.EXCEPTION, e);
			log.error(e);
		}
		return result;
	}

	private void assignSiteSourcer(IItem item, IUser user, String site) throws APIException {
		
		ITable stbl = item.getTable(ItemConstants.TABLE_SITES);
		ITwoWayIterator sit = stbl.getTableIterator();
		while(sit.hasNext()) {
			IRow row = (IRow)sit.next();
			if(!site.equals(CellUtils.getStringByBaseID(row, ItemConstants.ATT_SITES_SITE_NAME))) continue;
			CellUtils.setListCellValue(row.getCell(site_sourcer), new IUser[] {user});
			log.debug(item + " with sit 1200 is assigned a sourcer " + user);
		}
		
	}
}
