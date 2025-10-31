package com.java110.dev.cmd.menuGroup;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.menu.MenuGroupDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 更新菜单组命令类
 * 负责处理菜单组的更新操作
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"menuGroup.updateMenuGroup"
 */
@Java110Cmd(serviceCode = "menuGroup.updateMenuGroup")
public class UpdateMenuGroupCmd extends Cmd {
    
    /**
     * 菜单内部服务接口，用于执行实际的菜单组更新操作
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证组ID不能为空
        Assert.hasKeyAndValue(reqJson, "gId", "组Id不能为空");
        // 验证组名称不能为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写组名称");
        // 验证图标不能为空
        Assert.hasKeyAndValue(reqJson, "icon", "必填，请填写icon");
        // 验证序列号不能为空
        Assert.hasKeyAndValue(reqJson, "seq", "必填，请填写序列");
    }

    /**
     * 执行命令方法
     * 处理菜单组的更新业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 将请求JSON数据转换为MenuGroupDto对象
        MenuGroupDto menuGroupDto = BeanConvertUtil.covertBean(reqJson, MenuGroupDto.class);

        // 调用服务层更新菜单组，返回更新结果
        int saveFlag = menuInnerServiceSMOImpl.updateMenuGroup(menuGroupDto);

        // 根据更新结果设置响应实体
        // 更新成功返回HTTP 200状态码和"成功"消息
        // 更新失败返回HTTP 400状态码和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}