/**
 * 停车场区域保存命令类
 * 
 * 该类负责处理停车场区域的新增操作，包括停车场基本信息保存和属性信息保存
 * 通过注解@Java110Cmd声明服务代码，实现Cmd接口完成命令处理流程
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.parkingArea;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.intf.community.IParkingAreaAttrV1InnerServiceSMO;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.po.parking.ParkingAreaPo;
import com.java110.po.parking.ParkingAreaAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "parkingArea.saveParkingArea")
public class SaveParkingAreaCmd extends Cmd {

    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    @Autowired
    private IParkingAreaAttrV1InnerServiceSMO parkingAreaAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，包括必填字段检查和属性值校验
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "num", "必填，请填写停车场编号");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        Assert.hasKeyAndValue(reqJson, "typeCd", "必填，请选择停车场类型");
        // 校验属性值是否符合规范
        Assert.judgeAttrValue(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 执行业务逻辑，包括停车场信息保存和属性信息处理
     * 使用@Java110Transactional注解确保事务一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务执行失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 保存停车场基本信息
        addParkingArea(reqJson);
        // 处理停车场属性信息
        dealAttr(reqJson);
    }

    /**
     * 添加停车场信息
     * 
     * 保存停车场基本信息，包括编号校验、生成主键和数据库插入操作
     * 
     * @param paramInJson 输入参数JSON对象，包含停车场相关信息
     * @throws CmdException 当保存失败或编号重复时抛出异常
     */
    public void addParkingArea(JSONObject paramInJson) {
        // 创建停车场DTO对象用于查询校验
        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setNum(paramInJson.getString("num"));
        parkingAreaDto.setCommunityId(paramInJson.getString("communityId"));
        
        // 查询是否已存在相同编号的停车场
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaV1InnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
        // 校验停车场编号是否重复
        Assert.listIsNull(parkingAreaDtos, "停车场编号重复，请重新添加！");
        
        // 准备业务数据
        JSONObject businessParkingArea = new JSONObject();
        businessParkingArea.putAll(paramInJson);
        // 生成停车场主键ID
        businessParkingArea.put("paId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_paId));
        
        // 转换JSON对象为PO对象
        ParkingAreaPo parkingAreaPo = BeanConvertUtil.covertBean(businessParkingArea, ParkingAreaPo.class);
        // 调用服务保存停车场信息
        int flag = parkingAreaV1InnerServiceSMOImpl.saveParkingArea(parkingAreaPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存停车场失败");
        }
        // 将生成的paId设置回参数对象，供后续使用
        paramInJson.put("paId", businessParkingArea.getString("paId"));
    }

    /**
     * 处理停车场属性信息
     * 
     * 保存停车场的扩展属性信息，支持多个属性的批量保存
     * 
     * @param paramObj 参数JSON对象，包含属性数组attrs
     * @throws CmdException 当属性保存失败时抛出异常
     */
    private void dealAttr(JSONObject paramObj) {
        // 检查是否存在属性信息
        if (!paramObj.containsKey("attrs")) {
            return;
        }

        // 获取属性数组
        JSONArray attrs = paramObj.getJSONArray("attrs");
        if (attrs.size() < 1) {
            return;
        }

        JSONObject attr = null;
        int flag = 0;
        // 遍历所有属性并逐个保存
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 设置属性关联信息
            attr.put("communityId", paramObj.getString("communityId"));
            attr.put("paId", paramObj.getString("paId"));
            // 生成属性主键ID
            attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
            
            // 转换属性对象并保存
            ParkingAreaAttrPo parkingAreaAttrPo = BeanConvertUtil.covertBean(attr, ParkingAreaAttrPo.class);
            flag = parkingAreaAttrV1InnerServiceSMOImpl.saveParkingAreaAttr(parkingAreaAttrPo);
            
            // 检查属性保存结果
            if (flag < 1) {
                throw new CmdException("保存停车场失败");
            }
        }
    }
}