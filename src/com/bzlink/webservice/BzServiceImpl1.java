package com.bzlink.webservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.MTOM;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.agile.api.APIException;
import com.agile.api.ChangeConstants;
import com.agile.api.FileFolderConstants;
import com.agile.api.IAdmin;
import com.agile.api.IAgileClass;
import com.agile.api.IAgileList;
import com.agile.api.IAgileSession;
import com.agile.api.IAttachmentFile;
import com.agile.api.IAutoNumber;
import com.agile.api.ICell;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IFileFolder;
import com.agile.api.IItem;
import com.agile.api.IManufacturer;
import com.agile.api.IManufacturerPart;
import com.agile.api.IQuery;
import com.agile.api.IRow;
import com.agile.api.ISignoffReviewer;
import com.agile.api.IStatus;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.IWorkflow;
import com.agile.api.ItemConstants;
import com.agile.api.ManufacturerConstants;
import com.agile.api.ManufacturerPartConstants;
import com.agile.api.UserConstants;
import com.agile.api.WorkflowConstants;

@MTOM
@WebService(endpointInterface = "com.hand.webservice.HandService")
public class BzServiceImpl1 implements BzService1
{
    @Resource
    private WebServiceContext webServiceContext;
    
    private static final Logger log = Logger.getLogger("PUR2PLM");
    
    /**
     * BOM级替代定义的属性，用逗号分隔
     */
    public static final int REPLACEMENT_ITEMS_IN_BOM = 1341;
    
    /**
     * 替代料中忽略的物料 by number
     */
    public static Set<String> IGNORE_ITEMS = new HashSet<String>();
    static
    {
        IGNORE_ITEMS.add("081-00141-01");
        IGNORE_ITEMS.add("082-00114-01");
        IGNORE_ITEMS.add("082-00115-01");
    }
    
    static final String tZone = SystemConfig.getValue("TimeZone");
    
    static final boolean isText = Boolean.valueOf(SystemConfig.getValue("isText"));
    
    static final String dealmark = "!";
    
    static final String sql = "insert into agile.cig_plm2mes_bomversion (modelno,bomver,description,lifecyclephase,"
        + "revreleasedate,createuser,createtime,splch,sendtime,dealmark,ebomvalidstart,ebomvalidend,"
        + "revincorpdate,remark,validstart,validend,changenum,assemblytype,writetime,parttype,emailaddress,urlparttype)"
        + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    
    static final String sqlBom =
        "insert into agile.cig_plm2mes_bom (modelno,bomrev,levelNo,compno,ismain,replacement,parentcompno,"
            + "itemrev,qty,assemblytype,matchcode,unit,sendtime,dealmark,rc,objecturl,findnum,writetime,refdes,picurl,fileurl,parttype,kitno,emailaddress,urlparttype)"
            + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    
    static final String sqlFile =
        "insert into agile.cig_plm2mes_itemfile (itemno, itemrev, filename, foldernum, folderrev, dealmark,"
            + "sendtime, filetype, attachmenttype, itemtype, writetime) values(?,?,?,?,?,?,?,?,?,?,?)";
    
    //static final String sqlq = "select count(1) from agile.cig_plm2mes_bomversion b where b.changenum = ?";
    static final String sqlq = "select count(1) from agile.cig_plm2mes_bomversion b where b.changenum = ? and b.bomver = ? and modelno = ?";
    static final String sqlf = "select count(1) from agile.cig_plm2mes_itemfile f where f.itemno = ? and f.itemrev = ?"
        + " and f.filename = ? and f.foldernum = ? and f.folderrev = ?";
    
    static final String insertResultInfo = "insert into agile.CIG_LOGS(sysname,dealtime,changenum,action,data,success,errormsg) values(?,?,?,?,?,?,?)";
    
    private Connection conn = null;
    private Connection connlog = null;
    private PreparedStatement psa = null;
    
    private PreparedStatement psb = null;
    
    private PreparedStatement psc = null;
    
    private PreparedStatement psq = null;
    
    private PreparedStatement psf = null;
    private PreparedStatement insertPS = null;
    
    static String mfr_api = PURConfig.getValue("mfr_api");  //制造商API Name
    static String mfrp_api = PURConfig.getValue("mfrp_api");  //制造商物料API Name
    static String sa_api = PURConfig.getValue("sa_api"); //SA流程的API Name
    static String agileurl = PURConfig.getValue("agileurl");
    static String tempath = PURConfig.getValue("temppath"); //在服务器上临时生成文件
    static String insertsql = PURConfig.getValue("sql");
    static String type_standard = PURConfig.getValue("doctype1");
    static String type_paper = PURConfig.getValue("doctype2");
    static String type_other = PURConfig.getValue("doctype3");
    static String type_rohs = PURConfig.getValue("doctype4");
    static String sysname = "PUR2PLM";
    
