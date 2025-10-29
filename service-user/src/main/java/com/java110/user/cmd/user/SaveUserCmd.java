/**
 * 用户保存命令类
 * 
 * 该类负责处理用户保存相关的业务逻辑，包括参数验证和用户信息保存。
 * 通过注解@Java110Cmd声明服务代码为"user.saveUser"，用于处理用户保存请求。
 * 继承自Cmd类，实现了命令模式中的具体命令。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.user.UserAttrDto;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.user.UserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

@Java110Cmd(serviceCode = "user.saveUser")
public class SaveUserCmd extends Cmd {

    /**
     * 用户服务接口，用于操作用户数据
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 用户属性服务接口，用于操作用户属性数据
     */
    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，并对特定字段进行格式校验
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验json格式中是否包含必要的字段：name, tel, address, sex, levelCd
        Assert.jsonObjectHaveKey(reqJson, "name", "请求参数中未包含name节点，请确认");
        // 邮箱字段为可选，暂时注释掉强制校验
        // Assert.jsonObjectHaveKey(paramObj,"email","请求参数中未包含email节点，请确认");
        Assert.jsonObjectHaveKey(reqJson, "tel", "请求参数中未包含tel节点，请确认");
        Assert.jsonObjectHaveKey(reqJson, "address", "请求报文格式错误或未包含地址信息");
        Assert.jsonObjectHaveKey(reqJson, "sex", "请求报文格式错误或未包含性别信息");
        Assert.jsonObjectHaveKey(reqJson, "levelCd", "请求报文格式错误或未包含员工角色");

        // 如果请求中包含email字段且不为空，则验证邮箱格式
        if (reqJson.containsKey("email") && !StringUtil.isEmpty(reqJson.getString("email"))) {
            Assert.isEmail(reqJson, "email", "不是有效的邮箱格式");
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理用户保存的核心业务逻辑，包括用户ID生成、密码处理、用户信息保存和用户属性更新
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当业务操作失败时抛出异常
     * @throws ParseException 当数据解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 判断请求报文中是否包含userId，如果不存在或为-1，则生成新的用户ID
        // 否则使用已有的用户ID将用户添加为员工
        String userId = "";
        if (!reqJson.containsKey("userId") || "-1".equals(reqJson.getString("userId"))) {
            // 生成新的用户ID
            userId = GenerateCodeFactory.getUserId();
            reqJson.put("userId", userId);
        }
        
        // 处理用户密码
        String staffDefaultPassword = reqJson.getString("password");
        if (StringUtil.isEmpty(staffDefaultPassword)) {
            // 从映射表中获取默认密码配置
            staffDefaultPassword = MappingCache.getValue(MappingConstant.KEY_STAFF_DEFAULT_PASSWORD);
            // 验证默认密码配置是否存在
            Assert.hasLength(staffDefaultPassword, "映射表中未设置默认密码，请检查" + MappingConstant.KEY_STAFF_DEFAULT_PASSWORD);
        }

        // 对密码进行MD5加密
        staffDefaultPassword = AuthenticationFactory.passwdMd5(staffDefaultPassword);
        reqJson.put("password", staffDefaultPassword);
        
        // 将JSON对象转换为UserPo对象
        UserPo userPo = BeanConvertUtil.covertBean(reqJson, UserPo.class);
        // 调用服务保存用户信息
        int flag = userV1InnerServiceSMOImpl.saveUser(userPo);
        if (flag < 1) {
            throw new CmdException("保存用户失败");
        }

        // 如果存在扩展用户ID，则更新用户属性
        if (!StringUtil.isEmpty(reqJson.getString("extUserId"))) {
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setUserId(reqJson.getString("userId"));
            // 生成属性ID
            userAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
            // 设置属性规格代码为用户ID属性
            userAttrPo.setSpecCd(UserAttrDto.SPEC_PROPERTY_USER_ID);
            // 设置属性值为扩展用户ID
            userAttrPo.setValue(reqJson.getString("extUserId"));
            // 更新用户属性
            flag = userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
            if (flag < 1) {
                throw new CmdException("更新失败");
            }
        }
    }
}