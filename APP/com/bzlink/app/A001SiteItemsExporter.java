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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.agile.api.IAgileSession;
import com.agile.api.ItemConstants;
import com.bzlink.utils.BzHelper;
import com.xps.agile.utils.AgileUtils;
import com.xps.utils.ExcelUtils;
import com.xps.utils.MailUtils;

/**
 * 每天运行一遍，将工厂为1120/1100中无Sourcer的新物料导出成Excel文件，并发送邮件给指定的人
 *
 * @author Amy
 */
public class A001SiteItemsExporter implements Runnable {
	
	private static Logger log = Logger.getLogger(A001SiteItemsExporter.class);
	private IAgileSession session;
	private List<Map<String, Object>> datas;
//	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		// TODO Auto-generated method stub
		ByteArrayOutputStream oStream = null;
		try {
			login();
			this.datas = BzHelper.getItemsWithoutSiteSourcer(session);
			
			oStream = new ByteArrayOutputStream();
			generateExcel(oStream);
			
			InputStream iStream = new ByteArrayInputStream(oStream.toByteArray());
			mail(iStream);
			
			log.debug("程序执行完成");
		}catch(Exception e) {
			log.error(e.getMessage(), e);
		}finally {
			logout();
			if(oStream != null) {
				try {
					oStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void generateExcel(OutputStream excelOStream) throws Exception{
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Data");
		/** 表头 **/
		Row row = sheet.createRow(0);
		String[] heads = new String[] {"Part Number", "Description", "Revision", "Part Type", "Manu", "Sourcer"};
		ExcelUtils.writeRow(row, heads);
		
		for(Map<String, Object> data : datas) {
			Object[] val = new Object[] {
					data.get(ItemConstants.ATT_TITLE_BLOCK_NUMBER.toString())
					, data.get(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION.toString())
					, data.get(ItemConstants.ATT_TITLE_BLOCK_REV.toString())
					, data.get(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE.toString())
					, data.get(ItemConstants.ATT_SITES_SITE_NAME.toString())
					, data.get(AConstant.ITEM_SITE_SOURCER.toString())
			};
			ExcelUtils.appendRow(sheet, val);
		}
		
		workbook.write(excelOStream);
		try {
			workbook.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void mail(InputStream iStream) throws Exception{
		log.debug("发送邮件." + AConstant.getMailConfig());
		String subject = "1120/1100工厂无Sourcer物料清单 " + new SimpleDateFormat("yyyyMMdd").format(new Date());
		String content = "详情见附件";
		// 附件
		Map<String, Object> attachments = new HashMap<>();
		attachments.put("", iStream);
		MailUtils.send(AConstant.getMailConfig(), subject, content, attachments);
	}
	
	private void login() {
		log.debug("connect agile server " + AConstant.AGILE_URL);
		session = AgileUtils.createAgileSession(AConstant.AGILE_URL, AConstant.AGILE_USER, AConstant.AGILE_PASSWD);
		log.debug("Connected " + session.hashCode());
	}
	
	private void logout() {
		if(session == null) return ;
		log.debug("disconnect agile server " + session.hashCode());
		session.close();
	}
	
	/**
	 * @param args
	 *
	 * @author Amy
	 */
	public static void main(String[] args) {
		log.debug(" 开始导出1120/1100工厂无Sourcer的新物料...");
		new Thread(new A001SiteItemsExporter()).start();
	}
	
	
	

}
