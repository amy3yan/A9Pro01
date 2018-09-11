/*******************************************************************************
 * @project: BYD-Div5-PX
 * @package: com.hand.agile.sap
 * @file: SAPAdapter.java
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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.sap.mw.jco.IRepository;
import com.sap.mw.jco.JCO;

/**
 * @author fionn
 *
 */
public class SAPAdapter {

    private static Logger log = Logger.getLogger(SAPAdapter.class);
    private JCO.Client client;
    private IRepository repository;
    private ClientConfig config;
    /**
     * 
     */
    public SAPAdapter() {
        // TODO Auto-generated constructor stub
    }

    /**
     * 连接 SAP， 并初始化相关对象
     * @param user
     * @param passwd
     * @param language
     * @param host
     * @param systemNumber
     */
    public void connect(String user, String passwd, String lang, String host, String sysnr) throws JCO.Exception {
        config = new ClientConfig();
        config.setPool(ClientConfig.DEFAULT_POOL_NAME);
        config.setClient(ClientConfig.DEFAULT_CLIENT_NAME);
        config.setMaxnum(ClientConfig.DEFAULT_MAX_NUM);
        config.setUsername(user);
        config.setPassword(passwd);
        config.setLang(lang);
        config.setHost(host);
        config.setSysnum(sysnr);
        connect(config);
//        JCO.addClientPool(ClientConfig.POOL_NAME, MAX_NUM, ClientConfig.CLIENT_NAME, user, passwd, lang, host, sysnr);
//        this.repository = JCO.createRepository("SAPRepository", ClientConfig.POOL_NAME);
//        this.client = JCO.getClient(ClientConfig.POOL_NAME);
    }
    
    /**
     * 连接 SAP， 并初始化相关对象
     * @param user
     * @param passwd
     * @param language
     * @param host
     * @param systemNumber
     */
    public void connect(ClientConfig config) throws JCO.Exception {
        this.config = config;
        log.debug(config);
        JCO.addClientPool(config.getPool(), config.getMaxnum(), 
                config.getClient(), config.getUsername(), config.getPassword(), 
                config.getLang(), config.getHost(), config.getSysnum());
        this.repository = JCO.createRepository("SAPRepository", config.getPool());
        this.client = JCO.getClient(config.getPool());
    }
    
    /**
     * 向SAP提交数据
     * @param funName
     * @param payload
     * @return
     * @throws JCO.Exception
     */
    public JCO.Function submit(String funName, JcoPayload payload) throws JCO.Exception{
        JCO.Function function = repository.getFunctionTemplate(funName).getFunction();
        if(function == null) throw new JCO.Exception(-1, "Function Not Exist", function + " 不存在.");
        updateInputPayload(function, payload);
        client.execute(function);
        return function;
    }
    
    /**
     * 取 提交的返回结果
     * @param function
     * @return
     * @throws JCO.Exception
     */
    public JcoPayload getResponse(JCO.Function function, Set<String> structures, Set<String> tables) throws JCO.Exception{
        JcoPayload payload = new JcoPayload();
        // 获取 paramList 返回值
        JCO.ParameterList paramOut = function.getExportParameterList();
        log.debug("Export < Parameter: \n" + paramOut.toString());
        for(JCO.FieldIterator it = paramOut.fields(); it.hasMoreElements();) {
            JCO.Field field = it.nextField();
            payload.addParamValue(field.getName(), field.getValue());
        }
        // 获取 structurs 返回值
        if(structures != null && !structures.isEmpty()) {
            for(String struName : structures) {
                JCO.Structure structure = paramOut.getStructure(struName);
                if(structure == null) continue;
                log.debug("Export < Structure - " + struName + ":" + structure.toXML());
                log.debug("Export < Structure - " + struName + ": \n" + structure.toString());
                payload.addStructure(struName);
                for(JCO.FieldIterator it = structure.fields(); it.hasMoreElements();) {
                    JCO.Field field = it.nextField();
                    payload.addStructureValue(struName, field.getName(), field.getValue());
                }
            }
        }
     // 获取 table 返回值
        if(tables != null && !tables.isEmpty()) {
            JCO.ParameterList jcotable = function.getTableParameterList();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            for(String talName : tables) {
                JCO.Table table = jcotable.getTable(talName);
                if(table == null) continue;
                log.debug(table.toXML());
                log.debug("Export < Table - " + talName + ": \n" + table.toString());
                payload.addTable(talName);
                table.firstRow();
                for(int i=0; i<table.getNumRows(); i++, table.nextRow()) {
                    Map<String, Object> value = new HashMap<String, Object>();
                    for(JCO.FieldIterator it = table.fields(); it.hasMoreElements();) {
                        JCO.Field field = it.nextField();
                        String colvalue = (field.getValue() instanceof Date) 
                                ? sdf.format((Date)field.getValue()) 
                                : field.getString();
                        value.put(field.getName(), colvalue);
                    }
                    payload.addTableValue(talName, value);
                }
            }
        }
        return payload;
    }
    

