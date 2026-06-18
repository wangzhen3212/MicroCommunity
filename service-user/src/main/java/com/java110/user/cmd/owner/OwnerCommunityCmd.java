package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 业主社区关联命令处理类
 * 主要功能：根据联系电话查询业主信息，关联社区信息，并处理业主与APP用户的关联关系
 * 实现业主社区信息的查询、关联和同步功能
 */
@Java110Cmd(serviceCode = "owner.ownerCommunity")
public class OwnerCommunityCmd extends Cmd {
    private final static Logger logger = LoggerFactory.getLogger(OwnerCommunityCmd.class);

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的联系电话字段
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson           请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "link", "未包含联系电话");
    }

    /**
     * 主要业务逻辑处理方法
     * 根据联系电话查询业主信息，关联社区信息，并同步业主与APP用户的关联关系
     *
     * @param event             命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson           请求的JSON数据对象
     * @throws CmdException 当业务处理出现异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 根据联系电话查询业主成员信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setLink(reqJson.getString("link"));
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnerMembers(ownerDto);

        // 如果未查询到业主信息，直接返回成功响应
        if (ownerDtos == null || ownerDtos.size() < 1) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 收集所有业主所属的社区ID
        List<String> communityIds = new ArrayList<>();
        for (OwnerDto tmpOwnerDto : ownerDtos) {
            communityIds.add(tmpOwnerDto.getCommunityId());
            tmpOwnerDto.setAppUserName(tmpOwnerDto.getName()); // 设置APP用户名为业主姓名
        }

        // 根据社区ID查询社区详细信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setState("1100"); // 设置社区状态为有效
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 如果未查询到社区信息，直接返回成功响应
        if (communityDtos == null || communityDtos.size() < 1) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 将社区信息关联到对应的业主信息中
        for (OwnerDto tmpOwnerDto : ownerDtos) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (!tmpCommunityDto.getCommunityId().equals(tmpOwnerDto.getCommunityId())) {
                    continue; // 社区ID不匹配，跳过当前循环
                }
                // 设置社区相关信息到业主对象
                tmpOwnerDto.setCommunityName(tmpCommunityDto.getName());
                tmpOwnerDto.setsCommunityTel(tmpCommunityDto.getTel());
                tmpOwnerDto.setCommunityQrCode(tmpCommunityDto.getQrCode());
            }
        }

        // 查询业主APP用户关联关系
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setLink(reqJson.getString("link"));
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 如果不存在业主APP用户关联关系，直接返回业主信息
        if(ListUtil.isNull(ownerAppUserDtos)){
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(ownerDtos));
            return;
        }

        // 检查并补充缺失的业主APP用户关联关系
        for (OwnerDto tmpOwnerDto : ownerDtos) {
            if (hasOwnerAppUser(tmpOwnerDto, ownerAppUserDtos)) {
                continue; // 已存在关联关系，跳过
            }
            addOwnerAppUser(tmpOwnerDto, ownerAppUserDtos.get(0)); // 添加新的关联关系
        }

        // 返回处理后的业主信息
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(ownerDtos));
    }

    /**
     * 检查业主是否已存在APP用户关联关系
     *
     * @param ownerDto          业主信息对象
     * @param ownerAppUserDtos  APP用户关联关系列表
     * @return boolean  true-存在关联关系，false-不存在关联关系
     */
    private boolean hasOwnerAppUser(OwnerDto ownerDto, List<OwnerAppUserDto> ownerAppUserDtos) {
        // 如果关联关系列表为空，直接返回false
        if (ownerAppUserDtos == null || ownerAppUserDtos.size() < 1) {
            return false;
        }

        // 遍历关联关系列表，检查是否存在匹配的关联关系
        for (OwnerAppUserDto ownerAppUserDto : ownerAppUserDtos) {
            if (ownerDto.getLink().equals(ownerAppUserDto.getLink())
                    && ownerDto.getMemberId().equals(ownerAppUserDto.getMemberId())) {
                return true; // 找到匹配的关联关系
            }
        }

        return false; // 未找到匹配的关联关系
    }

    /**
     * 添加业主APP用户关联关系
     * 将业主信息同步到APP用户关联表中
     *
     * @param ownerDto         业主信息对象
     * @param ownerAppUserDto  APP用户关联模板对象
     * @throws CmdException 当添加关联关系失败时抛出
     */
    private void addOwnerAppUser(OwnerDto ownerDto, OwnerAppUserDto ownerAppUserDto) {
        // 将DTO对象转换为PO对象
        OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(ownerAppUserDto, OwnerAppUserPo.class);
        
        // 设置APP用户ID和业主相关信息
        ownerAppUserPo.setAppUserId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_appUserId));
        ownerAppUserPo.setMemberId(ownerDto.getMemberId());
        ownerAppUserPo.setCommunityName(ownerDto.getCommunityName());
        ownerAppUserPo.setCommunityId(ownerDto.getCommunityId());
        ownerAppUserPo.setAppUserName(ownerDto.getName());
        ownerAppUserPo.setIdCard(ownerDto.getIdCard());
        ownerAppUserPo.setOwnerTypeCd(ownerDto.getOwnerTypeCd());
        
        // 保存业主APP用户关联关系
        int flag = ownerAppUserV1InnerServiceSMOImpl.saveOwnerAppUser(ownerAppUserPo);
        if (flag < 1) {
            throw new CmdException("添加用户业主关系失败"); // 保存失败抛出异常
        }
    }
}