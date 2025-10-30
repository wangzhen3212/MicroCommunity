package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.smallWeChat.ApiSmallWeChatDataVo;
import com.java110.vo.api.smallWeChat.ApiSmallWeChatVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 小程序微信配置查询命令类
 * 功能：根据小区ID查询对应的小程序微信配置信息，仅返回AppId相关信息
 * 服务码：smallWeChat.listSmallWeChatOnlyAppId
 * 主要处理逻辑：查询小程序微信配置，并过滤敏感信息后返回
 */
@Java110Cmd(serviceCode = "smallWeChat.listSmallWeChatOnlyAppId")
public class ListSmallWeChatOnlyAppIdCmd extends Cmd {

    // 业主应用标识
    private static String OWNER_APP = "992019111758490006";
    // 业主微信应用标识
    private static String OWNER_WECHAT_APP = "992020061452450002";

    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数的有效性
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
        // 注释掉的代码：原小区信息验证
        //Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 主要功能：查询小程序微信配置，处理分页，过滤敏感信息并返回结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为SmallWeChatDto对象
        SmallWeChatDto smallWeChatDto = BeanConvertUtil.covertBean(reqJson, SmallWeChatDto.class);
        // 设置查询条件：对象类型为小区
        smallWeChatDto.setObjType(SmallWeChatDto.OBJ_TYPE_COMMUNITY);
        // 设置查询条件：小区ID
        smallWeChatDto.setObjId(reqJson.getString("communityId"));
        
        // 查询符合条件的记录总数
        int count = smallWeChatInnerServiceSMOImpl.querySmallWeChatsCount(smallWeChatDto);
        List<ApiSmallWeChatDataVo> smallWeChats = null;
        
        // 如果有数据，则查询详细列表并过滤敏感信息
        if (count > 0) {
            smallWeChats = BeanConvertUtil.covertBeanList(
                smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto), 
                ApiSmallWeChatDataVo.class
            );
            // 过滤敏感信息
            freshSecure(smallWeChats);
        } else {
            // 无数据时返回空列表
            smallWeChats = new ArrayList<>();
        }
        
        // 构建返回VO对象
        ApiSmallWeChatVo apiSmallWeChatVo = new ApiSmallWeChatVo();
        apiSmallWeChatVo.setTotal(count); // 设置总记录数
        // 计算总页数：总记录数/每页记录数，向上取整
        apiSmallWeChatVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiSmallWeChatVo.setSmallWeChats(smallWeChats); // 设置小程序微信数据列表
        
        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiSmallWeChatVo), 
            HttpStatus.OK
        );
        context.setResponseEntity(responseEntity);
    }

    /**
     * 过滤敏感信息方法
     * 将小程序微信配置中的敏感字段清空，防止敏感信息泄露
     *
     * @param smallWeChats 小程序微信数据列表
     */
    private void freshSecure(List<ApiSmallWeChatDataVo> smallWeChats) {
        // 遍历所有小程序微信配置数据
        for (ApiSmallWeChatDataVo apiSmallWeChatDataVo : smallWeChats) {
            // 清空应用密钥
            apiSmallWeChatDataVo.setAppSecret("");
            // 清空支付密码
            apiSmallWeChatDataVo.setPayPassword("");
            // 清空商户号
            apiSmallWeChatDataVo.setMchId("");
        }
    }
}