    /**
     * 取SAP提交的返回结果
     * @param function
     * @param structures
     * @param tables
     * @return
     * @throws JCO.Exception
     */
    public JcoPayload getResponse(JCO.Function function, String[] structures, String[] tables) throws JCO.Exception{
        Set<String> struSet = new HashSet<String>();
        Set<String> tblSet = new HashSet<String>();
        Collections.addAll(struSet, structures);
        Collections.addAll(tblSet, tables);
        return getResponse(function, struSet, tblSet);
    }
    
    /**
     * 断开连接
     * @throws JCO.Exception
     */
    public void disconnect() throws JCO.Exception{
        log.debug("断开SAP连接.");
        if(client != null) {
            JCO.releaseClient(client);
            JCO.removeClientPool(config.getPool());
        }
    }
    
    /**
     * 装载 输入 参数
     * @param function
     * @param payload
     */
    private void updateInputPayload(JCO.Function function, JcoPayload payload) {
        JCO.ParameterList paramInput  = function.getImportParameterList();
        paramInput.clear();
        /** 先设置structure， 因为param中有些参数来自structure **/
        // 装载 structure 参数
        if(payload.getStructureValues().size() > 0) {
            for(String struName : payload.getStructures()) {
                JCO.Structure structure = paramInput.getStructure(struName);
                if(structure == null) continue;
                structure.clear();
                Map<String, Object> structureValues = payload.getStructureValues().get(struName);
                for(Map.Entry<String, Object> entry : structureValues.entrySet()) {
                    structure.setValue(entry.getValue(), entry.getKey());
                }
                log.debug(" Import > Structure(XML) " + struName + " : " + structure.toXML());
                log.debug(" Import > Structure - " + struName + " : \n" + structure.toString());
            }
        }
        // 装载 ParamList 参数
        if(payload.getParamValues().size() > 0) {
            for(Map.Entry<String, Object> entry : payload.getParamValues().entrySet()) {
                paramInput.setValue(entry.getValue(), entry.getKey());
            }
        }
        log.debug(" Import > Parameter(XML): " + paramInput.toXML());
        log.debug(" Import > Parameter: \n" + paramInput.toString());
        // 装载 table 参数
        if(payload.getTableValues().size() > 0) {
            JCO.ParameterList tblInput = function.getTableParameterList();
            for(String tblName : payload.getTables()) {// for:1
                JCO.Table table = tblInput.getTable(tblName);
                if(table == null) continue;
                table.clear();
                List<Map<String, Object>> tableValues = payload.getTableValues().get(tblName);
                for(Map<String, Object> tableValue : tableValues) {// for:2
                    table.appendRow();
                    for(Map.Entry<String, Object> entry : tableValue.entrySet()) {
                        table.setValue(entry.getValue(), entry.getKey());
                    }
                }// for:2
                log.debug("Import > Table(XML) - " + table.toXML());
                log.debug("Import > Table - " + tblName + " : \n" + table.toString());
            }// end for:1
        }// end if
    }
    
    /**
     * @return the client
     */
    public JCO.Client getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(JCO.Client client) {
        this.client = client;
    }

    /**
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepository(IRepository repository) {
        this.repository = repository;
    }
    
    
    
}
