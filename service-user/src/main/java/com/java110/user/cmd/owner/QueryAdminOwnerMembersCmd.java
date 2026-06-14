/**
 * 业主成员查询命令类
 * 
 * 该命令类用于处理管理员查询业主成员信息的请求，包括验证请求参数、权限校验和数据查询等功能
 * 通过注解@Java110Cmd声明服务代码为"owner.queryAdminOwnerMembers"
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "owner.queryAdminOwnerMembers")
public class QueryAdminOwnerMembersCmd extends Cmd {

    /**
     * 业主信息服务接口，用于查询业主相关信息
     */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /**
     * 菜单服务接口，用于权限相关的菜单操作
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 文件关联服务接口，用于处理文件关联关系
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括必填字段检查和权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含ownerId字段
        Assert.jsonObjectHaveKey(reqJson, "ownerId", "请求中未包含ownerId信息");
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
        // 调用父类的管理员权限验证方法
        super.validateAdmin(context);
    }

    /**
     * 执行命令的主方法
     * 
     * 该方法负责处理业主成员查询的核心业务逻辑，包括数据查询、分页处理和响应构建
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");
        
        // 将请求JSON转换为OwnerDto对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);
        // 设置业主类型为成员类型
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_MEMBER);
        
        // 获取每页显示的行数
        int row = reqJson.getInteger("row");
        
        // 查询总记录数
        int total = ownerInnerServiceSMOImpl.queryOwnersMemberCount(ownerDto);

        List<OwnerDto> ownerDtos = null;
        // 根据总记录数判断是否需要查询详细数据
        if (total > 0) {
            // 查询业主成员详细信息列表
            ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);
        } else {
            // 如果没有记录，返回空列表
            ownerDtos = new ArrayList<>();
        }
        
        // 计算总页数并创建响应实体
        // 使用Math.ceil确保向上取整，正确处理余数情况
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) total / (double) row),  // 总页数计算
            total,                                           // 总记录数
            ownerDtos                                        // 数据列表
        );
        
        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}