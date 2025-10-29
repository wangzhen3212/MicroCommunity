/**
 * 删除业主成员命令类
 * 
 * 该命令类用于处理第三方系统（如招商系统）删除业主成员信息的请求。
 * 主要功能包括验证请求参数的有效性，删除业主成员信息以及相关的APP用户关联信息。
 * 使用事务保证数据操作的一致性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.owner.OwnerPo;
import com.java110.user.cmd.question.ListUserQuestionAnswerCmd;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110CmdDoc(title = "删除业主成员",
        description = "第三方系统，比如招商系统删除业主信息",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/owner.deleteOwnerMember",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "owner.deleteOwnerMember",
        seq = 11
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "memberId", length = 30, remark = "业主ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\n" +
                "\t\"memberId\": 123123123,\n" +
                "\t\"communityId\": \"2022121921870161\"\n" +
                "}",
        resBody = "{\"code\":0,\"msg\":\"成功\"}"
)
@Java110Cmd(serviceCode = "owner.deleteOwnerMember")
public class DeleteOwnerMemberCmd extends Cmd {

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl; // 业主信息服务接口

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl; // 业主V1版本服务接口

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl; // 房间信息服务接口

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl; // 业主车辆信息服务接口

    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl; // 业主APP用户信息服务接口

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl; // 业主APP用户V1版本服务接口

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl; // 用户V1版本服务接口

    /**
     * 验证请求参数的有效性
     * 
     * 该方法用于验证删除业主成员请求的合法性，包括：
     * 1. 检查当前环境是否为开发环境
     * 2. 验证当前用户权限
     * 3. 检查请求参数是否完整
     * 4. 验证业主成员是否存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();

        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 构建业主APP用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(userId);
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        // 查询当前用户的业主APP用户信息
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 验证业主APP用户是否存在
        if (ListUtil.isNull(ownerAppUserDtos)) {
            throw new CmdException("业主不存在");
        }

        // 验证业主成员ID是否为空
        if (StringUtil.isEmpty(ownerAppUserDtos.get(0).getMemberId())) {
            throw new CmdException("业主不存在");
        }

        // 验证请求参数中是否包含必要的字段
        Assert.jsonObjectHaveKey(reqJson, "memberId", "请求报文中未包含memberId");
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId");

        // 构建业主成员查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(reqJson.getString("memberId")); // 设置要删除的成员ID
        ownerDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        ownerDto.setOwnerId(ownerAppUserDtos.get(0).getMemberId()); // 设置业主ID
        ownerDto.setOwnerTypeCds(new String[]{OwnerDto.OWNER_TYPE_CD_MEMBER}); // 设置业主类型为成员

        // 查询业主成员信息
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);

        // 验证业主成员是否存在且唯一
        Assert.listOnlyOne(ownerDtos, "业主成员不存在");
    }

    /**
     * 执行删除业主成员操作
     * 
     * 该方法用于实际执行删除业主成员的操作，包括：
     * 1. 删除业主成员信息
     * 2. 删除相关的APP用户关联信息
     * 3. 使用事务保证数据一致性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求上下文信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建业主删除业务对象
        JSONObject businessOwner = new JSONObject();
        businessOwner.put("memberId", reqJson.getString("memberId"));
        businessOwner.put("communityId", reqJson.getString("communityId"));

        // 将JSON对象转换为PO对象
        OwnerPo ownerPo = BeanConvertUtil.covertBean(businessOwner, OwnerPo.class);
        // 执行删除业主操作
        int flag = ownerV1InnerServiceSMOImpl.deleteOwner(ownerPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除失败");
        }

        // 构建业主APP用户查询条件
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setMemberId(reqJson.getString("memberId"));
        ownerAppUserDto.setCommunityId(reqJson.getString("communityId"));
        // 查询关联的APP用户信息
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        
        // 如果没有关联的APP用户，直接返回
        if (ownerAppUserDtos == null && ownerAppUserDtos.size() < 1) {
            return;
        }

        // 遍历所有关联的APP用户，逐个删除
        for (OwnerAppUserDto ownerAppUser : ownerAppUserDtos) {
            // 将DTO对象转换为PO对象
            OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(ownerAppUser, OwnerAppUserPo.class);
            // 执行删除APP用户操作
            flag = ownerAppUserV1InnerServiceSMOImpl.deleteOwnerAppUser(ownerAppUserPo);
            
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("删除失败");
            }

            // TODO: 应该删除用户，但目前注释掉了相关代码
            // UserPo userPo = new UserPo();
            // userPo.setUserId(ownerAppUser.getUserId());
            // userV1InnerServiceSMOImpl.deleteUser(userPo);
        }
    }
}