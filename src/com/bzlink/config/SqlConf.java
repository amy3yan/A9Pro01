package com.bzlink.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.bzlink.SpringContextUtil;


public class SqlConf {
    private String SQL_FILE = "/com/bzlink/config/sql.xml";
    private Map<String, String> sqlMap = new HashMap<String, String>();
    private static Logger log = Logger.getLogger(SqlConf.class);

    private SqlConf() {

    }

    public void init() {
        log.info("init sql cache begin.");
        if (this.sqlMap.isEmpty()) {
            this.readXmlToCache();
        }
        log.info("init sql cache end.");
    }

    public static SqlConf getInstance() {
        return (SqlConf) SpringContextUtil.getBean("sqlConf");
    }

    public String getSql(String key) {
        return this.sqlMap.get(key);
    }

    private void readXmlToCache() {
        SAXReader saxReader = new SAXReader();
        try {
            Document doc = saxReader.read(SqlConf.class.getResourceAsStream(SQL_FILE));

            Element root = doc.getRootElement();

            List childList = root.elements();
            for (Object obj : childList) {
                Element ele = (Element) obj;
                String key = ele.getName();
                String value = ele.getStringValue();
                this.sqlMap.put(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
