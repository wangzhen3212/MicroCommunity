package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 查询用户密钥信息命令类
 * 
 * 该类用于处理查询用户密钥信息的业务逻辑，包括用户密钥和OpenID的获取
 * 通过用户ID查询用户信息，并提取用户的密钥和OpenID属性
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode user.queryUserSecret
 */
@Java110Cmd(serviceCode = "user.queryUserSecret")
public class QueryUserSecretCmd extends Cmd {

    /**
     * 用户内部服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的用户ID参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含userId参数
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含用户ID");
    }

    /**
     * 执行命令的主方法
     * 
     * 根据用户ID查询用户信息，提取用户的密钥和OpenID属性，并返回处理结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;
        
        // 将请求JSON转换为UserDto对象
        UserDto userDto = BeanConvertUtil.covertBean(reqJson, UserDto.class);

        // 调用用户服务查询用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 检查查询结果，如果用户不存在则返回错误信息
        if (userDtos == null || userDtos.size() < 1) {
            responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "未找到用户信息");
            context.setResponseEntity(responseEntity);
            return;
        }

        // 获取第一个用户信息（理论上根据userId查询应该只有一条记录）
        userDto = userDtos.get(0);
        
        // 获取用户属性列表
        List<UserAttrDto> userAttrDtos = userDto.getUserAttrs();
        
        // 初始化密钥和OpenID变量
        String key = "";
        String openId = "";
        
        // 遍历用户属性，查找密钥和OpenID
        for (UserAttrDto userAttrDto : userAttrDtos) {
            // 检查是否为密钥属性
            if (UserAttrDto.SPEC_KEY.equals(userAttrDto.getSpecCd())) {
                key = userAttrDto.getValue();
            }

            // 检查是否为OpenID属性
            if (UserAttrDto.SPEC_OPEN_ID.equals(userAttrDto.getSpecCd())) {
                openId = userAttrDto.getValue();
            }
        }
        
        // 将提取的密钥和OpenID设置到用户DTO中
        userDto.setKey(key);
        userDto.setOpenId(openId);
        
        // 创建成功响应，返回包含用户密钥信息的用户DTO
        responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_OK, "成功", userDto);
        context.setResponseEntity(responseEntity);
    }
}