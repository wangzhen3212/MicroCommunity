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
 * 删除菜单命令类
 * 处理菜单删除业务逻辑
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"menu.deleteMenu"
 */
@Java110Cmd(serviceCode = "menu.deleteMenu")
public class DeleteMenuCmd extends Cmd {

    // 注入菜单内部服务接口
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
        // 验证请求JSON中必须包含菜单ID字段
        Assert.hasKeyAndValue(reqJson, "mId", "菜单ID不能为空");
    }

    /**
     * 执行命令方法
     * 处理菜单删除的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为MenuDto对象
        MenuDto menuDto = BeanConvertUtil.covertBean(reqJson, MenuDto.class);

        // 调用菜单服务删除菜单，返回受影响的行数
        int count = menuInnerServiceSMOImpl.deleteMenu(menuDto);

        // 检查删除操作是否成功
        if (count < 1) {
            // 删除失败，抛出监听器执行异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "删除数据失败");
        }

        // 设置响应结果为成功
        context.setResponseEntity(ResultVo.success());
    }
}