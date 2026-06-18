/**
 * 组织架构查询命令类
 * 
 * 该类用于处理查询父级组织架构的请求，通过接收前端传入的参数，
 * 调用相应的服务接口获取父级组织列表，并将结果返回给前端
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.org;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.org.OrgDto;
import com.java110.intf.user.IOrgInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.org.ApiOrgDataVo;
import com.java110.vo.api.org.ApiOrgVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "org.listParentOrgs")
public class ListParentOrgsCmd extends Cmd {

    /**
     * 组织架构内部服务接口
     */
    @Autowired
    private IOrgInnerServiceSMO orgInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证前端传入的请求参数是否合法，主要检查必填参数是否存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 注释掉的分页信息验证，可根据需要启用
        //super.validatePageInfo(reqJson);
        
        // 验证请求参数中必须包含storeId字段
        Assert.hasKeyAndValue(reqJson, "storeId", "必填，请填写商户ID");
    }

    /**
     * 命令执行方法
     * 
     * 处理查询父级组织架构的核心业务逻辑，包括参数转换、服务调用和结果返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为OrgDto对象，便于后续处理
        OrgDto orgDto = BeanConvertUtil.covertBean(reqJson, OrgDto.class);

        // 调用组织架构服务接口查询父级组织列表，并将结果转换为ApiOrgDataVo对象列表
        List<ApiOrgDataVo> parentOrgs = BeanConvertUtil.covertBeanList(orgInnerServiceSMOImpl.queryParentOrgs(orgDto), ApiOrgDataVo.class);

        // 创建API响应对象
        ApiOrgVo apiOrgVo = new ApiOrgVo();
        // 设置组织架构数据到响应对象中
        apiOrgVo.setOrgs(parentOrgs);

        // 构建HTTP响应实体，将响应对象转换为JSON字符串并设置HTTP状态码为200
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiOrgVo), HttpStatus.OK);

        // 将响应实体设置到上下文中，返回给前端
        context.setResponseEntity(responseEntity);
    }
}