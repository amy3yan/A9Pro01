/*******************************************************************************
 * @project: BYD-Div5-PX
 * @package: com.hand.agile.utils
 * @file: MailUtils.java
 * @author: fionn
 * @created: 2016年4月22日
 * @purpose:
 * 
 * @version: 1.0
 * 
 * 
 * Copyright 2016 HAND All rights reserved.
 ******************************************************************************/
package com.hand.utils;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.log4j.Logger;

/**
 * @author fionn
 *
 */
public class MailUtils {

    private static Logger log = Logger.getLogger(MailUtils.class);
    private String server;
    private String user;
    private String password;
    private String from;
    private Transport transport;
    private MimeMessage message;
    /**
     * 
     */
    public MailUtils() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param server
     * @param user
     * @param password
     * @param from
     * @throws MessagingException 
     */
    public MailUtils(String server, String user, String password, String from) throws MessagingException {
        super();
        this.server = server;
        this.user = user;
        this.password = password;
        this.from = from;
        init();
    }

    /**
     * 发送邮件
     * @param subject： 邮件主题
     * @param content： 邮件正文html格式
     * @throws MessagingException
     */
    public void send(String subject, String content) throws MessagingException{
        log.debug("发送邮件：" + message);
        try {
            message.setSubject(MimeUtility.encodeText(subject, "GB2312", "B"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        BodyPart bodypart = new MimeBodyPart();
        bodypart.setContent(content, "text/html;charset=gb2312");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodypart); // 将含有信件内容的BodyPart加入到MimeMulitipart对象中
        message.setContent(multipart);
        message.saveChanges();
        transport.send(message, message.getAllRecipients());
    }
    
    /**
     * 设置Email地址
     * @param to
     * @param cc
     * @param bcc
     * @throws MessagingException
     */
    public void setEmailAddress(String to, String cc, String bcc) throws MessagingException {
        setEmailAddress(message, RecipientType.TO, to);
        setEmailAddress(message, RecipientType.CC, cc);
        setEmailAddress(message, RecipientType.BCC, bcc);
    }

    /**
     * 关闭邮件连接
     */
    public void close() {
        if(transport != null) {
            try {
                transport.close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 初始化
     * @throws MessagingException
     */
    private void init() throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", server);
        prop.put("mail.smtp.auth", "true");
        Session session = Session.getInstance(prop, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
        session.setDebug(false);
        message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        transport = session.getTransport("smtp");
        transport.connect(server, user, password);
    }
    
    /**
     * 设置Email地址，如发送/抄送
     * @param message
     * @param type
     * @param addresses
     * @throws MessagingException
     */
    private void setEmailAddress(MimeMessage message, Message.RecipientType type, String addresses) throws MessagingException {
        if(addresses == null || "".equals(addresses)) return;
        String[] emails = addresses.split(",");
        Set<String> emailLst = new HashSet<String>();
        for(String email : emails) {
            if("".equals(email) || email == null) continue;
            emailLst.add(email);
        }
        InternetAddress[] mailAddrs = new InternetAddress[emailLst.size()];
        int i = 0;
        for (String email : emailLst) {
            mailAddrs[i] = new InternetAddress(email);
            i++;
        }
        message.setRecipients(type, mailAddrs);
    }
    
    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(String from) {
        this.from = from;
    }
    
    
}
