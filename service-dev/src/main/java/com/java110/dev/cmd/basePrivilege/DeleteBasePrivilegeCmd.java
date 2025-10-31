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
 * 删除基础权限命令类
 * 负责处理删除权限信息的业务逻辑
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"basePrivilege.deleteBasePrivilege"
 */
@Java110Cmd(serviceCode = "basePrivilege.deleteBasePrivilege")
public class DeleteBasePrivilegeCmd extends Cmd{

    // 注入菜单内部服务接口
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否合法
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含权限ID(pId)
        Assert.hasKeyAndValue(reqJson, "pId", "权限ID不能为空");
    }

    /**
     * 执行命令方法
     * 处理删除权限的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 定义响应实体
        ResponseEntity<String> responseEntity = null;

        // 将请求JSON转换为BasePrivilegeDto对象
        BasePrivilegeDto basePrivilegeDto = BeanConvertUtil.covertBean(reqJson, BasePrivilegeDto.class);

        // 调用服务层删除权限信息，并获取删除结果
        int saveFlag = menuInnerServiceSMOImpl.deleteBasePrivilege(basePrivilegeDto);

        // 根据删除结果设置响应信息
        // 删除成功返回HTTP状态码200和"成功"消息
        // 删除失败返回HTTP状态码400和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}