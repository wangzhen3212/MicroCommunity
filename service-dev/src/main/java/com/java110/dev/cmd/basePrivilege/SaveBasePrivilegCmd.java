package com.java110.dev.cmd.basePrivilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 保存基础权限命令类
 * 处理权限信息的保存操作
 * 
 * @Java110Cmd 注解定义服务代码为 basePrivilege.saveBasePrivilege
 */
@Java110Cmd(serviceCode = "basePrivilege.saveBasePrivilege")
public class SaveBasePrivilegCmd extends Cmd{

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
        // 验证权限名称是否为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写权限名称");
        // 验证商户类型是否为空
        Assert.hasKeyAndValue(reqJson, "domain", "必填，请选择商户类型");
        // 验证资源路径是否为空
        Assert.hasKeyAndValue(reqJson, "resource", "必填，请选择资源路径");
        // 验证菜单ID是否为空
        Assert.hasKeyAndValue(reqJson, "mId", "必填，菜单为空");
    }

    /**
     * 执行命令方法
     * 处理权限保存的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 将JSON数据转换为权限数据传输对象
        BasePrivilegeDto basePrivilegeDto = BeanConvertUtil.covertBean(reqJson, BasePrivilegeDto.class);

        // 刷新权限ID，如果为空则生成新的ID
        freshPId(basePrivilegeDto);

        // 调用服务保存权限信息，并获取保存结果
        int saveFlag = menuInnerServiceSMOImpl.saveBasePrivilege(basePrivilegeDto);

        // 根据保存结果设置响应实体
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新权限ID方法
     * 如果权限ID为空，则生成新的权限ID
     * 
     * @param basePrivilegeDto 权限数据传输对象
     */
    private void freshPId(BasePrivilegeDto basePrivilegeDto) {
        // 如果权限ID不为空，直接返回
        if(!StringUtils.isEmpty(basePrivilegeDto.getPId())){
            return ;
        }
        
        // 生成新的权限ID流水号
        basePrivilegeDto.setPId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.BASE_PRIVILEGE));
    }
}