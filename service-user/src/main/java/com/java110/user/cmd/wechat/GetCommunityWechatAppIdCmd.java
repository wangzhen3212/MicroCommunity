package com.java110.user.cmd.wechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.WechatConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 获取小区微信AppId命令类
 * 该命令用于根据小区ID和对象类型获取对应的微信小程序AppId
 * 首先查询小区绑定的微信配置信息，如果未找到则使用全局默认的AppId
 * 
 * @author Java110
 * @serviceCode wechat.getCommunityWechatAppId
 */
@Java110Cmd(serviceCode = "wechat.getCommunityWechatAppId")
public class GetCommunityWechatAppIdCmd extends Cmd {

    /**
     * 微信小程序服务接口，用于查询微信小程序配置信息
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的小区ID和对象类型
     *
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含请求的所有参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含communityId
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区ID");
        // 验证请求参数中必须包含objType
        Assert.hasKeyAndValue(reqJson,"objType","未包含类型");
    }

    /**
     * 命令执行方法
     * 根据小区ID和对象类型获取对应的微信AppId
     * 优先查询小区绑定的微信配置，如果不存在则使用全局默认AppId
     *
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含请求的所有参数
     * @throws CmdException 当命令执行过程中出现错误时抛出命令异常
     * @throws ParseException 当数据解析过程中出现错误时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建微信小程序数据传输对象，用于封装查询条件
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        // 设置查询条件：小区ID，从请求参数中获取
        smallWeChatDto.setObjId(reqJson.getString("communityId"));
        // 设置查询条件：微信类型，从请求参数中获取
        smallWeChatDto.setWechatType(reqJson.getString("objType"));
        // 设置查询条件：微信类型（兼容性设置，确保两种属性名都能设置）
        smallWeChatDto.setWeChatType(reqJson.getString("objType"));
        
        // 根据条件查询微信小程序配置信息
        List<SmallWeChatDto> smallWeChatDtos = smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto);
        
        // 初始化AppId变量
        String appId = "";
        
        // 判断查询结果，如果未找到小区绑定的微信配置，则使用全局默认AppId
        if (smallWeChatDtos == null || smallWeChatDtos.size() < 1) {
            // 从缓存中获取全局默认的微信AppId
            appId = MappingCache.getValue(WechatConstant.WECHAT_DOMAIN, "appId");
        } else {
            // 使用查询到的第一个微信配置的AppId
            appId = smallWeChatDtos.get(0).getAppId();
        }

        // 将AppId设置到响应中返回给调用方
        context.setResponseEntity(ResultVo.createResponseEntity(appId));
    }
}