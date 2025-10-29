package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.account.AccountDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.acct.IAccountInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.account.AccountPo;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.owner.OwnerPo;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.acl.Owner;
import java.util.List;

/**
 * 业主房屋认证审核命令类
 * 
 * 处理业主房屋认证的审核流程，包括审核通过和审核拒绝两种情况
 * 审核通过时会根据业主类型（业主或家庭成员）执行不同的绑定逻辑
 * 审核拒绝则直接更新审核状态为失败
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "owner.auditAuthOwner")
public class AuditAuthOwnerCmd extends Cmd {

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IAccountInnerServiceSMO accountInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 检查必要的请求参数是否存在，并验证审核记录的状态
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "appUserId", "绑定ID不能为空");
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写状态");
        Assert.hasKeyAndValue(reqJson, "remark", "必填，请填写审核说明");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");

        // 验证审核记录是否存在且状态为待审核
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setAppUserId(reqJson.getString("appUserId"));
        ownerAppUserDto.setState(OwnerAppUserDto.STATE_AUDITING); // 设置查询条件为待审核状态
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 确保只存在一条待审核的记录
        Assert.listOnlyOne(ownerAppUserDtos, "审核记录不存在");
    }

    /**
     * 执行业主认证审核命令
     * 
     * 根据审核状态执行不同的处理逻辑：
     * - 审核拒绝：直接更新审核状态为失败
     * - 审核通过：根据业主类型执行绑定逻辑，并更新用户信息和审核状态
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        String userId = CmdContextUtils.getUserId(context);

        String state = "";
        // 处理审核拒绝的情况
        if ("1200".equals(reqJson.getString("state"))) {
            OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
            ownerAppUserPo.setAppUserId(reqJson.getString("appUserId"));
            ownerAppUserPo.setState(OwnerAppUserDto.STATE_AUDIT_ERROR); // 设置状态为审核失败
            ownerAppUserPo.setRemark(reqJson.getString("remark"));
            int flag = ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(ownerAppUserPo);
            if (flag < 1) {
                throw new CmdException("修改绑定失败");
            }
            return; // 审核拒绝直接返回，不执行后续逻辑
        }

        // 查询待审核的业主应用用户信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setAppUserId(reqJson.getString("appUserId"));
        ownerAppUserDto.setState(OwnerAppUserDto.STATE_AUDITING);
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        String memberId = "";
        // 根据业主类型执行不同的绑定逻辑
        if (OwnerDto.OWNER_TYPE_CD_OWNER.equals(ownerAppUserDtos.get(0).getOwnerTypeCd())) {
            // 处理业主类型的绑定
            memberId = bindOwner(ownerAppUserDtos.get(0));
        } else {
            // 处理家庭成员类型的绑定
            memberId = bindOwnerMember(ownerAppUserDtos.get(0), userId);
        }

        // 更新用户姓名信息
        UserPo userPo = new UserPo();
        userPo.setName(ownerAppUserDtos.get(0).getAppUserName());
        userPo.setUserId(ownerAppUserDtos.get(0).getUserId());
        userV1InnerServiceSMOImpl.updateUser(userPo);

        // 更新审核状态为成功
        OwnerAppUserPo ownerAppUserPo = new OwnerAppUserPo();
        ownerAppUserPo.setAppUserId(reqJson.getString("appUserId"));
        ownerAppUserPo.setState(OwnerAppUserDto.STATE_AUDIT_SUCCESS);
        ownerAppUserPo.setRemark(reqJson.getString("remark"));
        ownerAppUserPo.setMemberId(memberId);
        int flag = ownerAppUserV1InnerServiceSMOImpl.updateOwnerAppUser(ownerAppUserPo);
        if (flag < 1) {
            throw new CmdException("修改绑定失败");
        }

    }

    /**
     * 绑定家庭成员
     * 
     * 处理家庭成员的绑定逻辑，包括：
     * - 验证房屋是否已绑定业主
     * - 检查家庭成员是否已存在
     * - 创建新的家庭成员记录
     * 
     * @param ownerAppUserDto 业主应用用户数据传输对象
     * @param userId 当前操作用户ID
     * @return 成员ID
     * @throws CmdException 当绑定过程中出现错误时抛出异常
     */
    private String bindOwnerMember(OwnerAppUserDto ownerAppUserDto, String userId) {
        // 查询房屋与业主的绑定关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(ownerAppUserDto.getRoomId());
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 验证房屋是否已绑定业主
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            throw new CmdException("房屋未绑定业主");
        }

        // 根据手机号和小区查询是否已存在家庭成员
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerId(ownerRoomRelDtos.get(0).getOwnerId());
        ownerDto.setLink(ownerAppUserDto.getLink());
        ownerDto.setCommunityId(ownerAppUserDto.getCommunityId());
        ownerDto.setOwnerTypeCds(new String[]{
                OwnerDto.OWNER_TYPE_CD_MEMBER
                // 可以扩展其他类型，如租客、临时业主等
//                ,
//                OwnerDto.OWNER_TYPE_CD_RENTING,
//                OwnerDto.OWNER_TYPE_CD_OTHER,
//                OwnerDto.OWNER_TYPE_CD_TEMP
        });
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 如果家庭成员已存在，直接返回成员ID
        if (!ListUtil.isNull(ownerDtos)) {
            return ownerDtos.get(0).getMemberId();
        }

        // 根据业主类型设置人员角色
        String personRole = OwnerDto.PERSON_ROLE_OWNER;
        if (OwnerDto.OWNER_TYPE_CD_MEMBER.equals(ownerAppUserDto.getOwnerTypeCd())) {
            personRole = OwnerDto.PERSON_ROLE_MEMBER;
        }

        // 创建新的家庭成员记录
        OwnerPo ownerPo = new OwnerPo();
        ownerPo.setMemberId(GenerateCodeFactory.getGeneratorId("11")); // 生成成员ID
        ownerPo.setOwnerId(ownerRoomRelDtos.get(0).getOwnerId());
        ownerPo.setName(ownerAppUserDto.getAppUserName());
        ownerPo.setSex("0"); // 默认性别
        ownerPo.setLink(ownerAppUserDto.getLink());
        ownerPo.setUserId(userId);
        ownerPo.setRemark("业主认证，审核添加");
        ownerPo.setOwnerTypeCd(ownerAppUserDto.getOwnerTypeCd());
        ownerPo.setCommunityId(ownerAppUserDto.getCommunityId());
        ownerPo.setIdCard(ownerAppUserDto.getIdCard());
        ownerPo.setState(OwnerDto.STATE_FINISH); // 设置状态为完成
        ownerPo.setOwnerFlag(OwnerDto.OWNER_FLAG_TRUE); // 设置业主标志为真
        ownerPo.setPersonType(OwnerDto.PERSON_TYPE_PERSON); // 设置人员类型为个人
        ownerPo.setPersonRole(personRole); // 设置人员角色
        ownerPo.setAddress("无"); // 默认地址
        ownerV1InnerServiceSMOImpl.saveOwner(ownerPo);

        return ownerPo.getMemberId();
    }

    /**
     * 绑定业主
     * 
     * 处理业主的绑定逻辑，包括：
     * - 验证房屋是否已绑定业主
     * - 更新业主联系信息
     * - 更新账户联系信息
     * 
     * @param ownerAppUserDto 业主应用用户数据传输对象
     * @return 业主ID
     * @throws CmdException 当绑定过程中出现错误时抛出异常
     */
    private String bindOwner(OwnerAppUserDto ownerAppUserDto) {

        // 查询房屋与业主的绑定关系
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setRoomId(ownerAppUserDto.getRoomId());
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);

        // 验证房屋是否已绑定业主
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            throw new CmdException("房屋未绑定业主");
        }

        // 更新业主联系信息
        OwnerPo ownerPo = new OwnerPo();
        ownerPo.setLink(ownerAppUserDto.getLink());
        // ownerPo.setName(ownerAppUserDto.getAppUserName()); // 注释掉的姓名更新
        ownerPo.setMemberId(ownerRoomRelDtos.get(0).getOwnerId());
        ownerPo.setOwnerId(ownerRoomRelDtos.get(0).getOwnerId());
        ownerV1InnerServiceSMOImpl.updateOwner(ownerPo);

        // 查询业主账户信息
        AccountDto accountDto = new AccountDto();
        accountDto.setObjId(ownerRoomRelDtos.get(0).getOwnerId());
        accountDto.setPartId(ownerRoomRelDtos.get(0).getCommunityId());
        List<AccountDto> accountDtos = accountInnerServiceSMOImpl.queryAccounts(accountDto);
        
        // 如果账户不存在，直接返回业主ID
        if (ListUtil.isNull(accountDtos)) {
            return ownerRoomRelDtos.get(0).getOwnerId();
        }

        // 更新账户联系信息
        AccountPo accountPo = new AccountPo();
        accountPo.setoLink(ownerAppUserDto.getLink());
        accountPo.setAcctId(accountDtos.get(0).getAcctId());
        accountInnerServiceSMOImpl.updateAccount(accountPo);

        return ownerRoomRelDtos.get(0).getOwnerId();
    }
}