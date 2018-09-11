/*******************************************************************************
 * @project: BYD-Div5-PX
 * @package: com.hand.agile.sap
 * @file: JcoPayload.java
 * @author: fionn
 * @created: 2016年3月31日
 * @purpose:
 * 
 * @version: 1.0
 * 
 * 
 * Copyright 2016 HAND All rights reserved.
 ******************************************************************************/
package com.hand.sap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author fionn
 *
 */
public class JcoPayload {

    private static Logger log = Logger.getLogger(JcoPayload.class);
    private Set<String> tables;
    private Set<String> structures;
    private Map<String, Object>  paramValues;
    private Map<String, List<Map<String, Object>>>  tableValues;
    private Map<String, Map<String, Object>>  structureValues;
    /**
     * 
     */
    public JcoPayload() {
        tables = new HashSet<String>();
        structures = new HashSet<String>();
        paramValues = new HashMap<String, Object>();
        tableValues = new HashMap<String, List<Map<String, Object>>>();
        structureValues = new HashMap<String, Map<String,Object>>();
    }
    
    /**
     * 设置 ParameterList 承载属性值
     * @param key
     * @param value1
     */
    public void addParamValue(String key, Object value) {
        paramValues.put(key, value);
    }
    
    /**
     * 批量设置 ParameterList 承载属性值
     * @param values
     */
    public void addParamValues(Map<String, Object> values) {
        if(!values.isEmpty()) paramValues.putAll(values);
    }
    
    /**
     * 增加Table定义
     * @param table
     */
    public void addTable(String table) {
        tables.add(table);
        this.addTableValue(table, Collections.EMPTY_MAP);
    }
    
    /**
     * 设置 Table 承载属性值
     * @param table
     * @param value
     */
    public void addTableValue(String table, Map<String, Object> value) {
        List<Map<String, Object>> tblValues = tableValues.get(table);
        if(tblValues == null) {
            if(!tables.contains(table)) tables.add(table);
            tblValues = new ArrayList<Map<String,Object>>();
            tableValues.put(table, tblValues);
        }
        if(!value.isEmpty()) tblValues.add(value);
    }
    
    /**
     * 增加 Structure 定义
     * @param structure
     */
    public void addStructure(String structure) {
        this.structures.add(structure);
        addStructureValues(structure, new HashMap<String, Object>());
    }
    
    /**
     * 删除 structure 及其对应的数据
     * @param structrue
     */
    public void removeStructure(String structrue) {
        if(this.structures.remove(structrue)) {
            structureValues.remove(structrue);
        }
    }
    
    /**
     * @param structure
     * @param values
     */
    public void addStructureValues(String structure, Map<String, Object> values) {
        Map<String, Object> struValues = this.structureValues.get(structure);
        if(struValues == null) {
            if(!structures.contains(structure)) structures.add(structure);
            struValues = new HashMap<String, Object>();
            structureValues.put(structure, struValues);
        }
        if(!values.isEmpty()) struValues.putAll(values);
    }
    
    /**
     * 设置 Structure 承载属性值
     * @param key
     * @param value
     */
    public void addStructureValue(String structure, String key, Object value) {
        Map<String, Object> struValues = this.structureValues.get(structure);
        if(struValues == null) {
            if(!structures.contains(structure)) structures.add(structure);
            struValues = new HashMap<String, Object>();
            structureValues.put(structure, struValues);
        }
        struValues.put(key, value);
    }
    
    /**
     * 根据key 获取 Param 值 
     * @param key
     * @return
     */
    public Object getParamValue(String key) {
        return paramValues.get(key);
    }
    
    /**
     * 根据key 获取 structure 值 
     * @param structrue
     * @param key
     * @return
     */
    public Object getStructureValue(String structrue, String key) {
        Map<String, Object> structureValues = this.structureValues.get(structrue);
        return structureValues != null ? structureValues.get(key) : null;
    }
    
    /**
     * 根据tblname获取Table
     * @param tblname
     * @return
     */
    public List<Map<String, Object>> getTable(String tblname){
        return this.tableValues.get(tblname);
    }
    
    /**
     * @return the tables
     */
    public Set<String> getTables() {
        return tables;
    }
    /**
     * @param tables the tables to set
     */
    public void setTables(Set<String> table) {
        this.tables = table;
    }
    /**
     * @return the structures
     */
    public Set<String> getStructures() {
        return structures;
    }
    /**
     * @param structure the structure to set
     */
    public void setStructures(Set<String> structures) {
        this.structures = structures;
    }
    /**
     * @return the paramValues
     */
    public Map<String, Object> getParamValues() {
        return paramValues;
    }
    /**
     * @param paramValues the paramValues to set
     */
    public void setParamValues(Map<String, Object> paramValues) {
        this.paramValues = paramValues;
    }
    /**
     * @return the tableValues
     */
    public Map<String, List<Map<String, Object>>> getTableValues() {
        return tableValues;
    }
    /**
     * @param tableValues the tableValues to set
     */
    public void setTableValues(Map<String, List<Map<String, Object>>> tableValues) {
        this.tableValues = tableValues;
    }
    /**
     * @return the structureValues
     */
    public Map<String, Map<String, Object>> getStructureValues() {
        return structureValues;
    }
    /**
     * @param structureValues the structureValues to set
     */
    public void setStructureValues(Map<String, Map<String, Object>> structureValues) {
        this.structureValues = structureValues;
    }

    
}
