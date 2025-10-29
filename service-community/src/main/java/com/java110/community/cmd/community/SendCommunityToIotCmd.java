package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityMemberDto;
import com.java110.dto.data.DatabusDataDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.community.ICommunityMemberV1InnerServiceSMO;
import com.java110.intf.job.IDataBusInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 同步小区数据到物联网项目命令类
 * 
 * 该命令类负责将小区相关数据同步到物联网平台，包括小区基本信息、物业公司信息和业主信息等。
 * 在执行同步操作前会进行数据校验，确保小区数据的完整性和有效性。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
@Java110Cmd(serviceCode = "community.sendCommunityToIot")
public class SendCommunityToIotCmd extends Cmd {

    /**
     * 小区成员服务接口，用于查询小区关联的物业公司信息
     */
    @Autowired
    private ICommunityMemberV1InnerServiceSMO communityMemberV1InnerServiceSMOImpl;

    /**
     * 业主服务接口，用于查询小区内的业主信息
     */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 数据总线服务接口，用于将小区数据发送到物联网平台
     */
    @Autowired
    private IDataBusInnerServiceSMO dataBusInnerServiceSMOImpl;

    /**
     * 命令参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括：
     * 1. 检查小区ID是否存在
     * 2. 验证小区是否关联了物业公司
     * 3. 检查小区中是否存在业主信息
     * 
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param context 命令数据流上下文，用于获取和设置命令执行过程中的数据
     * @param reqJson 请求参数JSON对象，包含前端传递的参数数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");

        // 查询小区是否关联了物业公司
        CommunityMemberDto communityMemberDto = new CommunityMemberDto();
        communityMemberDto.setCommunityId(reqJson.getString("communityId"));
        // 设置成员类型为物业公司类型
        communityMemberDto.setMemberTypeCd(CommunityMemberDto.MEMBER_TYPE_PROPERTY);
        // 执行查询，获取小区关联的物业公司列表
        List<CommunityMemberDto> communityMemberDtos = communityMemberV1InnerServiceSMOImpl.queryCommunityMembers(communityMemberDto);

        // 如果小区未关联物业公司，则抛出异常，不允许同步数据
        if (ListUtil.isNull(communityMemberDtos)) {
            throw new CmdException("小区未关联物业公司，不能同步数据");
        }

        // 检查小区中是否存在业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        // 查询小区内的业主数量
        int count = ownerV1InnerServiceSMOImpl.queryOwnersCount(ownerDto);

        // 如果小区中没有业主信息，则抛出异常，提示先维护基础数据
        if (count < 1) {
            throw new CmdException("小区中不存在业主信息，请您先维护物业基础数据后，再同步");
        }
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责将小区数据通过数据总线发送到物联网平台。
     * 在执行前需要确保validate方法已通过所有验证。
     * 
     * @param event 命令事件对象，包含命令执行的相关信息
     * @param context 命令数据流上下文，用于获取和设置命令执行过程中的数据
     * @param reqJson 请求参数JSON对象，包含前端传递的参数数据
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     * @throws ParseException 当数据解析过程中发生错误时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 创建数据总线数据传输对象，设置业务类型为同步小区数据到物联网
        DatabusDataDto databusDataDto = new DatabusDataDto(
            DatabusDataDto.BUSINESS_TYPE_SEND_COMMUNITY_DATA_TO_IOT, 
            reqJson
        );
        
        // 通过数据总线服务将小区数据发送到物联网平台
        dataBusInnerServiceSMOImpl.databusData(databusDataDto);

        // 设置响应结果为成功状态
        context.setResponseEntity(ResultVo.success());
    }
}