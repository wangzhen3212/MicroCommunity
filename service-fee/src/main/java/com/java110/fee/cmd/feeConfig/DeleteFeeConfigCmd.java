/**
 * 费用配置删除命令类
 * 
 * 该类负责处理删除费用配置的请求，包括参数验证和删除操作。
 * 使用@Java110Cmd注解标识为命令类，服务码为"feeConfig.deleteFeeConfig"。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.feeConfig;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "feeConfig.deleteFeeConfig")
public class DeleteFeeConfigCmd extends Cmd {

    /**
     * 支付费用配置V1版本内部服务接口
     */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /**
     * 费用配置内部服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证删除费用配置请求的必要参数，并检查是否为默认费用项。
     * 如果是默认费用项则不允许删除。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "configId", "费用项ID不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        
        // 创建费用配置查询对象
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        feeConfigDto.setIsDefault("T"); // 设置查询条件为默认费用项
        
        // 查询是否为默认费用项
        int feeCount = feeConfigInnerServiceSMOImpl.queryFeeConfigsCount(feeConfigDto);
        
        // 如果是默认费用项，则不允许删除
        if (feeCount > 0) {
            throw new IllegalArgumentException("该费用项目不能删除");
        }
    }

    /**
     * 执行删除费用配置命令
     * 
     * 将请求参数转换为费用配置PO对象，调用服务层进行删除操作。
     * 如果删除失败则抛出异常，成功则返回操作结果。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为费用配置PO对象
        PayFeeConfigPo payFeeConfigPo = BeanConvertUtil.covertBean(reqJson, PayFeeConfigPo.class);

        // 调用服务层删除费用配置
        int flag = payFeeConfigV1InnerServiceSMOImpl.deletePayFeeConfig(payFeeConfigPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除费用项失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}