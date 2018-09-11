/*******************************************************************************
 * @project: BYD-Div5-PX
 * @package: com.hand.agile.utils
 * @file: StringUtils.java
 * @author: fionn
 * @created: 2016年4月27日
 * @purpose:
 * 
 * @version: 1.0
 * 
 * 
 * Copyright 2016 HAND All rights reserved.
 ******************************************************************************/
package com.hand.utils;

import java.util.regex.Pattern;

/**
 * @author fionn
 *
 */
public class StringUtils {

    /**
     * 
     */
    public StringUtils() {
        // TODO Auto-generated constructor stub
    }

    /**
     * 若number全是数字，返回前面补0的指定位长字符串
     * SAP 物料编码若全是数字，要求前面用0补全指定位长度
     * @param matnr
     * @return
     */
    public static String getNumberByPrefix(String number, int length) {
        if(Pattern.matches("^\\d{1,}$", number) && number.length() < length) {
            StringBuffer number18 = new StringBuffer();
            for(int i=0; i<(length - number.length()); i++) {
                number18.append("0");
            }
            return number18.append(number).toString();
        }else {
            return number;
        }
    }
    
    /**
     * 补全后导0
     * @param number
     * @param length
     * @return
     */
    public static String getNumberBySuffix(String number, int length) {
        if(Pattern.matches("^\\d{1,}$", number) && number.length() < length) {
            StringBuffer number18 = new StringBuffer(number);
            for(int i=0; i<(length - number.length()); i++) {
                number18.append("0");
            }
            return number18.toString();
        }else {
            return number;
        }
    }

    /**
     * @desc: 返回有效可用于文件路径的字符串
     *
     * @param str
     * @return
     * @author: fionn
     */
    public static String getValidPathStr(String str) {
        return str != null ? str.replaceAll("[\\\\/:\\-?\"<>|]", "") : str;
    }
}