    /**
     * 登录
     * 
     * @param user
     * @param pwd
     * @return
     */
    private IAgileSession login()
    {
        IAgileSession agileSession = null;
        try
        {
//            SysConf sysConf = (SysConf)SpringContextUtil.getBean("sysConf");
//            String url = sysConf.plmURL;
//            String pwd = sysConf.password;
//            String user = sysConf.userName;
            // login
            String url = "http://cigplm.cambridgeig.com:7001/Agile";
            String user = "jgz";
            String pwd ="123456";
            agileSession = AgileUtils.createAgileSession(url, user, pwd);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return agileSession;
    }
    
    /**
     * 获取物料的附件
     * 
     * @param sessionId
     * @param number
     * @param rowId
     * @return
     * @throws IOException
     */
    @Override
    public byte[] getFile(String itemNumber, String itemRev, String fileName)
        throws IOException
    {
        byte[] file = null;
        IAgileSession session = null;
        InputStream stream = null;
        try
        {
            session = this.login();
            log.info("获取文件 && itemnum：" + itemNumber + "itemrev：" + itemRev + "filename：" + fileName);
            IItem item = (IItem)session.getObject(ItemConstants.CLASS_ITEM_BASE_CLASS, itemNumber);
            item.setRevision(itemRev);// 获取指定版本的item
            ITable attTable = item.getAttachments();
            ITwoWayIterator it = attTable.getTableIterator();
            while (it.hasNext())
            {
                IRow row = (IRow)it.next();
                if (row.getValue(ItemConstants.ATT_ATTACHMENTS_FILENAME).toString().equals(fileName))
                {
                    IFileFolder fileFolder = (IFileFolder)row.getReferent();
                    ITable fileTb = fileFolder.getTable(FileFolderConstants.TABLE_FILES);
                    Iterator fit = fileTb.iterator();
                    while (fit.hasNext())
                    {
                        IRow fr = (IRow)fit.next();
                        if (CellUtils.getCellStringValue(fr.getCell(FileFolderConstants.ATT_FILES_FILE_NAME))
                            .equals(fileName))
                        {
                            stream = ((IAttachmentFile)fr).getFile();
                            file = HandStringUtils.toByteArray(stream);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (session != null)
            {
                session.close();
            }
            if (stream != null)
            {
                stream.close();
            }
        }
        return file;
    }
    
    @SuppressWarnings("finally")
    public String PLM2MES(String xml)
    {
        StringBuffer result = new StringBuffer();
        IAgileSession session = null;
        String bomNum = null;
        String rev = null;
        try
        {
            session = this.login();
            String method = this.getNodeText(xml, "RC", "HEAD");
            DBConnectionAgile dbConnection = new DBConnectionAgile();
            connlog = dbConnection.getConnection();
            this.insertPS = connlog.prepareStatement(insertResultInfo);
            if ("MSAG_BOM_A3".equals(method))
            {// 工艺接受或者拒绝的设计BOM结果反馈
                this.getMESRejectOrAccessInfo(xml, session);
            }
            else if ("MSAG_BOM_A2".equals(method))
            {// 整版本设计BOM请求
                bomNum = this.getNodeText(xml, "MODEL", "HEAD").trim();
                rev = this.getNodeText(xml, "VERSION", "HEAD").trim();
                log.info("bomNum:"+bomNum+";rev:"+rev);
                if (bomNum == null || "".equals(bomNum))
                {
                    log.info("MES传递参数错误:MODEL不能为空");
                    result.append("MES传递参数错误:MODEL不能为空");
                    insertInfo("bomNum:"+bomNum+";rev:"+rev,"","MSAG_BOM_A2","Y",result.toString());  
                }
                else
                {
                    conn = dbConnection.getConnection();
                    this.getBom(bomNum, rev, session);
                    insertInfo("bomNum:"+bomNum+";rev:"+rev,"","MSAG_BOM_A2","Y","insert into db success");  
                }
            }
        }
        catch (DocumentException e)
        {
            log.info(" 解析xml出错！");
            result.append(" 解析xml出错！");
            e.printStackTrace();
            insertInfo("bomNum:"+bomNum+";rev:"+rev,"","MSAG_BOM_A2","N","DocumentException:"+e.getMessage()+";"+result);
        }
        catch (APIException e)
        {
            log.info(" AgileAPI出错！");
            result.append(" AgileAPI出错！");
            e.printStackTrace();
            insertInfo("bomNum:"+bomNum+";rev:"+rev,"","MSAG_BOM_A2","N","APIException:"+e.getMessage()+";"+result);
        }
        catch (SQLException e)
        {
           // log.info(" 写入数据库出错！");
            result.append(" 写入数据库出错！");
            log.error("写入数据库出错！", e);
            e.printStackTrace();
            insertInfo("bomNum:"+bomNum+";rev:"+rev,"","MSAG_BOM_A2","N","SQLException:"+e.getMessage()+";"+result);
        }
        catch (Exception e)
        {
        	result.append(e.getMessage());
            log.error(e.getMessage());
            e.printStackTrace();
            insertInfo("bomNum:"+bomNum+";rev:"+rev,"","MSAG_BOM_A2","N","Exception:"+e.getMessage()+";"+result);
        }
        finally
        {
            if (session != null)
            {
                session.close();
            }
            if (psa != null)
            {
                try
                {
                    psa.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (psb != null)
            {
                try
                {
                    psb.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (psc != null)
            {
                try
                {
                    psc.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (psq != null)
            {
                try
                {
                    psq.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (psf != null)
            {
                try
                {
                    psf.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (conn != null)
            {
                try
                {
                    // conn.setAutoCommit(true);
                    conn.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (insertPS != null)
            {
                try
                {
                    insertPS.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            if (connlog != null)
            {
                try
                {
                    // conn.setAutoCommit(true);
                    connlog.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace(System.out);
                }
            }
            try
            {
                xml = setNodeText(xml, "ERRORMSG", result.toString());
            }
            catch (DocumentException e)
            {
                e.printStackTrace();
            }
            log.info(xml);
            return xml;
        }
    }
    
    /**
     * 
     * 获取指定xml节点的值
     * 
     * @param xml
     * @param nodeName
     * @return
     * @throws DocumentException
     * @author zhangguoli
     * @date 2016年10月18日 下午6:16:27
     */
    public String getNodeText(String xml, String nodeName, String topNode)
        throws DocumentException
    {
        SAXReader reader = new SAXReader();
        String value = null;
        Document doc = DocumentHelper.parseText(xml);
        Element root = doc.getRootElement();
        Iterator<Element> it = root.elementIterator();
        while (it.hasNext())
        {
            Element node = (Element)it.next();
            if (node.getName().equals(topNode))
            {
                Iterator<Element> childIt = node.elementIterator();
                while (childIt.hasNext())
                {
                    Element element = (Element)childIt.next();
                    if (element.getName().equals(nodeName))
                    {
                        value = element.getText();
                    }
                }
            }
        }
        return value;
    }
    
    /**
     * 
     * 设置指定节点的XML的值
     * 
     * @param xml
     * @param nodeName
     * @param text
     * @throws DocumentException
     * @author zhangguoli
     * @date 2016年10月18日 下午6:16:49
     */
    public String setNodeText(String xml, String nodeName, String text)
        throws DocumentException
    {
        SAXReader reader = new SAXReader();
        String value = null;
        Document doc = DocumentHelper.parseText(xml);
        Element root = doc.getRootElement();
        Iterator<Element> it = root.elementIterator();
        while (it.hasNext())
        {
            Element node = (Element)it.next();
            if (node.getName().equals("HEAD"))
            {
                Iterator<Element> childIt = node.elementIterator();
                while (childIt.hasNext())
                {
                    Element element = (Element)childIt.next();
                    if (element.getName().equals(nodeName))
                    {
                        element.setText(text);
                    }
                }
            }
        }
        
        return doc.asXML();
    }
    
    /**
     * 请求整版BOM数据
     * 
     * @param bomNum
     * @param rev
     * @return
     * @author zhangguoli
     * @throws Exception
     * @date 2016年10月18日 下午2:49:01
     */
    public void getBom(String bomNum, String rev, IAgileSession session)
        throws Exception
    {
        log.info("start BOM................"+bomNum);
        String sendtime = this.getLocalTime(tZone);
//        StringBuffer sb = new StringBuffer();
        IItem item = (IItem)session.getObject(ItemConstants.CLASS_ITEM_BASE_CLASS, bomNum);
        if (null == item)
        {
        	log.info(bomNum + "在Agile系统中不存在!");
        	throw new Exception(bomNum + "在Agile系统中不存在!");
        }
        else
        {
            // conn.setAutoCommit(false);
            this.psa = conn.prepareStatement(sql);
            this.psb = conn.prepareStatement(sqlBom);
            this.psc = conn.prepareStatement(sqlFile);
            this.psf = conn.prepareStatement(sqlf);
            this.psq = conn.prepareStatement(sqlq);
            /** 版本为空则取最新的版本，版本不为空，如果指定具体的版本（版本号+8个空格+changnumber）直接取指定的版本，如果只给版本号（比如1.2）则取1.2里面的最新版本 **/
            if (null == rev || "".equals(rev))
            {
                log.info("rev is null");
                dealBOM(item,sendtime);
            }
            else if (rev.length() < 8)
            {
                log.info("rev less than 8    :"+rev);
                Map map = item.getRevisions();
                Iterator it = map.keySet().iterator();
                while (it.hasNext())
                {
                    Object key = it.next();
                    Object value = map.get(key);
                    // System.out.println(key+"--"+value);
                    if (rev.equals(value))
                    {
                        item.setRevision(key);
                        dealBOM(item,sendtime);
                        break;
                    }
                }
            }
            else
            {
                log.info("rev is "+rev);
                item.setRevision(rev);
                dealBOM(item,sendtime);
                
            }
            long start = System.currentTimeMillis();
            log.info("submit BOM start................"+start);
            this.psb.executeBatch();
            this.psa.executeBatch();
            log.info("submit BOM end................"+String.valueOf(System.currentTimeMillis()-start));
            this.psa = null;
            this.psb = null;
            this.psc = null;
            this.psf = null;
            this.psq = null;
            conn=null;
            // conn.commit();
            log.info("insert into db success");
        }
        
//        sb.append("insert into db success");
//        return sb.toString();
    }
    
    /**
     * 
     * MES工艺接受或者拒绝的设计BOM结果反馈
     * 
     * @param xml
     * @param session
     * @throws DocumentException
     * @author zhangguoli
     * @throws APIException
     * @date 2016年10月18日 下午7:06:48
     */
    public void getMESRejectOrAccessInfo(String xml, IAgileSession session)
        throws DocumentException, APIException
    {
        
        Set<Map<String, String>> set = this.getChangeNumSet(xml);
        Iterator it = set.iterator();
        while (it.hasNext())
        {
            Map<String, String> map = (Map<String, String>)it.next();
            setInfoToAgile(map, session);
        }
        
    }
    
    /**
     * 表单上设置mes拒绝接收BOM信息 <详细描述>
     * 
     * @param map
     * @param session
     * @throws APIException
     * @author zhangguoli
     * @date 2016年10月19日 上午10:34:45
     */
    public void setInfoToAgile(Map<String, String> map, IAgileSession session)
        throws APIException
    {
        IChange change = (IChange)session.getObject(ChangeConstants.CLASS_CHANGE_BASE_CLASS, map.get("changeNum"));
        String mesInfo = CellUtils.getCellStringValue(change.getCell(ChangeConstants.ATT_PAGE_TWO_NOTES), tZone);
        StringBuffer sb = new StringBuffer(mesInfo);
        sb.append("MES拒绝接收：" + map.get("model") + "拒绝原因：" + map.get("dealremark"));
        change.setValue(ChangeConstants.ATT_PAGE_TWO_NOTES, sb);
    }
    
    /**
     * 
     * 获取change编号
     * 
     * @param xml
     * @return
     * @throws DocumentException
     * @author zhangguoli
     * @date 2016年10月18日 下午6:55:18
     */
    public Set<Map<String, String>> getChangeNumSet(String xml)
        throws DocumentException
    {
        Set<Map<String, String>> set = new HashSet<Map<String, String>>();
        
        SAXReader reader = new SAXReader();
        String value = null;
        Document doc = DocumentHelper.parseText(xml);
        Element root = doc.getRootElement();
        Iterator<Element> it = root.elementIterator();
        while (it.hasNext())
        {
            Element node = (Element)it.next();
            if (node.getName().equals("BOMVERSIONS"))
            {
                Iterator<Element> bomVersions = node.elementIterator();
                while (bomVersions.hasNext())
                {
                    Element element = (Element)bomVersions.next();
                    Iterator<Element> bomVersion = element.elementIterator();
                    Map<String, String> map = new HashMap<String, String>();
                    while (bomVersion.hasNext())
                    {
                        Element version = (Element)bomVersion.next();
                        if (version.getName().equals("CHANGENUM"))
                        {
                            map.put("changeNum", version.getText());
                        }
                        else if (version.getName().equals("MODEL"))
                        {
                            map.put("model", version.getText());
                        }
                        else if (version.getName().equals("DEALREMARK"))
                        {
                            map.put("dealremark", version.getText());
                        }
                    }
                    set.add(map);
                }
            }
        }
        return set;
    }
    
    /**
     * 
     * 处理bom同步
     * @param item
     * @param wt
     * @author zhangguoli
     * @throws Exception 
     * @date 2016年10月13日 上午10:38:53
     */
private void dealBOM(IItem item,String sendtime) throws Exception{
        String modelNo =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_NUMBER), tZone);
        String bomver = CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_REV), tZone);
        if ("Introductory".equals(bomver)) return;  //过滤初始版本的BOM(不用发给MES)
        String description =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_DESCRIPTION), tZone);
        String lifecyclephase =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_LIFECYCLE_PHASE), tZone);
        String revreleasedate =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_REV_RELEASE_DATE), tZone);
        String createuser =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_PAGE_TWO_CREATE_USER), tZone);
        String createtime =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_PAGE_TWO_DATE06), tZone);
        String splch = "";
//        String sendtime = this.sendtime;
        String dealmark = this.dealmark;
        String ebomvalidstart = "";
        // CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_EFFECTIVITY_DATE),tZone);
        String ebomvalidend = "";
        String revincorpdate =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_REV_INCORP_DATE), tZone);
        String remark = "";
        String validstart = "";
        String validend = "";
        String changenum = bomver.split("        ")[1];
        String assemblytype =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_PAGE_TWO_LIST01), tZone);
        String wt = this.writeTime(changenum,bomver,modelNo);
        IChange change = (IChange)item.getSession().getObject(IChange.OBJECT_TYPE, changenum);
        String email = this.getEmail(change);
        String parttype =
            CellUtils.getCellStringValue(item.getCell(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE), tZone);
        String aftype = null;
        if(item.getAgileClass().getAPIName().startsWith("Part_")){
            aftype = parttype.substring(6);
        }else{
            aftype = parttype;
        }
        
        // modelno,bomver,description,lifecyclephase,"
        // + "revreleasedate,createuser,createtime,splch,sendtime,dealmark,ebomvalidstart,ebomvalidend,"
        // + "revincorpdate,remark,validstart,validend,changenum,assemblytype,writetime
        this.psa.setString(1, modelNo);
        this.psa.setString(2, bomver);
        this.psa.setString(3, description);
        this.psa.setString(4, lifecyclephase);
        this.psa.setString(5, revreleasedate);
        this.psa.setString(6, createuser);
        this.psa.setString(7, createtime);
        this.psa.setString(8, splch);
        this.psa.setString(9, sendtime);
        this.psa.setString(10, dealmark);
        this.psa.setString(11, ebomvalidstart);
        this.psa.setString(12, ebomvalidend);
        this.psa.setString(13, revincorpdate);
        this.psa.setString(14, remark);
        this.psa.setString(15, validstart);
        this.psa.setString(16, validend);
        this.psa.setString(17, changenum);
        this.psa.setString(18, assemblytype);
        this.psa.setString(19, wt);
        this.psa.setString(20, parttype);
        this.psa.setString(21, email);
        this.psa.setString(22, aftype);
        this.psa.addBatch(); 
        dealAttachments(item, modelNo, bomver, parttype, dealmark, sendtime, wt); 
        /** 处理子阶数据 **/
        String modelno = modelNo;
        String bomrev = bomver;
        this.psb.setString(1, modelno);
        this.psb.setString(2, bomrev);
//        System.out.println("modelno:"+modelno+"    bomrev:"+bomrev);
        recursionBom(item, tZone, 1, wt,email,sendtime);
    }
    
    /**
     * 
     * 递归获取整版bom数据
     * 
     * @param item
     * @param tZone
     * @author zhangguoli
     * @throws Exception 
     * @date 2016年9月29日 下午5:28:16
     */
    private void recursionBom(IItem item, String tZone, int level, String wt,String email,String sendtime)
        throws Exception
    {
        ITable bomTable = item.getTable(ItemConstants.TABLE_BOM);
        ITwoWayIterator it = bomTable.getTableIterator();
        String replace_rc = "TP";
        int i = 0;
        while (it.hasNext())
        {
            IRow row = (IRow)it.next();
            IItem child = (IItem)row.getReferent();
            String levelno = String.valueOf(level);
            String compno = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_ITEM_NUMBER), tZone);
            String ismain = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_BOM_TEXT04), tZone);
            String replacement =
                CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_ITEM_MULTITEXT30), tZone);
            String parentcompno = item.getName();
            String itemrev = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_ITEM_REV), tZone);
            String qty = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_QTY), tZone);
            String assemblytype = CellUtils.getCellStringValue(child.getCell(ItemConstants.ATT_PAGE_TWO_LIST01), tZone);
            String matchcode = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_BOM_LIST04), tZone);
            String unit = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_ITEM_LIST06), tZone);
