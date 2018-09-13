package com.bzlink;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.bzlink.config.SysConf;

/**
 * 
 * <功能描述>
 * 
 * @author Amy
 * @date 2018.9
 */

@Controller
public class BaseController {
    protected final Logger log = Logger.getLogger(this.getClass());
    @Autowired
    private SysConf sysConf;

    /**
     * 
     * 提取session中的AgileSession
     * 
     * @param request
     * @return
     */
    protected IAgileSession getAgileSession(HttpServletRequest request) throws APIException {
        Cookie[] cookies = request.getCookies();
        String username = sysConf.userName;
        if (null != cookies) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals("j_username")) {
                    username = cookies[i].getValue();
                    log.info("---cookies---username:" + username);
                    break;
                }
            }
        }

        IAgileSession session = (IAgileSession) request.getSession().getAttribute(sysConf.AGILE_SESSION_PREFIX + username);
        if (null == session) { throw new APIException(new Throwable(GlobalMessageInfo.getInstance().getInfo("msg.session_expiry"))); }
        return session;
    }

    protected String getErrorMsg(Throwable e) {
        JSONObject jsonObj = new JSONObject();
        Map<String, Object> error = new HashMap<String, Object>();
        if (e instanceof APIException) {
            APIException ex = (APIException) e;
            if (null != ex.getRootCause()) {
                error.put("msg", ex.getRootCause().getMessage());
            } else {
                error.put("msg", ex.getMessage());
            }
        } else {
            error.put("msg", e.toString());
        }
        jsonObj.put("success", false);
        jsonObj.put("error", error);
        return jsonObj.toString();
    }

    /**
     * 
     * 根据Key值移除Cookie
     * 
     * @param request
     * @param response
     * @param key
     */
    protected void removeCookies(HttpServletRequest request, HttpServletResponse response, String key) {
        log.info("刪除 Cookies");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                String name = cookies[i].getName();
                if (name.equals(key)) {
                    cookies[i].setMaxAge(0);
                    response.addCookie(cookies[i]);
                }
            }
        }
    }

    /**
     * 
     * 根据key 获取cookie的值
     * 
     * @param request
     * @param key
     * @return
     */
    protected String getCookie(HttpServletRequest request, String key) {
        log.info("读Cookie");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookie.getName().equals(key)) { return cookie.getValue(); }
            }
        }
        return null;
    }

    /**
     * 
     * 写入cookies
     * 
     * @param request
     * @param response
     * @param key
     * @param value
     */
    protected void addCookies(HttpServletRequest request, HttpServletResponse response, String key, String value) {
        Cookie cookie = new Cookie(key, value);
        // 可以让Cookie文件在一年内有效。
        cookie.setMaxAge(365 * 24 * 60 * 60);
        // cookie.setPath(this.COOKIES_SAVE_PATH);
        response.addCookie(cookie);
    }

    /**
     * 
     * 返回json消息数据，供前台接收
     * 
     * @param success
     * @param title
     * @param content
     * @return
     */
    protected String createJsonMsg(Boolean success, String title, String content) {
        JSONObject jObject = new JSONObject();
        try {
            jObject.put("success", success);
            jObject.put("title", title);
            jObject.put("data", content);
        } catch (Exception e) {
            return "{success:false,title:\"System error\",content:\"Create message error,method is BaseController.createJsonHtml.\"}";
        }
        return jObject.toString();
    }

    /**
     * 
     * 检测权限
     * 
     * @param request
     * @param accessLevel
     * @return
     */
    // protected boolean checkUserLevel(HttpServletRequest request, int accessLevel)
    // {
    //
    // }

    protected Map<String, String> getAllParams(HttpServletRequest request) {
        Map<String, String> map = new HashMap<String, String>();
        Enumeration<String> keys = request.getParameterNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            map.put(key, StringUtils.getStr(request.getParameter(key)));
        }
        return map;
    }

    /**
     * 
     * 填写消息
     * 
     * @param request
     * @param response
     * @param msg
     */
    protected void writeMsg(HttpServletRequest request, HttpServletResponse response, String msg) {
        try {
            request.getRequestDispatcher("/noCache.jsp").include(request, response);
            StringBuffer buffer = new StringBuffer("<html>\n<head>\n<title>Message:</title></head><body>");
            buffer.append(msg);
            buffer.append("\n</body>\n</html>");
            response.getWriter().println(buffer.toString());
        } catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
