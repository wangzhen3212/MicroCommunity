package com.java110.utils.util;

import junit.framework.TestCase;
import org.dom4j.Element;

public class XmlUtilTest extends TestCase {

    public void testParseNormalXml() {
        String xml = "<xml><ToUserName><![CDATA[gh_xxx]]></ToUserName><Content>hello</Content></xml>";
        Element root = XmlUtil.parse(xml);
        assertEquals("gh_xxx", root.elementText("ToUserName"));
        assertEquals("hello", root.elementText("Content"));
    }

    public void testParseXxePayload() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<!DOCTYPE xml [<!ENTITY xxe SYSTEM \"file:///c:/windows/win.ini\">]>"
                + "<xml><Content>&xxe;</Content></xml>";
        try {
            XmlUtil.parse(xml);
            fail("存在 DOCTYPE 声明的报文必须解析失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("报文格式错误"));
        }
    }

    public void testParseEmptyXml() {
        try {
            XmlUtil.parse("");
            fail("空报文必须解析失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("不能为空"));
        }
    }

    public void testParseOverLengthXml() {
        StringBuilder xml = new StringBuilder("<xml><Content>");
        for (int i = 0; i < XmlUtil.MAX_XML_LENGTH; i++) {
            xml.append("a");
        }
        xml.append("</Content></xml>");
        try {
            XmlUtil.parse(xml.toString());
            fail("超长报文必须解析失败");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("超长"));
        }
    }
}