//            String sendtime = this.sendtime;
            String dealmark = this.dealmark;
            String rc = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_BOM_TEXT03), tZone);
            String objecturl = "";
            String findnum = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_FIND_NUM), tZone);
            String writetime = wt;
            String refdes = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_REF_DES), tZone);
            String picurl = "";
            String fileurl = "";
            String part_type = null;
            String parttype =
                CellUtils.getCellStringValue(child.getCell(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE), tZone);            
            if(child.getAgileClass().getAPIName().startsWith("Part_")){
                part_type = parttype.substring(6);
            }else{
                part_type = parttype;
            }
            String kitno = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_BOM_LIST04), tZone);
            String specialRep = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_BOM_BOM_LIST01), tZone); 
           // System.out.println("compno:"+specialRep);
            Set<IItem> set = getReplacementsByRelation(child,row);
            List<IItem> repList = this.sortByName(set);
           
            if(repList.isEmpty()){
                this.psb.setString(3, levelno);
                this.psb.setString(4, compno);
                if("".equals(ismain)&&!"".equals(qty)){
                    i++;
                    this.psb.setString(5, "Y");
                    this.psb.setString(15, replace_rc+i);
                }else{
                    this.psb.setString(5, ismain);
                    this.psb.setString(15, rc);
                }
                this.psb.setString(6, replacement);
                this.psb.setString(7, parentcompno);
                this.psb.setString(8, itemrev);
                this.psb.setString(9, qty);
                this.psb.setString(10, assemblytype);
                this.psb.setString(11, matchcode);
                this.psb.setString(12, unit);
                this.psb.setString(13, sendtime);
                this.psb.setString(14, dealmark);
//                this.psb.setString(15, rc);
                this.psb.setString(16, objecturl);
                this.psb.setString(17, findnum);
                this.psb.setString(18, writetime);
              //  this.psb.setString(19, refdes);
                StringReader reader = new StringReader(refdes);
                this.psb.setCharacterStream(19, reader, refdes.length());
                
                this.psb.setString(20, picurl);
                this.psb.setString(21, fileurl);
                this.psb.setString(22, part_type);
                this.psb.setString(23, kitno);
                this.psb.setString(24, email);
                this.psb.setString(25, parttype);
                this.psb.addBatch();
            }else{
                i++;
                this.psb.setString(3, levelno);
                this.psb.setString(4, compno);
                
                this.psb.setString(6, replacement);
                this.psb.setString(7, parentcompno);
                this.psb.setString(8, itemrev);
                this.psb.setString(9, qty);
                this.psb.setString(10, assemblytype);
                this.psb.setString(11, matchcode);
                this.psb.setString(12, unit);
                this.psb.setString(13, sendtime);
                this.psb.setString(14, dealmark);
                
                this.psb.setString(16, objecturl);
                this.psb.setString(17, findnum);
                this.psb.setString(18, writetime);
              //  this.psb.setString(19, refdes);
                
                StringReader reader = new StringReader(refdes);
                this.psb.setCharacterStream(19, reader, refdes.length());
                this.psb.setString(20, picurl);
                this.psb.setString(21, fileurl);
                this.psb.setString(22, part_type);
                this.psb.setString(23, kitno);
                this.psb.setString(24, email);
                this.psb.setString(25, parttype);
                if("Yes".equalsIgnoreCase(specialRep))
                {
                    this.psb.setString(5, ismain);
                    this.psb.setString(15, rc);
                }else{
                    this.psb.setString(5, "Y");
                    this.psb.setString(15, replace_rc+i);
                }
                this.psb.addBatch();
                
                Iterator repaceIt = repList.iterator();
                while (repaceIt.hasNext())
                {
                    IItem replaceItem = (IItem)repaceIt.next();
                    this.psb.setString(3, levelno);
                    String replace_compno = CellUtils.getCellStringValue(replaceItem.getCell(ItemConstants.ATT_TITLE_BLOCK_NUMBER), tZone);
                    
                    this.psb.setString(4, replace_compno);
                    this.psb.setString(5, "");
                    this.psb.setString(6, "");
                    this.psb.setString(7, parentcompno);
                    
                    String replace_itemrev = CellUtils.getCellStringValue(replaceItem.getCell(ItemConstants.ATT_TITLE_BLOCK_REV), tZone);
                    
                    this.psb.setString(8, replace_itemrev);
                    this.psb.setString(9, "");
                    String replace_assemblytype = CellUtils.getCellStringValue(replaceItem.getCell(ItemConstants.ATT_PAGE_TWO_LIST01), tZone);
                    this.psb.setString(10, replace_assemblytype);
                    this.psb.setString(11, "");
                    String replace_unit = CellUtils.getCellStringValue(replaceItem.getCell(ItemConstants.ATT_PAGE_TWO_LIST06), tZone);
                    this.psb.setString(12, replace_unit);
                    this.psb.setString(13, sendtime);
                    this.psb.setString(14, dealmark);
                    if("Yes".equalsIgnoreCase(specialRep))
                    {
                        this.psb.setString(15, rc);
                    }else{
                        this.psb.setString(15, replace_rc+i);
                    }
                    this.psb.setString(16, "");
                    this.psb.setString(17, "");
                    this.psb.setString(18, writetime);
                    this.psb.setString(19, "");
                    this.psb.setString(20, "");
                    this.psb.setString(21, "");
                    String replacePartType = null;
                    String replace_parttype =
                        CellUtils.getCellStringValue(replaceItem.getCell(ItemConstants.ATT_TITLE_BLOCK_PART_TYPE), tZone);
                    
                    if(replaceItem.getAgileClass().getAPIName().startsWith("Part_")){
                        replacePartType = replace_parttype.substring(6);
                    }else{
                        replacePartType = replace_parttype;
                    }
                    this.psb.setString(22, replacePartType);
                    this.psb.setString(23, "");
                    this.psb.setString(24, email);
                    this.psb.setString(25, replace_parttype);
                    this.psb.addBatch();
                }
            }          
