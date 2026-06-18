package com.java110.store.cmd.store;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreAttrDto;
import com.java110.intf.store.IStoreAttrInnerServiceSMO;
import com.java110.intf.store.IStoreAttrV1InnerServiceSMO;
import com.java110.po.store.StoreAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 更新店铺属性命令类
 * 
 * 该类负责处理店铺属性的更新操作，通过接收前端请求数据，
 * 验证必要参数后执行店铺属性信息的更新操作
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode storeAttr.updateStoreAttr
 */
@Java110Cmd(serviceCode = "storeAttr.updateStoreAttr")
public class UpdateStoreAttrCmd extends Cmd {

    /**
     * 店铺属性内部服务接口
     */
    @Autowired
    private IStoreAttrInnerServiceSMO storeAttrInnerServiceSMOImpl;

    /**
     * 店铺属性V1版本内部服务接口
     */
    @Autowired
    private IStoreAttrV1InnerServiceSMO storeAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的属性ID字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含attrId字段
        Assert.hasKeyAndValue(reqJson, "attrId", "attrId不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 根据属性ID查询店铺属性信息，并将请求参数合并后更新店铺属性
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建店铺属性数据传输对象
        StoreAttrDto storeAttrDto = new StoreAttrDto();
        // 设置要查询的属性ID
        storeAttrDto.setAttrId(reqJson.getString("attrId"));
        // 根据属性ID查询店铺属性列表
        List<StoreAttrDto> storeAttrDtos = storeAttrInnerServiceSMOImpl.queryStoreAttrs(storeAttrDto);

        // 验证查询结果必须只有一条记录
        Assert.listOnlyOne(storeAttrDtos, "未找到需要修改的活动 或多条数据");

        // 创建业务店铺属性JSON对象
        JSONObject businessStoreAttr = new JSONObject();
        // 将查询到的店铺属性信息转换为Map并放入业务对象
        businessStoreAttr.putAll(BeanConvertUtil.beanCovertMap(storeAttrDtos.get(0)));
        // 将请求参数合并到业务对象中，实现数据更新
        businessStoreAttr.putAll(reqJson);

        // 将业务JSON对象转换为店铺属性持久化对象
        StoreAttrPo storeAttrPo = BeanConvertUtil.covertBean(businessStoreAttr, StoreAttrPo.class);

        // 执行更新操作，返回影响的行数
        int flag = storeAttrV1InnerServiceSMOImpl.updateStoreAttr(storeAttrPo);

        // 验证更新操作是否成功
        if (flag < 1) {
            throw new CmdException("修改异常");
        }
    }
}