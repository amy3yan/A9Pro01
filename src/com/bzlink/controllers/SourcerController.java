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
package com.bzlink.controllers;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.bzlink.BaseController;
import com.bzlink.services.SourcerService;

import net.sf.json.JSONObject;

/**
 * 
 *
 * @author Amy
 */
@Controller
public class SourcerController extends BaseController {

	private static Logger log = Logger.getLogger(SourcerController.class);
	
	@Autowired
	private SourcerService service;
	
	@RequestMapping(value = "/sourcer/updateSourcers.do", method = RequestMethod.POST )
	public void updateSourcers(HttpServletRequest request, HttpServletResponse response) {
		PrintWriter writer = null;
        JSONObject jsonObject = new JSONObject();
        Connection conn = null;
        try {
        	MultipartFile file = ((MultipartHttpServletRequest)request).getFile("file");
            if(file.isEmpty()) return;
            
            List<Map<String, Object>> datas = service.readDatas(file.getInputStream());
            
            
            
            
            String filename = file.getOriginalFilename();//上传文件的名字
            
            conn = DatabaseUtils.getInstance().getConnection();
            log.debug("Connected to Database: " + conn.hashCode());
            writer = response.getWriter();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            String partNumber = request.getParameter("partno");
            jsonObject.put("data", maturityService.getPartMaturityDetailInfos(conn, partNumber));
            jsonObject.put("success", true);
        }catch(Exception e) {
            jsonObject.put("success", false);
            jsonObject.put("error", e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println(jsonObject.toString());
            if (writer != null) {
                writer.write(jsonObject.toString());
                writer.flush();
                writer.close();
            }
            if(conn != null) {
                try {
                    log.info("Closed Connection: " + conn.hashCode());
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	
}
