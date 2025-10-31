package com.java110.dev.cmd.basePrivilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 更新基础权限命令类
 * 负责处理权限信息的更新操作
 * 
 * @Java110Cmd 注解声明服务代码为 basePrivilege.updateBasePrivilege
 */
@Java110Cmd(serviceCode = "basePrivilege.updateBasePrivilege")
public class UpdateBasePrivilegeCmd extends Cmd {

    /**
     * 菜单内部服务接口，用于权限相关的数据操作
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数校验失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验权限ID不能为空
        Assert.hasKeyAndValue(reqJson, "pId", "权限ID不能为空");
        // 校验权限名称不能为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写权限名称");
        // 校验商户类型不能为空
        Assert.hasKeyAndValue(reqJson, "domain", "必填，请选择商户类型");
        // 校验资源路径不能为空
        Assert.hasKeyAndValue(reqJson, "resource", "必填，请选择资源路径");
    }

    /**
     * 命令执行方法
     * 执行权限信息的更新操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 将JSON数据转换为BasePrivilegeDto对象
        BasePrivilegeDto basePrivilegeDto = BeanConvertUtil.covertBean(reqJson, BasePrivilegeDto.class);

        // 调用服务更新权限信息，并获取更新结果
        int saveFlag = menuInnerServiceSMOImpl.updateBasePrivilege(basePrivilegeDto);

        // 根据更新结果设置响应实体
        // 更新成功返回HTTP状态码200和"成功"消息
        // 更新失败返回HTTP状态码400和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}