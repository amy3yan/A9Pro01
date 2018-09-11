/**
 * @(#)CellUtils.java        2015-4-17
 *
 * Copyright (c) 2015 Hand.
 * All right reserved.
 *
 * #none-javadoc
 */


package com.hand.utils;

import com.agile.api.APIException;
import com.agile.api.DataTypeConstants;
import com.agile.api.IAgileList;
import com.agile.api.ICell;

/**
 * 读取/存储 ICell 的值
 * 
 * @version 1.0 2009-8-28
 * @author fionn.peng
 */
public class CellUtils{

	/**
	 * 单列表cell属性
	 * 
	 * @param cell
	 * @return
	 * @throws APIException
	 */
	public static Object getSingleListCellValue(ICell cell) throws APIException{
		Object value = null;
		IAgileList list = (IAgileList) cell.getValue();
		IAgileList[] selected = list.getSelection();
		if(selected != null && selected.length > 0){
			value = selected[0].getValue();
		}
		return value;
	}

	/**
	 * 多列表cell属性
	 * 
	 * @param cell
	 * @return
	 * @throws APIException
	 */
	public static String getMultiListCellValue(ICell cell) throws APIException{
		String value = null;
		IAgileList agileList = (IAgileList) cell.getValue();
		value = agileList.toString();
		return value;
	}

	/**
	 * 非列表属性
	 * 
	 * @param cell
	 * @return
	 * @throws APIException
	 */
	public static Object getCellValue(ICell cell) throws APIException{
		Object value = null;
		value = cell.getValue();
		return value;
	}

	/**
	 * cell 的文本表示值
	 * 
	 * @param cell
	 * @return
	 * @throws APIException
	 */
	public static String getCellStringValue(ICell cell) throws APIException{
		if(cell == null)
			return "";
		Object value = null;
		switch(cell.getDataType()){
		case DataTypeConstants.TYPE_SINGLELIST:
			value = getSingleListCellValue(cell);
			break;
		case DataTypeConstants.TYPE_MULTILIST:
			value = getMultiListCellValue(cell);
			break;
		default:
			value = getCellValue(cell);
		}
		return value != null ? value.toString() : "";
	}

	/**
	 * 为List属性赋值
	 * @param cell
	 * @param newValues
	 * @throws APIException
	 */
	public static void setListCellValue(ICell cell, Object[] newValues) throws APIException {
	    IAgileList values = cell.getAvailableValues();
	    values.setSelection(newValues);
	    cell.setValue(values);
	}
}