//            this.psb.setString(3, levelno);
//            this.psb.setString(4, compno);
//            this.psb.setString(5, ismain);
//            this.psb.setString(6, replacement);
//            this.psb.setString(7, parentcompno);
//            this.psb.setString(8, itemrev);
//            this.psb.setString(9, qty);
//            this.psb.setString(10, assemblytype);
//            this.psb.setString(11, matchcode);
//            this.psb.setString(12, unit);
//            this.psb.setString(13, sendtime);
//            this.psb.setString(14, dealmark);
//            this.psb.setString(15, rc);
//            this.psb.setString(16, objecturl);
//            this.psb.setString(17, findnum);
//            this.psb.setString(18, writetime);
//            this.psb.setString(19, refdes);
//            this.psb.setString(20, picurl);
//            this.psb.setString(21, fileurl);
//            this.psb.setString(22, parttype);
//            this.psb.setString(23, kitno);
//            this.psb.addBatch();            
            dealAttachments(child, compno, itemrev, parttype, dealmark, sendtime, writetime);
            recursionBom(child, tZone, level + 1, wt,email,sendtime);
        }
        
    }
    
    /**
     * 处理附件
     * 
     * @param item
     * @author JGZ
     * @throws SQLException
     * @date 2016年11月3日下午1:37:19
     */
    public void dealAttachments(IItem item, String itemNum, String itemRev, String itemType, String dealMark,
        String sendTime, String writeTime)
        throws APIException, SQLException
    {
        ITable attachmentTable = item.getTable(ItemConstants.TABLE_ATTACHMENTS);
        ITwoWayIterator iter = attachmentTable.getTableIterator();
        while (iter.hasNext())
        {
            IRow row = (IRow)iter.next();
            String filename = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_ATTACHMENTS_FILE_NAME), tZone);
            if (isText && !filename.endsWith(".txt"))
            {
                continue;
            }
            String filetype = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_ATTACHMENTS_FILE_TYPE), tZone);
            String foldernum =
                CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_ATTACHMENTS_FOLDER_NUMBER), tZone);
            String folderrev =
                CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_ATTACHMENTS_FOLDER_VERSION), tZone);
            String attachmenttype =
                CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_ATTACHMENTS_ATTACHMENT_TYPE), tZone);
            if (this.isSameDate(itemNum, itemRev, filename, foldernum, folderrev))
            {
                continue;
            }
            this.psc.setString(1, itemNum);
            this.psc.setString(2, itemRev);
            this.psc.setString(3, filename);
            this.psc.setString(4, foldernum);
            this.psc.setString(5, folderrev);
            this.psc.setString(6, dealMark);
            this.psc.setString(7, sendTime);
            this.psc.setString(8, filetype);
            this.psc.setString(9, attachmenttype);
            this.psc.setString(10, itemType);
            this.psc.setString(11, writeTime);
            this.psc.addBatch();
        }
        this.psc.executeBatch();// 每一个item处理完毕提交，避免重复文件，会造成DB压力大
    }
    
    /**
     * 
     * 获取本地时间
     * 
     * @param tZone
     * @return
     * @author zhangguoli
     * @date 2016年9月26日 下午6:46:38
     */
    private String getLocalTime(String tZone)
    {
        Date date = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        sf.setTimeZone(TimeZone.getTimeZone(tZone)); // China timezone
        return sf.format(date);
    }
    
    /**
     * 
     * 获取写入次数
     * 
     * @param conn
     * @param sql
     * @param query
     * @param changeNum
     * @return
     * @throws SQLException
     * @author zhangguoli
     * @date 2016年9月29日 下午6:39:42
     */
    private String writeTime(String changeNum)
        throws SQLException
    {
        this.psq.setString(1, changeNum);
        ResultSet rs = this.psq.executeQuery();
        int count = 0;
        while (rs.next())
        {
            count = rs.getInt(1);
        }
        return String.valueOf(++count);
    }
    
    
    public static void main(String[] argc)
    {
       // StringBuffer sb = new StringBuffer();
        
        String bomarray = "";
        
        String array[] = bomarray.split(";");
        
        String xml ="<?xml version=\"1.0\"?>"
            + "<ROOT>"
            + "<HEAD>"
            + "<RC>MSAG_BOM_A2</RC>"
            + "<RCNAME>整版设计BOM请求</RCNAME>"
            + "<APP_ID>{C1B131DC-C718-4FDE-8DF4-F3C2D0AF3126}</APP_ID>"
            + "<AUTH_TOKEN>F6I:ST@XNJSJSggjTaosc^yhzklmllvvyl</AUTH_TOKEN>"
            + "<RETURNTYPE>处理回执</RETURNTYPE>"
            + "<TIMESTAMP>2017-02-06 17:06:59</TIMESTAMP>"
            + "<STATUS>1</STATUS>"
            + "<REMARK></REMARK>"
            + "<ERRORMSG></ERRORMSG>"
            + "<LGUID>50969E50-141B-41EA-8B16-1E71003133EC</LGUID>"
            + "<SENDTYPE>同步</SENDTYPE>"
            + "<SENDFROM>MES</SENDFROM>"
            + "<MODEL>19G-90136</MODEL>"
            + "<VERSION>1.1</VERSION>"
            + "<APPLYTIME>20170206170659</APPLYTIME>"
            + "</HEAD>"
            + "</ROOT>";
        
        for (String bomNum : array)
        {
            System.out.println(bomNum);
//           String bomxml = xml.replace("00000000", bomNum);
            BzServiceImpl1 service = new BzServiceImpl1();
            String rs = service.PLM2MES(xml);
          //  System.out.println(rs);
        }
        
        
            
//            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<ROOT>" + "<HEAD>" + "<RC>MSAG_BOM_A2</RC>"
//                + "<RCNAME>整版设计BOM请求</RCNAME>" + "<APP_ID>{051AE010-5606-457E-963D-494D54C6B6E5}</APP_ID>"
//                + "<AUTH_TOKEN>3:8JP=@A@JMIQJSVZJTBA`S^GNK}PQLWL{L~</AUTH_TOKEN>" + "<RETURNTYPE>处理回执</RETURNTYPE>"
//                + "<TIMESTAMP>2016-08-15 14:42:50</TIMESTAMP>" + "<STATUS>1</STATUS>" + "<REMARK></REMARK>"
//                + "<LGUID>1B3E8058-0130-4CCC-8678-A790472E4A07</LGUID>" + "<SENDTYPE>同步</SENDTYPE>"
//                + "<SENDFROM></SENDFROM>" + "<MODEL>13Y-00201-AM1</MODEL>" + "<VERSION>1.1        HDC-01121</VERSION>"
//                + "<APPLYTIME></APPLYTIME>" + "</HEAD>" + "</ROOT>";
        
        String rejectXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<ROOT>"
            + "<HEAD>"
            + "<RC>MSAG_BOM_A3</RC>"
            + "<RCNAME>工艺接受或者拒绝的设计BOM结果反馈</RCNAME>"
            + "<APP_ID>{28498373-8493-4BCE-B0A8-EA283C8D3007}</APP_ID>"
            + "<AUTH_TOKEN>5=;BC@FD@MKRNJScfjTk[ng^xviqnlwlvuwl</AUTH_TOKEN>"
            + "<RETURNTYPE>处理回执</RETURNTYPE>"
            + "<TIMESTAMP>2016-10-21 15:53:38</TIMESTAMP>"
            + "<STATUS>1</STATUS>"
            + "<REMARK></REMARK>"
            + "<LGUID>0FB8B71E-34D1-47E4-AFA5-AAEB8EB5AA29</LGUID>"
            + "<SENDTYPE>同步</SENDTYPE>"
            + "<SENDFROM>MES</SENDFROM>"
            + "</HEAD>"
            + "<BOMVERSIONS>"
            + "<BOMVERSION>"
            + "<MODEL>13G-00005-AM2</MODEL>"
            + "<BOMVER>1.5        HDC-01092</BOMVER>"
            + "<DEALSTATUS></DEALSTATUS>"
            + "<VALIDSTART>20160804</VALIDSTART>"
            + "<VALIDEND></VALIDEND>"
            + "<DEALMAN></DEALMAN>"
            + "<DEALTIME></DEALTIME>"
            + "<DEALREMARK></DEALREMARK>"
            + "<CHANGENUM>HDC-01092</CHANGENUM>"
            + "</BOMVERSION>"
            + "</BOMVERSIONS>"
            + "</ROOT>";

        
    }
    
    /**
	 * 把记录插入数据库
	 *  
	 * @param data
	 * @param changenum
	 * @param issuccess
	 * @param errormsg
	 * @throws Exception
	 * @author JGZ
	 * @date 2017年4月11日下午3:34:08
	 */
	public void insertLogs(PreparedStatement psa, String action, String data, String changenum, boolean issuccess, String errormsg) throws Exception
	{
		String dealtime = StringUtil.dateToStr(new Date());
		
		psa.setString(1, sysname);
		psa.setString(2, dealtime);
		psa.setString(3, changenum);
		psa.setString(4, action);
		psa.setString(5, data);
		psa.setString(6, issuccess ? "Y" : "N");
		psa.setString(7, errormsg);
		psa.addBatch();
		psa.executeBatch();
		conn.commit();
	}

	/** 
	 * PUR请求，Agile新建制造商
	 *  
	 * @param jsonData
	 * @return
	 * @author JGZ
	 * @throws Exception 
	 * @date 2016年11月17日上午10:03:01
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public String updateMFR(String username, String password, String mfrInfo) throws Exception 
	{
		log.info("invoke updateMFR method begin!");
		log.info("MFRInfo: " + mfrInfo);
		IAgileSession session = null;
		JSONArray jsonArray = JSONArray.fromObject(mfrInfo);
		JSONObject jsonMsg = new JSONObject();
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		int count = 0;
		PreparedStatement psmt = null;
		String action = "PUR2PLM_updateMFR";
		try 
		{
			DBConnectionAgile dbConnection = new DBConnectionAgile();
			conn = dbConnection.getConnection();
			psmt = conn.prepareStatement(insertsql);
			session = AgileUtils.createSession(agileurl, username, password);
			if (null == session)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "无法与Agile系统建立连接,请联系其管理员查看账号是否失效!");
				insertLogs(psmt, action, mfrInfo, "", false, jsonMsg.toString());
				log.info(jsonMsg);
				return jsonMsg.toString();
			}
			for (Iterator iterator = jsonArray.iterator(); iterator.hasNext();) 
			{
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				JSONObject jsonObj = (JSONObject) iterator.next();
				
				int id = jsonObj.getInt("ID");                             //唯一ID
				String mfrcn = StringUtil.jsonToStr(jsonObj, "VendName");              //制造商名称全称(中文)
				String mfren = StringUtil.jsonToStr(jsonObj, "VendNameEN");            //制造商名称全称(英文)
				String mfrname = StringUtil.jsonToStr(jsonObj, "VendNameSimplified");  //制造商名称简称
				String city = StringUtil.jsonToStr(jsonObj, "Country");            //城市
				String address = StringUtil.jsonToStr(jsonObj, "Address");         //地址
				String creator = StringUtil.jsonToStr(jsonObj, "Creator");         //PUR WEB创建人(工号)
				String creatorname = StringUtil.jsonToStr(jsonObj, "CreatorName"); //创建人姓名
				String telphone = StringUtil.jsonToStr(jsonObj, "Phone");          //电话
				String fax = StringUtil.jsonToStr(jsonObj, "Fax");                 //传真
				String url = StringUtil.jsonToStr(jsonObj, "Website");             //URL
				String contact = StringUtil.jsonToStr(jsonObj, "ContSalesNM");     //联系人
				String email = StringUtil.jsonToStr(jsonObj, "ContSalesEmail");    //Email
				
				if ("".equals(mfrcn) || "".equals(mfren))
				{
					jsonMsg.put("success", false);
					jsonMsg.put("msg", "厂商中/英文全称不能为空");
					log.info(jsonMsg);
					insertLogs(psmt, action, mfrInfo, "", false, jsonMsg.toString());
					return jsonMsg.toString();
				}
				
				try 
				{
					IQuery query = (IQuery) session.createObject(IQuery.OBJECT_TYPE, mfr_api.trim());
					//根据厂商名字来查找是否存在这个厂商
					query.setCriteria("[1754] equal to '" + mfrname + "'");
					ITable table = query.execute();
					//找到多余一个，说明Agile系统中厂商可能有问题
					if (table.size() >= 1)
					{
						jsonMsg.put("success", false);
						jsonMsg.put("msg", "Agile系统中已存在一个此名称" + mfrname + "的制造商!");
						log.info(jsonMsg);
						insertLogs(psmt, action, mfrInfo, "", false, jsonMsg.toString());
						return jsonMsg.toString();
					}
					//只查到一个,即更新制造商
//					if (table.size() == 1)
//					{
//						ITwoWayIterator iter = table.getReferentIterator();
//						while (iter.hasNext()) 
//						{
//							Map<Integer, Object> param = new HashMap<Integer, Object>();
//							IManufacturer mfr = (IManufacturer) iter.next();
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_CITY, city);
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_ADDRESS, address);
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_PHONE, telphone);
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_FAX, fax);
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_URL, url);
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_CONTACT, contact);
//							param.put(ManufacturerConstants.ATT_GENERAL_INFO_EMAIL, email);
//							param.put(ManufacturerConstants.ATT_PAGE_TWO_TEXT11, mfrcn);
//							param.put(ManufacturerConstants.ATT_PAGE_TWO_TEXT12, mfren);
//							log.info(param);
//							mfr.setValues(param);
//							mfr.getCell(ManufacturerConstants.ATT_PAGE_TWO_TEXT13).setValue(id);
//						}
//					}
					//找不到制造商即新建，并且设置属性
					if (table.size() <= 0)
					{
						IManufacturer mfr = (IManufacturer) session.createObject(mfr_api, mfrname);
						Map<Integer, Object> param = new HashMap<Integer, Object>();
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_CITY, city);
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_ADDRESS, address);
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_PHONE, telphone);
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_FAX, fax);
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_URL, url);
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_CONTACT, contact);
						param.put(ManufacturerConstants.ATT_GENERAL_INFO_EMAIL, email);
						param.put(ManufacturerConstants.ATT_PAGE_TWO_TEXT11, mfrcn);
						param.put(ManufacturerConstants.ATT_PAGE_TWO_TEXT12, mfren);
						param.put(ManufacturerConstants.ATT_PAGE_TWO_NOTES, creator+ "_" + creatorname);
						log.info(param);
						mfr.setValues(param);
//						mfr.getCell(ManufacturerConstants.ATT_PAGE_TWO_TEXT13).setValue(id);
					}
					count++;
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
					jsonMsg.put("success", false);
					jsonMsg.put("msg", mfrname + ": " + e.getMessage());
					log.error(e.getMessage(), e);
					insertLogs(psmt, action, mfrInfo, "", false, jsonMsg.toString());
					return jsonMsg.toString();
				}
			}
			
			if (count == 0)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "没有符合条件的数据");
				log.info(jsonMsg);
				insertLogs(psmt, action, mfrInfo, "", false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			String successmsg = "成功处理" + count + "条数据";
			jsonMsg.put("success", true);
			jsonMsg.put("msg", successmsg);
			log.info(jsonMsg);
			insertLogs(psmt, action, mfrInfo, "", true, "");
			log.info("invoke updateMFR method end!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			jsonMsg.put("success", false);
			jsonMsg.put("msg", e.getMessage());
			log.error(e.getMessage(), e);
			try 
			{
				insertLogs(psmt, action, mfrInfo, "", false, jsonMsg.toString());
			} 
			catch (Exception ex) 
			{
				ex.printStackTrace();
			}
			return jsonMsg.toString();
		}
		finally
		{
			if (session != null)
			{
				session.close();
			}
			if (psmt != null)
            {
                try
                {
                	psmt.close();
                }
                catch (SQLException e)
                {
                	log.error(e);
                    e.printStackTrace();
                }
            }
			if (conn != null)
			{
				try 
				{
					if (!conn.isClosed())
					{
						conn.close();
						log.info("成功关闭数据库连接!");
					}
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					log.error(e);
				}
			}
		}
		
		return jsonMsg.toString();
	}
	
	/** 
	 * PUR集成，供方PCN，创建SA流程
	 *  
	 * @param pcnInfo
	 * @return
	 * @author JGZ
	 * @date 2016年11月21日上午9:17:11
	 */
	@SuppressWarnings({ "rawtypes", "deprecation" })
	@Override
	public String createPCN(String username, String password, String pcnInfo) throws Exception
	{
		log.info("invoke createPCN method begin!");
		log.info("PCNInfo: " + pcnInfo);
		JSONObject jsonObj = JSONObject.fromObject(pcnInfo);
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		JSONObject jsonMsg = new JSONObject();
		int count = 0;
		IAgileSession session = null;
		FTPClient ftp = null;
		String number = "";
		IChange change = null;
		PreparedStatement psmt = null;
		String action = "PUR2PLM_createPCN";
		try 
		{
			DBConnectionAgile dbConnection = new DBConnectionAgile();
			conn = dbConnection.getConnection();
			psmt = conn.prepareStatement(insertsql);
			session = AgileUtils.createSession(agileurl, username, password);
			if (null == session)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "无法与Agile系统建立连接,请联系其管理员查看账号是否失效!");
				log.info(jsonMsg);
				insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			ftp = FTPUtil.getConnection();
			IAdmin admin = session.getAdminInstance();
			IAgileClass cls = admin.getAgileClass(sa_api);
			IAutoNumber[] numSource = cls.getAutoNumberSources();
			
			if (numSource != null)
			{
				StringBuffer suggestion = new StringBuffer();
				int id = jsonObj.getInt("ID");     //编号ID
				String changedesc = StringUtil.jsonToStr(jsonObj, "ChangeDes");      //变更描述
				String changereason = StringUtil.jsonToStr(jsonObj, "Subject");      //变更原因
				String productline = StringUtil.jsonToStr(jsonObj, "ProductLine");   //产品线
				String parttype = StringUtil.jsonToStr(jsonObj, "PartType");         //物料类型
				String spcn = StringUtil.jsonToStr(jsonObj, "SPCN");                 //SPCN
				String eol = StringUtil.jsonToStr(jsonObj, "EOL");                   //EOL
				String creator = StringUtil.jsonToStr(jsonObj, "Creator");           //创建人工号
				String creatorname = StringUtil.jsonToStr(jsonObj, "CreatorName");   //创建人姓名
				try 
				{
					JSONArray jsonArray = jsonObj.getJSONArray("List");
					for (Iterator iterator = jsonArray.iterator(); iterator.hasNext();) 
					{
						Map<String, Object> map = new LinkedHashMap<String, Object>();
						String itemid = "";
						try 
						{
							JSONObject jsonChild = (JSONObject) iterator.next();
							String pcnid = StringUtil.jsonToStr(jsonChild, "PCNID");               //PCN编号
							String parts = StringUtil.jsonToStr(jsonChild, "Parts");               //采购建议替代料
							String partnum = StringUtil.jsonToStr(jsonChild, "PartNum");           //制造商物料编码
							itemid = StringUtil.jsonToStr(jsonChild, "ItemID");                    //物料号
							String fileurl = StringUtil.jsonToStr(jsonChild, "FileURL");           //附件说明书
							
							//取物料
							IItem item = (IItem) session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemid);
							if (null == item)
							{
								jsonMsg.put("success", false);
								jsonMsg.put("changenum", number);
								jsonMsg.put("msg", "此物料:" + itemid + "不存在!");
								log.info(jsonMsg);
								insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
								return jsonMsg.toString();
							}
							//判断物料是否在变更中(如果在变更中则直接返回给PURWEB)
							ITable pendingTable = item.getTable(ItemConstants.TABLE_PENDINGCHANGES);
							if (pendingTable.size() > 0)
							{
								String pendchangnum = "";
								ITwoWayIterator pendit = pendingTable.getTableIterator();
								while (pendit.hasNext()) 
								{
									IRow row = (IRow) pendit.next();
									pendchangnum = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_PENDING_CHANGES_NUMBER));
								}
								jsonMsg.put("success", false);
								jsonMsg.put("changenum", number);
								jsonMsg.put("msg", "此物料:" + itemid + "正在" + pendchangnum + "变更单中!");
								log.info(jsonMsg);
								insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
								return jsonMsg.toString();
							}
							map.put("ItemId", item);
							
							//把采购建议替代料进行组合
							if (!"".equals(parts))
							{
								suggestion.append(itemid).append(":").append(parts).append("; ");
							}
							
							//获取物料制造商物料编号并判断是否需要修改
							ITable mfrtable = item.getTable(ItemConstants.TABLE_MANUFACTURERS);
							ITwoWayIterator mfriter = mfrtable.getTableIterator();
							String oldmpn = "";
							String oldmfn = "";
							while (mfriter.hasNext()) 
							{
								IRow row = (IRow) mfriter.next();
								oldmpn = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_MANUFACTURERS_MFR_PART_NUMBER));
								oldmfn = CellUtils.getCellStringValue(row.getCell(ItemConstants.ATT_MANUFACTURERS_MFR_NAME));
							}
							map.put("flag", false);
							//如果原制造商物料编号和传过来的制造商物料编号不一致，则获取新的制造商物料
							if (!partnum.equals(oldmpn))
							{
								Map<Integer, String> params = new HashMap<Integer, String>();
								params.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_NAME, oldmfn);
								params.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_PART_NUMBER, partnum);
								IManufacturerPart mfrp = (IManufacturerPart) session.getObject(mfrp_api, params);
								if (null == mfrp)
								{
									mfrp = (IManufacturerPart) session.createObject(mfrp_api, params);
								}
								map.put("mfrp", mfrp);
								map.put("flag", true);
							}
							map.put("fileurls", fileurl);
							list.add(map);
						} 
						catch (Exception e) 
						{
							jsonMsg.put("success", false);
							jsonMsg.put("changenum", number);
							jsonMsg.put("msg", "ItemId：" + itemid + "  " + e.getMessage());
							log.error(e.getMessage(), e);
							insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
							return jsonMsg.toString();
						}
					}
					
					//获取表单自动编码
					for (int i = 0; i < numSource.length; i++) 
					{
						if (numSource[i].getName().startsWith("Please")) continue;
						number = numSource[i].getNextNumber();
					}
					
					change = (IChange) session.createObject(sa_api, number);
					
					//设置表单基本属性
					IUser user = (IUser) session.getObject(UserConstants.CLASS_USER_BASE_CLASS, creator);
					if (null != user)
					{
						change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR).setValue(user);  //把获取到的User对象放进发起人
					}
					else
					{
						jsonMsg.put("success", false);
						jsonMsg.put("msg", "用户" + creator + "在Agile系统中不存在");
						insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
						return jsonMsg.toString();
					}
					Map<Integer, Object> params = new HashMap<Integer, Object>();
					params.put(ChangeConstants.ATT_COVER_PAGE_DESCRIPTION_OF_CHANGE, changedesc);
					params.put(ChangeConstants.ATT_COVER_PAGE_REASON_FOR_CHANGE, changereason);
					params.put(ChangeConstants.ATT_COVER_PAGE_PRODUCT_LINES, productline);
					params.put(1539, parttype);
					params.put(1540, spcn);
					params.put(1541, eol);
					params.put(1567, "".equals(suggestion.toString()) ? "无" : suggestion.toString());
