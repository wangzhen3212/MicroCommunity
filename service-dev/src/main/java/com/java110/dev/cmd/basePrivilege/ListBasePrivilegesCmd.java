package com.java110.dev.cmd.basePrivilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.basePrivilege.ApiBasePrivilegeDataVo;
import com.java110.vo.api.basePrivilege.ApiBasePrivilegeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询基础权限列表命令类
 * 服务编码：basePrivilege.listBasePrivileges
 * 用于查询系统基础权限信息列表
 */
@Java110Cmd(serviceCode = "basePrivilege.listBasePrivileges")
public class ListBasePrivilegesCmd extends Cmd{

    // 注入菜单内部服务接口
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询基础权限列表并返回分页结果
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为权限查询DTO对象
        BasePrivilegeDto basePrivilegeDto = BeanConvertUtil.covertBean(reqJson, BasePrivilegeDto.class);

        // 查询权限总记录数
        int count = menuInnerServiceSMOImpl.queryBasePrivilegesCount(basePrivilegeDto);

        // 权限数据列表
        List<ApiBasePrivilegeDataVo> basePrivileges = null;

        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询权限列表并转换为VO对象
            basePrivileges = BeanConvertUtil.covertBeanList(
                    menuInnerServiceSMOImpl.queryBasePrivileges(basePrivilegeDto), 
                    ApiBasePrivilegeDataVo.class);
        } else {
            // 无数据时返回空列表
            basePrivileges = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiBasePrivilegeVo apiBasePrivilegeVo = new ApiBasePrivilegeVo();

        // 设置总记录数
        apiBasePrivilegeVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，向上取整
        apiBasePrivilegeVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置权限数据列表
        apiBasePrivilegeVo.setBasePrivileges(basePrivileges);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
                JSONObject.toJSONString(apiBasePrivilegeVo), 
                HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}