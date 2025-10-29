package com.java110.community.cmd.parkingArea;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IParkingAreaAttrV1InnerServiceSMO;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.po.parking.ParkingAreaPo;
import com.java110.po.parking.ParkingAreaAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 更新停车场信息命令类
 * 
 * 该类负责处理停车场信息的更新操作，包括停车场基本信息的更新和停车场属性的批量更新
 * 通过@Java110Cmd注解声明服务代码，用于路由到对应的命令处理器
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "parkingArea.updateParkingArea")
public class UpdateParkingAreaCmd extends Cmd {

    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    @Autowired
    private IParkingAreaAttrV1InnerServiceSMO parkingAreaAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求的JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "paId", "停车场ID不能为空");
        Assert.hasKeyAndValue(reqJson, "num", "必填，请填写停车场编号");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        Assert.hasKeyAndValue(reqJson, "typeCd", "必填，请选择停车场类型");

        // 验证属性值是否符合规范
        Assert.judgeAttrValue(reqJson);
    }

    /**
     * 执行更新停车场信息命令
     * 
     * 该方法负责更新停车场的基本信息和属性信息，采用事务保证数据一致性
     * 如果属性信息不存在则创建，存在则更新
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建停车场业务对象并复制请求参数
        JSONObject businessParkingArea = new JSONObject();
        businessParkingArea.putAll(reqJson);
        
        // 将JSON对象转换为停车场POJO对象
        ParkingAreaPo parkingAreaPo = BeanConvertUtil.covertBean(businessParkingArea, ParkingAreaPo.class);
        
        // 更新停车场基本信息
        int flag = parkingAreaV1InnerServiceSMOImpl.updateParkingArea(parkingAreaPo);
        if (flag < 1) {
            throw new CmdException("保存停车场失败");
        }
        
        // 检查请求中是否包含属性信息，如果没有则直接返回
        if (!reqJson.containsKey("attrs")) {
            return;
        }

        // 获取属性数组
        JSONArray attrs = reqJson.getJSONArray("attrs");
        if (attrs == null || attrs.size() < 1) {
            return;
        }

        // 遍历处理每个属性
        JSONObject attr = null;
        ParkingAreaAttrPo parkingAreaAttrPo = null;
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 设置小区ID到属性中
            attr.put("communityId", reqJson.getString("communityId"));
            
            // 判断属性ID是否存在或为临时ID，如果不存在则创建新属性
            if (!attr.containsKey("attrId") || attr.getString("attrId").startsWith("-") || StringUtil.isEmpty(attr.getString("attrId"))) {
                // 生成新的属性ID
                attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                // 转换为属性POJO对象
                parkingAreaAttrPo = BeanConvertUtil.covertBean(attr, ParkingAreaAttrPo.class);
                // 设置停车场ID
                parkingAreaAttrPo.setPaId(parkingAreaPo.getPaId());
                // 保存新属性
                flag = parkingAreaAttrV1InnerServiceSMOImpl.saveParkingAreaAttr(parkingAreaAttrPo);
                if (flag < 1) {
                    throw new CmdException("保存停车场失败");
                }
                continue;
            }
            
            // 更新已存在的属性
            parkingAreaAttrPo = BeanConvertUtil.covertBean(attr, ParkingAreaAttrPo.class);
            flag = parkingAreaAttrV1InnerServiceSMOImpl.updateParkingAreaAttr(parkingAreaAttrPo);
            if (flag < 1) {
                throw new CmdException("保存停车场失败");
            }
        }
    }
}