//					params.put(1568, "-");
					change.setValues(params);
					
					ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
					for (Map<String, Object> param : list) 
					{
						IItem item = (IItem) param.get("ItemId");
						IRow row = table.createRow(item);
						
						//为物料新版本加 0.1
						String oldrev = CellUtils.getCellStringValue(row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_OLD_REV));
						if ("Introductory".equals(oldrev))
						{
							oldrev = "0";
						}
						BigDecimal oldrev_d = new BigDecimal(oldrev);
						BigDecimal rev_added = new BigDecimal("0.1");
						double newrev = oldrev_d.add(rev_added).doubleValue();
						row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_NEW_REV).setValue(newrev);
						
						//如果EOL为Yes，则物料新生命周期都改为EOL，反之不变
						if ("YES".equals(eol))
						{
							row.getCell(ChangeConstants.ATT_AFFECTED_ITEMS_LIFECYCLE_PHASE).setValue("EOL");
						}

						
						//如果制造商物料变化了则修改，否则不做变动
						if ((boolean) param.get("flag"))
						{
							IManufacturerPart mfrp = (IManufacturerPart) param.get("mfrp");
							ITable mfrtable = item.getTable(ItemConstants.TABLE_REDLINEMANUFACTURERS);
							ITwoWayIterator mfriter = mfrtable.getTableIterator();
							while (mfriter.hasNext()) 
							{
								IRow mfrrow = (IRow) mfriter.next();
								mfrrow.getCell(ItemConstants.ATT_MANUFACTURERS_MFR_PART_NUMBER).setValue(mfrp.getName());
							}
						}
						
						ITable attachtable = ((IItem) row.getReferent()).getAttachments();
						String[] urls = param.get("fileurls").toString().replace(";", " ; ").split(";");
						
						//新的物料附件添加有三种类型：1、 规格书; 2、图纸; 3、 其他
						if (urls.length != 3)
						{
							session.disableAllWarnings();
							table.clear();
							change.delete();
							change.delete();
							session.enableAllWarnings();
							jsonMsg.put("success", false);
							jsonMsg.put("changenum", "");
							jsonMsg.put("msg", item.getName() + "的附件URL长度有误！");
							log.info(jsonMsg);
							insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
							return jsonMsg.toString();
						}
						
