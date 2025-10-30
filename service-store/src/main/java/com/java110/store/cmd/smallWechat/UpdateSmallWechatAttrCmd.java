/**
 * 小程序微信属性更新命令类
 * 
 * 该类负责处理小程序微信属性更新的业务逻辑，包括参数验证和数据更新操作。
 * 通过注解@Java110Cmd声明服务代码为"smallWechat.updateSmallWechatAttr"，
 * 当接收到对应服务代码的请求时，会执行该命令类中的validate和doCmd方法。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.ISmallWechatAttrV1InnerServiceSMO;
import com.java110.po.wechat.SmallWechatAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "smallWechat.updateSmallWechatAttr")
public class UpdateSmallWechatAttrCmd extends Cmd {

    /**
     * 小程序微信属性服务接口
     * 用于调用小程序微信属性的数据操作
     */
    @Autowired
    private ISmallWechatAttrV1InnerServiceSMO smallWechatAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象，包含请求的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证attrId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "attrId", "attrId不能为空");
        // 验证specCd参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "specCd", "请求报文中未包含specCd");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证value参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "value", "请求报文中未包含value");
    }

    /**
     * 命令执行方法
     * 
     * 执行小程序微信属性的更新操作，将请求数据转换为实体对象并调用服务层进行数据更新
     * 
     * @param event 命令事件对象，包含请求的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当数据更新失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求的JSON数据转换为SmallWechatAttrPo实体对象
        SmallWechatAttrPo smallWechatAttrPo = BeanConvertUtil.covertBean(reqJson, SmallWechatAttrPo.class);
        
        // 调用服务层接口更新小程序微信属性数据
        int flag = smallWechatAttrV1InnerServiceSMOImpl.updateSmallWechatAttr(smallWechatAttrPo);

        // 检查更新操作是否成功，flag小于1表示更新失败
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        context.setResponseEntity(ResultVo.success());
    }
}