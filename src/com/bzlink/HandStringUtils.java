package com.bzlink;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;

import com.agile.api.APIException;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.IUser;
import com.agile.api.UserConstants;
import com.bzlink.config.SysConf;

/**
 * 本质：内部类 和 单独文件中的类的区别 <br>
 * BeanUtils.populate方法的限制：<br>
 * The class must be public, and provide a public constructor that accepts no arguments. <br>
 * This allows tools and applications to dynamically create new instances of your bean, <br>
 * without necessarily knowing what Java class name will be used ahead of time
 */
public class HandStringUtils extends org.apache.commons.lang.StringUtils
{ 
    public static APIException getAPIException(String msgKey, String... values)
    {
        return new APIException(new Throwable(GlobalMessageInfo.getInstance().getInfo(msgKey, values)));
    } 
    
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
    
    public static InputStream toInputStream(byte[] file) throws IOException {
        InputStream input = new ByteArrayInputStream(file); 
        return input;
    }
    
    public static String getStr(Object obj)
    {
        return null == obj ? "" : obj.toString();
    }
    
    public static boolean isNullOrBlank(Object obj)
    {
        String str = getStr(obj);
        return "".equals(str);
    }
    
    public static String getParamSql(List<Object> param, Collection<String> values, List<Integer> types)
    {
        StringBuffer inStr = new StringBuffer();
        for(String value : values)
        {
            if(!"".equals(value))
            {
                inStr.append("?,");
                param.add(value); 
                types.add(Types.VARCHAR);
            } 
        }
        if(inStr.length() > 0)
        {
            inStr.deleteCharAt(inStr.lastIndexOf(","));
        }
        return inStr.toString();
    }
    
    public static class MapKeyComparator implements Comparator<String>{
        @Override
        public int compare(String str1, String str2) {
            return str1.compareTo(str2);
        }
    }
    
    public static Map sortMapByKey(Map map) {
        if (map == null || map.isEmpty()) {
            return new HashMap();
        }
        Map sortMap = new TreeMap(new MapKeyComparator()); 
        sortMap.putAll(map);
        return sortMap;
    }
    
    public static String getParamSql(List<Object> param, Collection<String> values)
    {
        StringBuffer inStr = new StringBuffer();
        for(String value : values)
        {
            if(!"".equals(value))
            {
                inStr.append("?,");
                param.add(value); 
            } 
        }
        if(inStr.length() > 0)
        {
            inStr.deleteCharAt(inStr.lastIndexOf(","));
        }
        return inStr.toString();
    }
     
    public static String filterImg(String key)
    {  
        String match = "<img.*?>";
        Pattern pat = Pattern.compile(match);
        Matcher mat = pat.matcher(key); 
        StringBuffer sb = new StringBuffer();
        while(mat.find())
        {
            String value = mat.group();
            value = ""; 
            mat.appendReplacement(sb, value);
        }
        mat.appendTail(sb); 
        return sb.toString();
    }
    
    public static String getWeekStr(String week)
    {
        String value = "";
        switch (week)
        {
            case "0":
                value = "星期天";
                break;
            case "1":
                value = "星期一";
                break;
            case "2":
                value = "星期二";
                break;
            case "3":
                value = "星期三";
                break;
            case "4":
                value = "星期四";
                break;
            case "5":
                value = "星期五";
                break;
            case "6":
                value = "星期六";
                break;
            
            default:
                break;
        }
        return value;
    }
    
    public static int getRandomNumber()
    {
        Random random = new Random();  
        int value = random.nextInt(899999); 
        value = value + 100000;
        return value;
    }
    
    public static String getNewRevOfChange(String oldRev, String subType)
    { 
        String newRev = "";
        if("".equals(oldRev) || "初始".equals(oldRev))
        {
            switch (subType)
            {
                case "Project":
                    newRev = "PJ-1";
                    break;
                case "ACT_0001":
                    newRev = "T1-1";
                    break;  
                default:
                    break;
            }
        }
        else
        {
            int index = oldRev.indexOf("-");
            String head = oldRev.substring(0, index+1);
            String tail = oldRev.substring(index+1);
            int rev = Integer.parseInt(tail);
            rev++;
            newRev = head + rev;
        }
        return newRev;
    }
    
