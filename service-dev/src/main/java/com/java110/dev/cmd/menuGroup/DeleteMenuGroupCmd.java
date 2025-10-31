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
 * 删除菜单组命令类
 * 负责处理菜单组的删除操作
 * 
 * @Java110Cmd 注解定义服务编码为 menuGroup.deleteMenuGroup
 */
@Java110Cmd(serviceCode = "menuGroup.deleteMenuGroup")
public class DeleteMenuGroupCmd extends Cmd {
    
    /**
     * 菜单内部服务接口
     * 用于执行实际的菜单组删除操作
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含gId字段
        Assert.hasKeyAndValue(reqJson, "gId", "组Id不能为空");
    }

    /**
     * 执行命令方法
     * 执行菜单组的删除操作并返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 响应实体，用于返回操作结果
        ResponseEntity<String> responseEntity = null;

        // 将请求JSON转换为MenuGroupDto对象
        MenuGroupDto menuGroupDto = BeanConvertUtil.covertBean(reqJson, MenuGroupDto.class);

        // 调用服务删除菜单组，并获取删除结果
        int saveFlag = menuInnerServiceSMOImpl.deleteMenuGroup(menuGroupDto);

        // 根据删除结果设置响应实体
        // 删除成功返回HTTP 200状态码和"成功"消息
        // 删除失败返回HTTP 400状态码和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}