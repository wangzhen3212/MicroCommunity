package com.java110.dev.cmd.menuGroup;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.menu.MenuGroupDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 保存菜单组命令类
 * 处理菜单组保存相关的业务逻辑
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"menuGroup.saveMenuGroup"
 */
@Java110Cmd(serviceCode = "menuGroup.saveMenuGroup")
public class SaveMenuGroupCmd extends Cmd {

    /**
     * 菜单内部服务接口，用于操作菜单组数据
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
        // 验证必填参数：组名称
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写组名称");
        // 验证必填参数：图标
        Assert.hasKeyAndValue(reqJson, "icon", "必填，请填写icon");
        // 验证必填参数：序列号
        Assert.hasKeyAndValue(reqJson, "seq", "必填，请填写序列");
    }

    /**
     * 执行命令方法
     * 处理菜单组保存的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 将JSON数据转换为MenuGroupDto对象
        MenuGroupDto menuGroupDto = BeanConvertUtil.covertBean(reqJson, MenuGroupDto.class);

        // 刷新菜单组ID，确保ID不为空
        freshGId(menuGroupDto);

        // 调用服务保存菜单组数据
        int saveFlag = menuInnerServiceSMOImpl.saveMenuGroup(menuGroupDto);

        // 根据保存结果设置响应实体
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新菜单组ID
     * 如果菜单组ID为空，则生成一个新的ID
     * 
     * @param menuGroupDto 菜单组数据传输对象
     */
    private void freshGId(MenuGroupDto menuGroupDto) {
        // 如果菜单组ID不为空，直接返回
        if(!StringUtils.isEmpty(menuGroupDto.getgId())){
            return ;
        }
        
        // 生成新的菜单组ID
        menuGroupDto.setgId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.MENU_GROUP));
    }
}