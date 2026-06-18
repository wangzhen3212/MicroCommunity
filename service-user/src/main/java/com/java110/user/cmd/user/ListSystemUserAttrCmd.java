/**
 * 用户属性查询命令类
 * 
 * 该命令类用于查询系统用户的属性信息，支持分页查询功能。
 * 通过调用用户属性服务接口获取用户属性数据，并返回标准格式的响应结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "user.listSystemUserAttr")
public class ListSystemUserAttrCmd extends Cmd {
    
    /**
     * 用户属性服务接口
     */
    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;
    
    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括管理员权限验证、分页信息验证和必要参数检查。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证必填参数：用户ID
        Assert.hasKeyAndValue(reqJson,"systemUserId","未包含用户ID");
    }

    /**
     * 命令执行方法
     * 
     * 执行用户属性查询业务逻辑，包括参数转换、数据查询、结果封装和响应设置。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为用户属性数据传输对象
        UserAttrDto userAttrDto = BeanConvertUtil.covertBean(reqJson, UserAttrDto.class);
        // 设置用户ID到查询条件中
        userAttrDto.setUserId(reqJson.getString("systemUserId"));

        // 查询用户属性总记录数
        int count = userAttrV1InnerServiceSMOImpl.queryUserAttrsCount(userAttrDto);

        // 用户属性数据列表
        List<UserAttrDto> userAttrDtos = null;

        // 根据查询结果数量判断是否查询详细数据
        if (count > 0) {
            // 查询用户属性详细数据
            userAttrDtos = userAttrV1InnerServiceSMOImpl.queryUserAttrs(userAttrDto);
        } else {
            // 如果没有数据，返回空列表
            userAttrDtos = new ArrayList<>();
        }

        // 构造分页结果对象
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, userAttrDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }
}