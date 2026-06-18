package com.java110.dev.cmd.menu;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.menu.MenuDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 更新菜单命令类
 * 负责处理菜单更新相关的业务逻辑
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"menu.updateMenu"
 */
@Java110Cmd(serviceCode = "menu.updateMenu")
public class UpdateMenuCmd extends Cmd {

    /**
     * 菜单内部服务接口，用于操作菜单数据
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 对请求参数进行校验，确保必填字段不为空
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证菜单ID不能为空
        Assert.hasKeyAndValue(reqJson, "mId", "菜单ID不能为空");
        // 验证菜单名称不能为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写菜单名称");
        // 验证菜单地址不能为空
        Assert.hasKeyAndValue(reqJson, "url", "必填，请菜单菜单地址");
        // 验证序列号不能为空
        Assert.hasKeyAndValue(reqJson, "seq", "必填，请填写序列");
        // 验证是否显示菜单字段不能为空
        Assert.hasKeyAndValue(reqJson, "isShow", "必填，请选择是否显示菜单");
    }

    /**
     * 执行菜单更新命令
     * 将请求数据转换为菜单DTO并更新到数据库
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为MenuDto对象
        MenuDto menuDto = BeanConvertUtil.covertBean(reqJson, MenuDto.class);

        // 调用服务层更新菜单信息
        int count = menuInnerServiceSMOImpl.updateMenu(menuDto);

        // 检查更新是否成功，count表示受影响的行数
        if (count < 1) {
            // 更新失败，抛出业务异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "修改数据失败");
        }

        // 设置响应结果为成功
        context.setResponseEntity(ResultVo.success());
    }
}