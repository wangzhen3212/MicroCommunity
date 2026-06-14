package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeConfigAttrV1InnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeConfigV1InnerServiceSMO;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.po.tempCarFee.TempCarFeeConfigPo;
import com.java110.po.tempCarFee.TempCarFeeConfigAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 删除临时车费配置命令类
 * 
 * 该类负责处理删除临时车费配置的请求，包括删除临时车费配置信息、相关属性配置以及费用配置项
 * 使用事务保证数据一致性，确保所有相关数据都能被正确删除
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode fee.deleteTempCarFeeConfig
 */
@Java110Cmd(serviceCode = "fee.deleteTempCarFeeConfig")
public class DeleteTempCarFeeConfigCmd extends Cmd {

    /**
     * 临时车费配置服务接口
     */
    @Autowired
    private ITempCarFeeConfigV1InnerServiceSMO tempCarFeeConfigV1InnerServiceSMOImpl;

    /**
     * 临时车费配置属性服务接口
     */
    @Autowired
    private ITempCarFeeConfigAttrV1InnerServiceSMO tempCarFeeConfigAttrV1InnerServiceSMOImpl;

    /**
     * 费用配置服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作有足够的信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含configId字段
        Assert.hasKeyAndValue(reqJson, "configId", "configId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 执行删除临时车费配置命令
     * 
     * 该方法使用事务管理，确保删除操作的原子性
     * 主要执行以下操作：
     * 1. 删除临时车费配置主记录
     * 2. 删除相关的临时车费配置属性记录
     * 3. 删除对应的费用配置项
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为临时车费配置PO对象
        TempCarFeeConfigPo tempCarFeeConfigPo = BeanConvertUtil.covertBean(reqJson, TempCarFeeConfigPo.class);
        
        // 删除临时车费配置主记录
        int flag = tempCarFeeConfigV1InnerServiceSMOImpl.deleteTempCarFeeConfig(tempCarFeeConfigPo);
        
        // 检查删除操作是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("删除临时车费失败");
        }
        
        // 获取临时车费配置属性列表
        JSONArray attrs = reqJson.getJSONArray("tempCarFeeConfigAttrs");
        
        // 如果存在属性配置，则遍历删除所有相关属性记录
        if (attrs != null && attrs.size() > 0) {
            JSONObject attr = null;
            // 遍历所有属性配置
            for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
                attr = attrs.getJSONObject(attrIndex);
                // 设置属性关联的规则ID和小区ID
                attr.put("ruleId", reqJson.getString("ruleId"));
                attr.put("communityId", reqJson.getString("communityId"));
                
                // 跳过无效的属性ID（不存在、以-开头或为空）
                if (!attr.containsKey("attrId") || attr.getString("attrId").startsWith("-") || StringUtil.isEmpty(attr.getString("attrId"))) {
                    continue;
                }
                
                // 将属性JSON转换为PO对象并删除
                TempCarFeeConfigAttrPo tempCarFeeConfigAttrPo = BeanConvertUtil.covertBean(attr, TempCarFeeConfigAttrPo.class);
                flag = tempCarFeeConfigAttrV1InnerServiceSMOImpl.deleteTempCarFeeConfigAttr(tempCarFeeConfigAttrPo);
                
                // 检查属性删除是否成功
                if (flag < 1) {
                    throw new CmdException("删除临时车费失败");
                }
            }
        }
        
        // 创建费用配置PO对象并设置删除状态
        PayFeeConfigPo payFeeConfigPo = new PayFeeConfigPo();
        payFeeConfigPo.setConfigId(reqJson.getString("feeConfigId"));
        payFeeConfigPo.setStatusCd("1"); // 设置状态为删除状态
        
        // 删除费用配置项
        int i = feeConfigInnerServiceSMOImpl.deleteFeeConfig(payFeeConfigPo);
        
        // 检查费用配置项删除是否成功
        if (i < 1) {
            throw new CmdException("删除费用项失败");
        }
    }
}