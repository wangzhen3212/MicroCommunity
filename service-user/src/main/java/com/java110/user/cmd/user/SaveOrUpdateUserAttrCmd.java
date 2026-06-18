package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.user.UserAttrDto;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.user.UserAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 用户属性保存或更新命令类
 * 
 * 该类负责处理用户属性的保存和更新操作，根据用户ID和属性规格代码判断是新增还是更新用户属性
 * 使用@Java110Cmd注解标识服务代码为"user.saveOrUpdateUserAttr"
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "user.saveOrUpdateUserAttr")
public class SaveOrUpdateUserAttrCmd extends Cmd {

    /**
     * 用户内部服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 用户属性V1版本内部服务接口
     */
    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求JSON中是否包含必要的参数节点
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含userId节点
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求参数中未包含userId 节点，请确认");
        // 验证请求参数中必须包含specCd节点（属性规格代码）
        Assert.jsonObjectHaveKey(reqJson, "specCd", "请求参数中未包含属性 节点，请确认");
        // 验证请求参数中必须包含value节点（属性值）
        Assert.jsonObjectHaveKey(reqJson, "value", "请求参数中未包含属性值 节点，请确认");
    }

    /**
     * 执行用户属性保存或更新命令
     * 
     * 根据用户ID和属性规格代码查询是否已存在对应属性：
     * - 如果存在则执行更新操作
     * - 如果不存在则执行新增操作
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存或更新操作失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建用户属性DTO对象并设置查询条件
        UserAttrDto userAttrDto = new UserAttrDto();
        userAttrDto.setUserId(reqJson.getString("userId"));
        userAttrDto.setSpecCd(reqJson.getString("specCd"));
        
        // 根据用户ID和属性规格代码查询已存在的用户属性
        List<UserAttrDto> userAttrDtos = userInnerServiceSMOImpl.getUserAttrs(userAttrDto);
        int flag = 0; // 操作结果标志
        
        // 如果查询到已存在的用户属性，则执行更新操作
        if(userAttrDtos != null && userAttrDtos.size() >0){
            // 创建用户属性PO对象用于更新操作
            UserAttrPo userAttrPo = new UserAttrPo();
            userAttrPo.setUserId(userAttrDtos.get(0).getUserId()); // 设置用户ID
            userAttrPo.setAttrId(userAttrDtos.get(0).getAttrId()); // 设置属性ID
            userAttrPo.setSpecCd(reqJson.getString("specCd")); // 设置属性规格代码
            userAttrPo.setValue(reqJson.getString("value")); // 设置属性值
            
            // 执行更新操作并获取影响行数
            flag = userAttrV1InnerServiceSMOImpl.updateUserAttr(userAttrPo);
            // 如果更新失败（影响行数小于1），抛出异常
            if(flag< 1){
                throw new CmdException("更新失败");
            }
            return; // 更新成功后直接返回
        }

        // 如果不存在对应的用户属性，则执行新增操作
        UserAttrPo userAttrPo = new UserAttrPo();
        userAttrPo.setUserId(reqJson.getString("userId")); // 设置用户ID
        // 生成唯一的属性ID
        userAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        userAttrPo.setSpecCd(reqJson.getString("specCd")); // 设置属性规格代码
        userAttrPo.setValue(reqJson.getString("value")); // 设置属性值
        
        // 执行保存操作并获取影响行数
        flag = userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
        // 如果保存失败（影响行数小于1），抛出异常
        if(flag< 1){
            throw new CmdException("添加失败");
        }
    }
}