//						ITwoWayIterator attit = attachtable.getTableIterator();
//						Map<Integer, IRow> rowMap = new HashMap<Integer, IRow>();
//						while (attit.hasNext()) 
//						{//取得原有附件中的三种文件
//							IRow typerow = (IRow) attit.next();
//							String type = CellUtils.getCellStringValue(typerow.getCell(ItemConstants.ATT_ATTACHMENTS_ATTACHMENT_TYPE));
//							if (type.equals(type_standard))
//							{
//								rowMap.put(0, typerow);  //规格书
//							}
//							else if (type.equals(type_paper))
//							{
//								rowMap.put(1, typerow); //图纸
//							}
//							else if (type.equals(type_other))
//							{
//								rowMap.put(2, typerow); //其他
//							}
//						}
						
						//取文件并放到物料下面(PURWEB那边按顺序放置三种附件类型,Agile这边按顺序取：1、 规格书/承认书; 2、图纸; 3、 其他(SPCN文件))
						for (int i = 0; i < urls.length; i++) 
						{
							if (null == urls[i].trim() || "".equals(urls[i].trim())) continue;
							File file = FTPUtil.tempStore(tempath, urls[i].trim(), ftp);
							if (null == file) 
							{
								//如果出现错误则先清空受影响物料表，在删除表单
								session.disableAllWarnings();
								table.clear();
								change.delete();
								change.delete();
								session.enableAllWarnings();
								jsonMsg.put("success", false);
								jsonMsg.put("changenum", "");
								jsonMsg.put("msg", item.getName() + "文件在此路径：" + urls[i].trim() + "不存在!");
								log.info(jsonMsg);
								insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
								return jsonMsg.toString();
							}
//							if (rowMap.get(i) != null)
//							{
//								attachtable.removeRow(rowMap.get(i));   //移除该类型的原有附件
//							}
							IRow arow = attachtable.createRow(file);
							if (i == 0)
							{
								arow.getCell(ItemConstants.ATT_ATTACHMENTS_ATTACHMENT_TYPE).setValue(type_standard);
							}
							else if (i == 1)
							{
								arow.getCell(ItemConstants.ATT_ATTACHMENTS_ATTACHMENT_TYPE).setValue(type_paper);
							}
							else if (i == 2)
							{
								arow.getCell(ItemConstants.ATT_ATTACHMENTS_ATTACHMENT_TYPE).setValue(type_other);
							}
							if (file.exists()) file.delete();
						}
						
						count++;
					}
					
					ICell wfCell = change.getCell(ChangeConstants.ATT_COVER_PAGE_WORKFLOW);
					IAgileList value = (IAgileList)wfCell.getValue();
					
					//获取WorkFlow的选项值
					Object[] wfList = value.getChildren();
					if (wfList.length < 1) //流程没有选项
					{
						jsonMsg.put("success", false);
						jsonMsg.put("changenum", number);
						jsonMsg.put("msg", "该类型的表单没有关联流程,请联系Agile管理员!");
						log.info(jsonMsg);
						insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
						return jsonMsg.toString();
					}
					
					//提交流程(有自发动选择流程的Event)
					IWorkflow wf = change.getWorkflow();
					if (null == wf && wfList.length == 1)
					{
						//关联表单流程(两个流程时，需要手动关联)
						change.setValue(ChangeConstants.ATT_COVER_PAGE_WORKFLOW, ((IAgileList)wfList[0]).getValue().toString());
						wf = change.getWorkflow();
					}
					IStatus toStatus = wf.getStates()[1];
			        ISignoffReviewer[] approvers = change.getReviewers(toStatus, WorkflowConstants.USER_APPROVER);
			        ISignoffReviewer[] observers = change.getReviewers(toStatus, WorkflowConstants.USER_OBSERVER);
			        ISignoffReviewer[] acknowledgers = change.getReviewers(toStatus, WorkflowConstants.USER_ACKNOWLEDGER);
			        change.changeStatus(toStatus, false, "", false, false, Collections.EMPTY_LIST, Arrays.asList(approvers), Arrays.asList(observers), Arrays.asList(acknowledgers), false);
				} 
				catch (Exception e) 
				{
					if (e.getMessage().contains("Attachment Type"))
					{
						jsonMsg.put("success", true);
						jsonMsg.put("changenum", number);
						jsonMsg.put("msg", "流程下受影响物料的附件表中“Attachment Type”属性值需要补全");
						log.info(jsonObj);
						insertLogs(psmt, action, pcnInfo, number, true, jsonMsg.toString());
						return jsonMsg.toString();
					}
					e.printStackTrace();
					jsonMsg.put("success", false);
					if (!number.equals(""))
					{
						session.disableAllWarnings();
						ITable table = change.getTable(ChangeConstants.TABLE_AFFECTEDITEMS);
						table.clear();
						change.delete();
						change.delete();
						session.enableAllWarnings();
					}
					jsonMsg.put("changenum", "");
					jsonMsg.put("msg", "提交失败," + e.getMessage());
					log.error(e.getMessage(), e);
					insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
					return jsonMsg.toString();
				}		
			}
			else
			{
				jsonMsg.put("success", false);
				jsonMsg.put("changenum", "");
				jsonMsg.put("msg", "无法创建SA流程,Error: SA没有自动编码!");
				log.info(jsonMsg);
				insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			
			String successmsg = "成功创建SA流程:" + number + "并成功添加了" + count + "个受影响物料";
			jsonMsg.put("success", true);
			jsonMsg.put("changenum", number);
			jsonMsg.put("msg", successmsg);
			log.info(jsonMsg);
			insertLogs(psmt, action, pcnInfo, number, true, "");
			log.info("invoke createPCN method end!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			jsonMsg.put("success", false);
			jsonMsg.put("changenum", number);
			jsonMsg.put("msg", "提交失败," + e.getMessage());
			log.error(e.getMessage(), e);
			insertLogs(psmt, action, pcnInfo, number, false, jsonMsg.toString());
			return jsonMsg.toString();
		}
		finally
		{
			//关闭AgileSession和FTP
			if (session != null)
			{
				session.close();
			}
			FTPUtil.disConnection(ftp);
			if (psmt != null)
            {
                try
                {
                	psmt.close();
                }
                catch (SQLException e)
                {
                	log.error(e);
                    e.printStackTrace();
                }
            }
			if (conn != null)
			{
				try 
				{
					if (!conn.isClosed())
					{
						conn.close();
						log.info("成功关闭数据库连接!");
					}
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					log.error(e);
				}
			}
		}
		
		return jsonMsg.toString();
	}

	/**
	 * PUR集成，修改物料风险等级
	 * 
	 * @param riskInfo
	 * @return
	 * @author JGZ
	 * @date 2016年11月18日上午9:17:44
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public String updateRiskLevel(String username, String password, String riskInfo) throws Exception
	{
		log.info("invoke updateRiskLevel method begin!");
		log.info("RiskInfo: " + riskInfo);
		JSONArray jsonArray = JSONArray.fromObject(riskInfo);
		JSONObject jsonMsg = new JSONObject();
		IAgileSession session = null;
		int count = 0;
		PreparedStatement psmt = null;
		String action = "PUR2PLM_updateRiskLevel";
		try 
		{
			DBConnectionAgile dbConnection = new DBConnectionAgile();
			conn = dbConnection.getConnection();
			psmt = conn.prepareStatement(insertsql);
			session = AgileUtils.createAgileSession(agileurl, username, password);
			if (null == session)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "无法与Agile系统建立连接,请联系其管理员查看账号是否失效!");
				log.info(jsonMsg);
				insertLogs(psmt, action, riskInfo, "", false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			for (Iterator iterator = jsonArray.iterator(); iterator.hasNext();) 
			{
				JSONObject jsonObj = (JSONObject) iterator.next();
				String itemNumber = StringUtil.jsonToStr(jsonObj, "ItemNum");     //物料编号
				String riskLevel = StringUtil.jsonToStr(jsonObj, "RiskLevel");      //物料的风险等级
				
				try 
				{
					IItem item = (IItem) session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemNumber);
					if (null == item)
					{
						jsonMsg.put("success", false);
						jsonMsg.put("msg", itemNumber + "：" + "Agile系统中不存在此物料!");
						log.info(jsonMsg);
						insertLogs(psmt, action, riskInfo, "", false, jsonMsg.toString());
						return jsonMsg.toString();
					}
					//设置物料风险等级
					item.getCell(ItemConstants.ATT_PAGE_TWO_LIST02).setValue(riskLevel);
					count++;
				} 
				catch (APIException e) 
				{
					e.printStackTrace();
					jsonMsg.put("success", false);
					jsonMsg.put("msg", itemNumber + "：" + e.getMessage());
					log.error(e.getMessage(), e);
					insertLogs(psmt, action, riskInfo, "", false, jsonMsg.toString());
					return jsonMsg.toString();
				}
			}
			
			if (count == 0)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "没有符合条件的数据");
				log.info(jsonMsg);
				insertLogs(psmt, action, riskInfo, "", false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			String successmsg = "成功修改" + count + "个物料的风险等级";
			jsonMsg.put("success", true);
			jsonMsg.put("msg", successmsg);
			log.info(jsonMsg);
			insertLogs(psmt, action, riskInfo, "", true, "");
			log.info("invoke updateRiskLevel method end!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			jsonMsg.put("success", false);
			jsonMsg.put("msg", e.getMessage());
			log.error(e.getMessage(), e);
			insertLogs(psmt, action, riskInfo, "", false, jsonMsg.toString());
			return jsonMsg.toString();
		}
		finally
		{
			if (session != null)
			{
				session.close();
			}
			if (psmt != null)
            {
                try
                {
                	psmt.close();
                }
                catch (SQLException e)
                {
                	log.error(e);
                    e.printStackTrace();
                }
            }
			if (conn != null)
			{
				try 
				{
					if (!conn.isClosed())
					{
						conn.close();
						log.info("成功关闭数据库连接!");
					}
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					log.error(e);
				}
			}
		}
		
		return jsonMsg.toString();
	}

	/** 
	 * PUR集成，处理ROHS报告 
	 *  
	 * @param username
	 * @param password
	 * @return
	 * @author JGZ
	 * @date 2016年12月13日下午4:07:41
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public String uploadROSH(String username, String password, String rohsinfo) throws Exception
	{
		log.info("invoke uploadROSH method begin!");
		log.info("ROHSInfo: " + rohsinfo);
		JSONArray jsonArray = JSONArray.fromObject(rohsinfo);
		JSONObject jsonMsg = new JSONObject();
		FTPClient ftp = null;
		IAgileSession session = null;
		int count = 0;
		PreparedStatement psmt = null;
		String action = "PUR2PLM_uploadROSH";
		try 
		{
			DBConnectionAgile dbConnection = new DBConnectionAgile();
			conn = dbConnection.getConnection();
			psmt = conn.prepareStatement(insertsql);
			//开启AgileSession和FTP
			session = AgileUtils.createAgileSession(agileurl, username, password);
			if (null == session)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "无法与Agile系统建立连接,请联系其管理员查看账号是否失效!");
				log.info(jsonMsg);
				insertLogs(psmt, action, rohsinfo, "", false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			ftp = FTPUtil.getConnection();
			
			for (Iterator iterator = jsonArray.iterator(); iterator.hasNext();) 
			{
				JSONObject jsonObj = (JSONObject) iterator.next();
				String mfrname = StringUtil.jsonToStr(jsonObj, "MFR");
				String mfrpnum = StringUtil.jsonToStr(jsonObj, "MPN");
				String itemnum = StringUtil.jsonToStr(jsonObj, "ItemId");
				String fileurl = StringUtil.jsonToStr(jsonObj, "FileURL");
				String rohs = StringUtil.jsonToStr(jsonObj, "ROHS");
				String rohs_validdate = StringUtil.jsonToStr(jsonObj, "ROHS_ValidDate");
				String rohs_riskclass = StringUtil.jsonToStr(jsonObj, "ROHS_RiskClass");
				String rohs_reportnumber = StringUtil.jsonToStr(jsonObj, "ROHS_ReportNumber");
				
				try 
				{
					//获取制造商物料并在其附件表中添加RoHS附件
//					Map<Integer, String> params = new HashMap<Integer, String>();
//					params.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_NAME, mfrname);
//					params.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_PART_NUMBER,mfrpnum);
//					IManufacturerPart mfrp = (IManufacturerPart) session.getObject(mfrp_api, params);
//					if (null == mfrp)
//					{
//						jsonMsg.put("success", false);
//						jsonMsg.put("msg", "Agile系统中找不到此制造商物料:" + mfrpnum);
//						log.info(jsonMsg);
//						return jsonMsg.toString();
//					}
//					Map<String, IRow> attachmentmap =  new HashMap<String, IRow>();
//					ITable table = mfrp.getTable(ManufacturerPartConstants.TABLE_ATTACHMENTS);
//					ITwoWayIterator attiter = table.getTableIterator();
//					while (attiter.hasNext()) 
//					{
//						IRow row = (IRow) attiter.next();
//						String filename = CellUtils.getCellStringValue(row.getCell(ManufacturerPartConstants.ATT_ATTACHMENTS_FILE_NAME));
//						attachmentmap.put(filename, row);
//					}
//					String[] urls = fileurl.split(";");
//					//上传文件，如有附件名一样的，则移除原来的附件
//					for (String url : urls) 
//					{
//						if ("".equals(url.trim())) continue;
//						File file = FTPUtil.tempStore(tempath, url.trim(), ftp);
//						if (null == file) 
//						{
//							jsonMsg.put("success", false);
//							jsonMsg.put("msg", "Agile系统中找不到此路径的文件:" + url);
//							log.info(jsonMsg);
//							return jsonMsg.toString();
//						}
//						String filename = url.split("/")[url.split("/").length - 1];
//						//找不到同名文件则直接上传，反之移除原来的再上传
//						if (attachmentmap.get(filename) == null)
//						{
//							table.createRow(file);
//						}
//						else
//						{
//							IRow row = attachmentmap.get(filename);
//							table.removeRow(row);
//							table.createRow(file);
//						}
//						if (file.exists()) file.delete();
//					}
					
					//获取物料并设置其ROHS属性值
					IItem item = (IItem) session.getObject(ItemConstants.CLASS_PARTS_CLASS, itemnum);
					if (null == item)
					{
						jsonMsg.put("success", false);
						jsonMsg.put("msg", "Agile系统中找不到此物料:" + itemnum);
						log.info(jsonMsg);
						insertLogs(psmt, action, rohsinfo, "", false, jsonMsg.toString());
						return jsonMsg.toString();
					}
					else
					{
						//设置物料的ROHS属性值
						Map<Integer, Object> param = new HashMap<Integer, Object>();
						param.put(ItemConstants.ATT_PAGE_TWO_LIST12, rohs);
						param.put(ItemConstants.ATT_PAGE_TWO_DATE02, StringUtil.strToDate(rohs_validdate));
						param.put(ItemConstants.ATT_PAGE_TWO_LIST14, rohs_riskclass);
						param.put(ItemConstants.ATT_PAGE_TWO_TEXT12, rohs_reportnumber);
//						item.getCell(ItemConstants.ATT_PAGE_TWO_LIST12).setValue(rohs);
//						item.getCell(ItemConstants.ATT_PAGE_TWO_DATE02).setValue(StringUtil.strToDate(rohs_validdate));
//						item.getCell(ItemConstants.ATT_PAGE_TWO_LIST14).setValue(rohs_riskclass);
//						item.getCell(ItemConstants.ATT_PAGE_TWO_TEXT12).setValue(rohs_reportnumber);
						item.setValues(param);
						count++;
					}
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
					jsonMsg.put("success", false);
					jsonMsg.put("msg", mfrpnum + e.getMessage());
					log.error(e.getMessage(), e);
					insertLogs(psmt, action, rohsinfo, "", false, jsonMsg.toString());
					return jsonMsg.toString();
				}
			}
			
			String successmsg = "成功提交了" + count + "条数据"; 
			if (count == 0)
			{
				jsonMsg.put("success", false);
				jsonMsg.put("msg", "没有符合条件的数据");
				insertLogs(psmt, action, rohsinfo, "", false, jsonMsg.toString());
				return jsonMsg.toString();
			}
			jsonMsg.put("success", true);
			jsonMsg.put("msg", successmsg);
			log.info(jsonMsg);
			insertLogs(psmt, action, rohsinfo, "", true, "");
			log.info("invoke uploadROSH method end!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			jsonMsg.put("success", false);
			jsonMsg.put("msg", "提交失败" + e.getMessage());
			log.error(e.getMessage(), e);
			return jsonMsg.toString();
		}
		finally
		{
			if (session != null)
			{
				session.close();
			}
			FTPUtil.disConnection(ftp);
			if (psmt != null)
            {
                try
                {
                	psmt.close();
                }
                catch (SQLException e)
                {
                	log.error(e);
                    e.printStackTrace();
                }
            }
			if (conn != null)
			{
				try 
				{
					if (!conn.isClosed())
					{
						conn.close();
						log.info("成功关闭数据库连接!");
					}
				} 
				catch (SQLException e) 
				{
					e.printStackTrace();
					log.error(e);
				}
			}
		}
		
		return jsonMsg.toString();
	}
	/**
     * @desc: 根据 之前 的替代关系获取物料的所有替代关系物料
     * (包括：物料级替代、BOM级替代、单向替代、单向时效性替代)
     * @param item
     * @return
     * @throws Exception
     * @author: fionn
     */
    public static Set<IItem> getReplacementsByRelation(IItem item, IRow bomrow) throws Exception{
        String deny = CellUtils.getStringByBaseID(bomrow, 1638);
        Set<IItem> replaces = new HashSet<IItem>();
        replaces.addAll(getReplacementsLevel2(bomrow));
        if(!"Yes".equalsIgnoreCase(deny)) {
            replaces.addAll(getReplacementsLevel1(item));
            replaces.addAll(getReplacementsLevel3(item));
            replaces.addAll(getReplacementsLevel4(item));
        }
        return replaces;
    }
    
    /**
     * @desc: 获取item 物料级替代料
     * Relationship页签中，物料类型的属于物料级替代料(不含TXP和特殊物料）
     * @param item
     * @return
     * @throws Exception
     * @author: fionn
     */
    public static Set<IItem> getReplacementsLevel1(IItem item) throws Exception{
        Set<IItem> replaces = new HashSet<IItem>();
        String sql = "["+ItemConstants.ATT_RELATIONSHIPS_TYPE+"] Not In (%0)";
        ITable tbl = item.getRelationship().where(sql, new Object[] {"TXP"});
        ITwoWayIterator it = tbl.getTableIterator();
        while(it.hasNext()) {
            IRow row = (IRow)it.next();
            IDataObject obj = row.getReferent();
            if(!obj.getAgileClass().isSubclassOf(ItemConstants.CLASS_PARTS_CLASS) 
                    || IGNORE_ITEMS.contains(obj.getName())) continue;
            replaces.add((IItem)obj);
        }
        return replaces;
    }
    
    /**
     * @desc: 物料BOM级替代
     *
     * @param item
     * @return
     * @throws Exception
     * @author: fionn
     */
    public static Set<IItem> getReplacementsLevel2(IRow bomrow) throws Exception{
        String repitems = CellUtils.getStringByBaseID(bomrow, REPLACEMENT_ITEMS_IN_BOM);
        if("".equals(repitems)||repitems.trim().length()<9) return Collections.EMPTY_SET;
        Set<IItem> replaces = new HashSet<IItem>();
        IAgileSession session = bomrow.getSession();
        String[] numbers = repitems.replaceAll("[；:,]", ";").split(";");
        if(numbers.length==0){
            return Collections.EMPTY_SET;
        }
        for(String number : numbers) {
            IItem item = (IItem) session.getObject(IItem.OBJECT_TYPE, number);
            if(item == null || !item.getAgileClass().isSubclassOf(ItemConstants.CLASS_PARTS_CLASS) 
                    || IGNORE_ITEMS.contains(item.getName())) continue;
            replaces.add(item);
        }
        return replaces;
    }
    
    /**
     * @desc: 物料级 单向替代
     *
     * @param item
     * @return
     * @throws Exception
     * @author: fionn
     */
    public static Set<IItem> getReplacementsLevel3(IItem item) throws Exception{
        Set<IItem> replaces = new HashSet<IItem>();
        Set<Object> repItems = CellUtils.getCellListValues(item.getCell(2090));
        for(Object obj : repItems) {
            IItem ritem = (IItem) obj;
            ritem = (IItem)ritem.getSession().getObject(IItem.OBJECT_TYPE, ritem.getName());
            if(!ritem.getAgileClass().isSubclassOf(ItemConstants.CLASS_PARTS_CLASS) 
                    || IGNORE_ITEMS.contains(ritem.getName())) continue;
            replaces.add(ritem);
        }
        return replaces;
    }
    
    /**
     * @desc: 物料级单向（时效性）替代
     *
     * @param item
     * @return
     * @throws Exception
     * @author: fionn
     */
    public static Set<IItem> getReplacementsLevel4(IItem item) throws Exception{
        Set<IItem> replaces = new HashSet<IItem>();
        
        return replaces;
    }
    
    /**
     * 
     * 数据库有重复数据返回true，否则返回false
     * 
     * @param itemNum
     * @param itemRev
     * @param filename
     * @param foldernum
     * @param folderrev
     * @return
     * @throws SQLException
     * @author zhangguoli
     * @date 2016年12月20日 上午9:58:41
     */
    public boolean isSameDate(String itemNum, String itemRev, String filename, String foldernum, String folderrev)
        throws SQLException
    {
        boolean isSame = false;
        
        this.psf.setString(1, itemNum);
        this.psf.setString(2, itemRev);
        this.psf.setString(3, filename);
        this.psf.setString(4, foldernum);
        this.psf.setString(5, folderrev);
        ResultSet rs = this.psf.executeQuery();
        int count = 0;
        while (rs.next())
        {
            count = rs.getInt(1);
        }
        if (count >= 1)
        {
            isSame = true;
        }
        return isSame;
    }
    
    /**
     * 
     * 获取邮箱
     * 
     * @param change
     * @return
     * @throws APIException
     * @author zhangguoli
     * @date 2016年12月22日 下午4:09:00
     */
    public String getEmail(IChange change)
        throws APIException
    {
        String email = null;
        ICell cell = change.getCell(ChangeConstants.ATT_COVER_PAGE_ORIGINATOR);
        if (null != cell)
        {
            IUser user = (IUser)cell.getReferent();
            email = CellUtils.getCellStringValue(user.getCell(UserConstants.ATT_GENERAL_INFO_EMAIL), tZone);
        }
        return email;
    }
    /**
     * 按照编号排序
     *  
     * @param set
     * @return
     * @throws APIException
     * @author guolizhang
     * @date 2017年4月18日上午11:33:54
     */
    public List<IItem> sortByName(Set<IItem> set) throws APIException{
        List<IItem> list = new ArrayList<IItem>();
        Map<String, IItem> map = new TreeMap<String, IItem>();
        for (IItem item : set)
        {
            String number = item.getName();
            map.put(number, item);
        }       
        for (String key : map.keySet()) {  
            list.add(map.get(key));
//            System.out.println(key);
            }
        return list;
    }
    /**
     * 
     * 获取写入次数
     * 
     * @param conn
     * @param sql
     * @param query
     * @param changeNum
     * @return
     * @throws SQLException
     * @author zhangguoli
     * @date 2016年9月29日 下午6:39:42
     */
    private String writeTime(String changeNum,String bomver,String modelNo)
        throws SQLException
    {
        this.psq.setString(1, changeNum);
        this.psq.setString(2, bomver);
        this.psq.setString(3, modelNo);
        ResultSet rs = this.psq.executeQuery();
        int count = 0;
        while (rs.next())
        {
            count = rs.getInt(1);
        }
        return String.valueOf(++count);
    }
    /**
     * 插入集成的运行结果
     *  
     * @param changeNum
     * @param xml
     * @param action
     * @param isSuccess
     * @param msg
     * @throws SQLException
     * @author guolizhang
     * @date 2017年4月17日下午2:59:23
     */
    public void insertInfo(String changeNum,String xml,String action,String isSuccess,String msg) throws SQLException{
        insertPS = connlog.prepareStatement(insertResultInfo);
        insertPS.setString(1, "PLM2MES");
        insertPS.setString(2, this.getLocalTime(tZone));
        insertPS.setString(3, changeNum);
        insertPS.setString(4, action);
        insertPS.setString(5, xml);
        insertPS.setString(6, isSuccess);
        insertPS.setString(7, msg);
        insertPS.executeUpdate();
    }
}
