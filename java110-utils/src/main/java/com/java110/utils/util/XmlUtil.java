package com.java110.utils.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;

/**
 * 安全 XML 解析工具
 * <p>
 * 统一关闭 DOCTYPE 声明、外部通用实体、外部参数实体与外部 DTD，避免 XXE 任意文件读取
 * 与实体膨胀攻击；所有解析入口请使用本工具，不要直接使用 DocumentHelper.parseText
 */
public final class XmlUtil {

    private static final Logger logger = LogManager.getLogger(XmlUtil.class);

    /**
     * xml 报文最大长度 64KB
     */
    public static final int MAX_XML_LENGTH = 64 * 1024;

    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private XmlUtil() {
    }

    /**
     * 安全解析 xml 报文，返回根节点
     *
     * @param xml xml 报文
     * @return 根节点
     */
    public static Element parse(String xml) {
        return parse(xml, MAX_XML_LENGTH);
    }

    /**
     * 安全解析 xml 报文，返回根节点
     *
     * @param xml          xml 报文
     * @param maxXmlLength 报文最大长度
     * @return 根节点
     */
    public static Element parse(String xml, int maxXmlLength) {
        if (StringUtil.isNullOrNone(xml)) {
            throw new IllegalArgumentException("xml 报文不能为空");
        }
        if (xml.length() > maxXmlLength) {
            throw new IllegalArgumentException("xml 报文超长");
        }
        if (hasDoctypeDecl(xml)) {
            throw new IllegalArgumentException("xml 报文格式错误");
        }
        SAXReader reader = new SAXReader();
        setFeature(reader, FEATURE_DISALLOW_DOCTYPE, true);
        setFeature(reader, FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
        setFeature(reader, FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
        setFeature(reader, FEATURE_LOAD_EXTERNAL_DTD, false);
        setFeature(reader, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        try {
            return reader.read(new StringReader(xml)).getRootElement();
        } catch (DocumentException e) {
            throw new IllegalArgumentException("xml 报文格式错误", e);
        }
    }

    /**
     * 创建禁用外部实体与 DOCTYPE 的 w3c Document，用于 xml 报文组装
     *
     * @return Document
     * @throws ParserConfigurationException 解析器配置异常
     */
    public static org.w3c.dom.Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        setFeature(factory, FEATURE_DISALLOW_DOCTYPE, true);
        setFeature(factory, FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
        setFeature(factory, FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
        setFeature(factory, FEATURE_LOAD_EXTERNAL_DTD, false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    private static boolean hasDoctypeDecl(String xml) {
        String lowerXml = xml.toLowerCase();
        return lowerXml.contains("<!doctype") || lowerXml.contains("<!entity");
    }

    private static void setFeature(SAXReader reader, String feature, boolean value) {
        try {
            reader.setFeature(feature, value);
        } catch (SAXException e) {
            logger.warn("设置 xml 解析安全特性失败，feature={}，error={}", feature, e.getMessage());
        }
    }

    private static void setFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException e) {
            logger.warn("设置 xml 解析安全特性失败，feature={}，error={}", feature, e.getMessage());
        }
    }
}
