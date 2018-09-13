package com.bzlink.webservice;
 
import java.io.IOException;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface BzService1
{
    // 下载二进制文件
    @WebMethod 
    public byte[] getFile(String itemNumber,String itemRev,String fileName) throws IOException;
    @WebMethod
    public String PLM2MES(String xml);
    @WebMethod
    public String updateMFR(String username, String password, String mfrInfo) throws Exception;
    @WebMethod
    public String createPCN(String username, String password, String pcnInfo) throws Exception;
    @WebMethod
    public String updateRiskLevel(String username, String password, String riskInfo) throws Exception;
    @WebMethod
    public String uploadROSH(String username, String password, String rohsinfo) throws Exception;
}
