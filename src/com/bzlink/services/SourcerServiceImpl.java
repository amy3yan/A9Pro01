package com.bzlink.services;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.agile.api.IAgileSession;
import com.bzlink.BaseService;
import com.bzlink.config.SysConf;
import com.bzlink.dao.SourcerDao;

@Service
public class SourcerServiceImpl extends BaseService implements SourcerService {
	
	private static Logger log = Logger.getLogger(SourcerServiceImpl.class);
	
	@Autowired
	private SysConf sysConf;
	@Autowired
	private SourcerDao dao;

	public String updateItemSiteSourcers(IAgileSession session, List<Map<String, String>> records) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public String updateItemSiteWhenSourcersChanged(IAgileSession session, List<Map<String, String>> records) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Map<String, Object>> readDatas(InputStream excel) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
