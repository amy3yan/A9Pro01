/**
 * 
 */
package com.hand.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.IAdmin;
import com.agile.api.IAgileClass;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.IAutoNumber;

/**
 * @author fionn
 *
 */
public class AgileUtils {

	private static Logger log = Logger.getLogger(AgileUtils.class);
	
	public static IAgileSession createAgileSession(String url, String user, String passwd) {
		try {
			HashMap<Integer, Object> maps = new HashMap<Integer, Object>();
			maps.put(AgileSessionFactory.USERNAME, user);
			maps.put(AgileSessionFactory.PASSWORD, passwd);
			AgileSessionFactory sessionFactory = AgileSessionFactory.getInstance(url);
			IAgileSession session = sessionFactory.createSession(maps);
			return session;
		} catch (APIException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * @param className
	 * @param session
	 * @return
	 * @throws APIException
	 */
	public static String getOneAutoNumber(String className, IAgileSession session) throws APIException {
	   String number = null;
	   IAdmin admin = session.getAdminInstance();
	   IAgileClass a9class = admin.getAgileClass(className);
	   IAutoNumber[] ans = a9class.getAutoNumberSources();
	   if(ans.length > 0) {
	       number = ans[0].getNextNumber();
	   }
	   return number;
	}
	
	/**
	 * 将Agile取得的值转为字符串
	 * @param obj
	 * @return
	 * @throws APIException 
	 */
	public static String getAgileValue(Object obj) throws APIException {
	    if (obj == null) return null;
	    // 列表值
        if (obj instanceof IAgileList) {
            return getListValue(obj);
        } else if (obj instanceof Date) {// 日期，转换为本地日期
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            format.setTimeZone(TimeZone.getTimeZone("GMT+8"));//本地时间
            String s = format.format(obj);
            log.info("date1: "+s);
            return s;
        } else if(obj instanceof Double){
            String val = String.valueOf(obj);
            if(val.endsWith(".0")) val = val.substring(0, val.indexOf("."));
            return val;
        } else { // 其他 直接转换成字符串
            return String.valueOf(obj);
        }
	}
	
	/**
	 * Agile 中列表值转换成字符串， 分多列表和单列表处理
	 * @param object
	 * @return
	 * @throws APIException
	 */
	public static String getListValue(Object object) throws APIException {
	    String value = null;
	    if(object == null) return value;
        IAgileList list = (IAgileList) object;
        IAgileList[] selected = list.getSelection();
        if(selected == null || selected.length < 1) return null;
        // cascadeList 多列表，直接返回字符串即可
        if (selected[0] instanceof IAgileList) {
            value = String.valueOf(list);
        } else { // 单列表
            value = (String)(selected[0].getValue());
        }
        return value;
    }
	
}