    /** 
     * <查询前一个版本>
     * @param rev
     * @return
     * @author huangtao
     * @date 2016年5月2日 上午11:08:16
     */
    public static String getBeforeRev(String rev)
    {
        String oldRev = "";
        if(!"初始".equals(rev)) 
        {
            int index = rev.indexOf("-");
            String head = rev.substring(0, index+1);
            String tail = rev.substring(index+1);
            int number = Integer.parseInt(tail);
            if(number == 1)
            {
                oldRev = "初始";
            }
            else
            {
                number--;
                oldRev = head + number;
            } 
        }
        return oldRev;
    }
    
    public static double getDouble(Double d)
    {
        double value = 0.0;
        if(null != d)
        {
            value = d;
        }
        return value;
    }
    
   // private static DecimalFormat df = new DecimalFormat("#.##"); 
    public static double getDoubleValue(Object obj)
    {
        String str = getStr(obj);
        if("".equals(str) || "null".equals(str))
        {
            str = "0";
        }
        double d = 0.0;
        try
        {
            d = Double.parseDouble(str);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        } 
        return d;
    }
    
    public static int getIntValue(Object obj)
    {
        String str = getStr(obj);
        int value = 0;
        if(!"".equals(str))
        {
            value = Integer.parseInt(str);
        }
        return value;
    } 
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMdd");
    public static Date getDate(String dateStr)
    {
        try
        {
            if (!"".equals(dateStr))
            {
                return format.parse(dateStr);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        } 
        return null;
    }
    
    private static SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static Date getDateTime(String dateStr)
    {
        try
        {
            if (!"".equals(dateStr))
            {
                return sim.parse(dateStr);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        } 
        return null;
    }
    
    public static String cutDateStr(Object dateStr)
    {
        String str = getStr(dateStr);
        if (str.contains(" "))
        {
            str = str.substring(0, str.indexOf(" "));
        }
        return str;
    }
    
    public static String getDateFormat2(Date date)
    {
        try
        {
            if (null != date)
            {
                return format2.format(date);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        } 
        return "";
    }
    
    public static String getDateStr(Date date)
    {
        try
        {
            if (null != date)
            {
                return format.format(date);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        } 
        return "";
    }
    
    // Map --> Bean 2: 利用org.apache.commons.beanutils 工具类实现 Map --> Bean
    public static void transMap2Bean2(Map<String, Object> map, Object obj)
    {
        if (map == null || obj == null)
        {
            return;
        }
        try
        {
            BeanUtils.populate(obj, map);
        }
        catch (Exception e)
        {
            System.out.println("transMap2Bean2 Error " + e);
        }
    }
    
    // Map --> Bean 1: 利用Introspector,PropertyDescriptor实现 Map --> Bean
    public static void transMap2Bean(Map<String, Object> map, Object obj)
    {
        
        try
        {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            
            for (PropertyDescriptor property : propertyDescriptors)
            {
                String key = property.getName();
                
                if (map.containsKey(key))
                {
                    Object value = map.get(key);
                    // 得到property对应的setter方法
                    Method setter = property.getWriteMethod();
                    setter.invoke(obj, value);
                }
                
            }
            
        }
        catch (Exception e)
        {
            System.out.println("transMap2Bean Error " + e);
        }
        
        return;
        
    }
    
    // Bean --> Map 1: 利用Introspector和PropertyDescriptor 将Bean --> Map
    public static Map<String, Object> transBean2Map(Object obj)
    {
        
        if (obj == null)
        {
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        try
        {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors)
            {
                String key = property.getName();
                
                // 过滤class属性
                if (!key.equals("class"))
                {
                    // 得到property对应的getter方法
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);
                    
                    map.put(key, value);
                }
                
            }
        }
        catch (Exception e)
        {
            System.out.println("transBean2Map Error " + e);
        }
        return map;
        
    }
    
    /**
     * 去除字符串首尾出现的某个字符.
     * 
     * @param source 源字符串.
     * @param element 需要去除的字符.
     * @return String.
     */
    public static String trimFirstAndLastChar(String source, char element)
    {
        boolean beginIndexFlag = true;
        boolean endIndexFlag = true;
        do
        {
            int beginIndex = source.indexOf(element) == 0 ? 1 : 0;
            int endIndex =
                source.lastIndexOf(element) + 1 == source.length() ? source.lastIndexOf(element) : source.length();
            source = source.substring(beginIndex, endIndex);
            beginIndexFlag = (source.indexOf(element) == 0);
            endIndexFlag = (source.lastIndexOf(element) + 1 == source.length());
        } while (beginIndexFlag || endIndexFlag);
        return source;
    }
    /**
     * 
     * 特殊字符的转换
     * @param source
     * @param format
     * @return
     * @throws UnsupportedEncodingException
     * @author zhangguoli
     * @date 2016年3月29日 下午10:51:35
     */
    public static String encode(String source, String format) throws UnsupportedEncodingException
    {
      String ts =source.replace("+", "%2B")
                .replace(" ", "%20")
                .replace("/", "%2F");
      return ts;
    }

    /**
     * 判断是否为数字
     * @param str 传入的字符串   
     * @return 是整数返回true,否则返回false
     * @author zhangguoli
     * @date 2016年5月10日 上午9:33:04
     */
    public static boolean isInteger(Object obj){
        String str = getStr(obj);
        if("".equals(str))
        {
            return false;
        }
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]+$");
        return pattern.matcher(str).matches();
    } 
    
    public static boolean isDouble(Object obj) {  
        String str = getStr(obj);
        if("".equals(str))
        {
            return false;
        }
        Pattern pattern = Pattern.compile("^[-+]?[0-9]+[.]?[0-9]*$"); 
        return pattern.matcher(str).matches();
      } 
    
    /**
     * 某一个月第一天和最后一天
     * @param date
     * @return
     */
    private static Map<String, String> getFirstday_Lastday_Month(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, -1);
        Date theDate = calendar.getTime();
        
        //上个月第一天
        GregorianCalendar gcLast = (GregorianCalendar) Calendar.getInstance();
        gcLast.setTime(theDate);
        gcLast.set(Calendar.DAY_OF_MONTH, 1);
        String day_first = df.format(gcLast.getTime());
        StringBuffer str = new StringBuffer().append(day_first).append(" 00:00:00");
        day_first = str.toString();

        //上个月最后一天
        calendar.add(Calendar.MONTH, 1);    //加一个月
        calendar.set(Calendar.DATE, 1);        //设置为该月第一天
        calendar.add(Calendar.DATE, -1);    //再减一天即为上个月最后一天
        String day_last = df.format(calendar.getTime());
        StringBuffer endStr = new StringBuffer().append(day_last).append(" 23:59:59");
        day_last = endStr.toString();

        Map<String, String> map = new HashMap<String, String>();
        map.put("first", day_first);
        map.put("last", day_last);
        return map;
    }

    /**
     * 当月第一天
     * @return
     */
    private static String getFirstDay() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        Date theDate = calendar.getTime();
        
        GregorianCalendar gcLast = (GregorianCalendar) Calendar.getInstance();
        gcLast.setTime(theDate);
        gcLast.set(Calendar.DAY_OF_MONTH, 1);
        String day_first = df.format(gcLast.getTime());
        StringBuffer str = new StringBuffer().append(day_first).append(" 00:00:00");
        return str.toString();

    }
    
    /**
     * 当月最后一天
     * @return
     */
    private static String getLastDay() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        Date theDate = calendar.getTime();
        String s = df.format(theDate);
        StringBuffer str = new StringBuffer().append(s).append(" 23:59:59");
        return str.toString();

    }

}