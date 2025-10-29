package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.auditAppUserBindingOwner.ApiAuditAppUserBindingOwnerDataVo;
import com.java110.vo.api.auditAppUserBindingOwner.ApiAuditAppUserBindingOwnerVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审核业主绑定用户列表查询命令类
 * 用于查询待审核的业主绑定用户信息，并进行数据脱敏处理
 * 
 * @Java110Cmd 注解指定服务编码为 owner.listAuditAppUserBindingOwners
 */
@Java110Cmd(serviceCode = "owner.listAuditAppUserBindingOwners")
public class ListAuditAppUserBindingOwnersCmd extends Cmd {

    /**
     * 业主应用用户内部服务接口
     */
    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    /**
     * 菜单内部服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event CmdEvent对象，包含事件信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");

        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 调用父类方法验证属性
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 查询审核业主绑定用户列表，并进行数据脱敏处理
     *
     * @param event CmdEvent对象，包含事件信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取当前用户ID
        String userId = reqJson.getString("userId");
        
        // 将请求JSON转换为OwnerAppUserDto对象
        OwnerAppUserDto ownerAppUserDto = BeanConvertUtil.covertBean(reqJson, OwnerAppUserDto.class);
        // 清空用户ID，避免查询条件冲突
        ownerAppUserDto.setUserId("");
        
        // 查询符合条件的业主绑定用户总数
        int count = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsersCount(ownerAppUserDto);
        // 获取每页显示行数
        int row = reqJson.getIntValue("row");

        List<OwnerAppUserDto> ownerAppUserDtos = null;
        
        // 如果查询到数据，进行数据处理
        if (count > 0) {
            // 查询业主绑定用户列表
            ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
            // 获取用户权限信息，用于判断是否需要进行数据脱敏
            List<Map> mark = getPrivilegeOwnerList("/roomCreateFee", userId);
            
            // 遍历处理每个业主绑定用户信息
            for (OwnerAppUserDto owner : ownerAppUserDtos) {
                // 区分小程序和公众号，设置应用类型名称
                if (owner.getAppType().equals("WECHAT")) {
                    owner.setAppTypeName("公众号");
                } else if (owner.getAppType().equals("WECHAT_MINA")) {
                    owner.setAppTypeName("小程序");
                }
                
                // 对业主身份证号进行脱敏处理（如果没有权限）
                String idCard = owner.getIdCard();
                if (mark.size() == 0 && !StringUtil.isEmpty(idCard) && idCard.length() > 16) {
                    // 身份证号脱敏：保留前6位，隐藏中间10位，保留后4位
                    idCard = idCard.substring(0, 6) + "**********" + idCard.substring(16);
                    owner.setIdCard(idCard);
                }
                
                // 对业主手机号进行脱敏处理（如果没有权限）
                String link = owner.getLink();
                if (mark.size() == 0 && !StringUtil.isEmpty(link) && link.length() == 11) {
                    // 手机号脱敏：保留前3位，隐藏中间4位，保留后4位
                    link = link.substring(0, 3) + "****" + link.substring(7);
                    owner.setLink(link);
                }
            }
        } else {
            // 如果没有查询到数据，返回空列表
            ownerAppUserDtos = new ArrayList<>();
        }

        // 计算总页数并创建响应实体
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) row), count, ownerAppUserDtos);
        // 设置响应实体到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 获取用户权限列表方法
     * 用于检查用户是否具有特定资源的访问权限，决定是否需要进行数据脱敏
     *
     * @param resource 资源路径，用于权限验证
     * @param userId 用户ID，用于查询用户权限
     * @return List<Map> 用户权限列表，如果列表为空表示没有相应权限
     */
    public List<Map> getPrivilegeOwnerList(String resource, String userId) {
        // 创建权限查询对象
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource(resource);
        basePrivilegeDto.setUserId(userId);
        
        // 查询用户是否具有指定资源的访问权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        return privileges;
    }
}