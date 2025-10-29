package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.area.AreaDto;
import com.java110.dto.community.CommunityDto;
import com.java110.intf.common.IAreaInnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 运营查询小区命令类
 * 该命令用于查询管理员权限下的小区列表信息，支持分页查询和区域信息处理
 * 主要功能包括：权限验证、小区数据查询、区域信息刷新等
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode community.listAdminCommunitys
 */
@Java110Cmd(serviceCode = "community.listAdminCommunitys")
public class ListAdminCommunitysCmd extends Cmd {

    /**
     * 区域信息服务接口，用于获取省市区信息
     */
    @Autowired
    private IAreaInnerServiceSMO areaInnerServiceSMOImpl;

    /**
     * 小区信息服务接口，用于查询小区数据
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 员工小区关联服务接口，用于查询员工管理的小区ID列表
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和用户权限
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 查询管理员权限下的小区列表，并返回分页结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为CommunityDto对象
        CommunityDto communityDto = BeanConvertUtil.covertBean(reqJson, CommunityDto.class);

        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询该员工管理的所有小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工有管理的小区，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询满足条件的小区总数
        int count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);

        List<CommunityDto> communityDtos = null;
        // 如果有数据，查询小区列表并刷新区域信息
        if (count > 0) {
            communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
            // 刷新小区的城市名称信息
            refreshCommunityCity(communityDtos);
        } else {
            // 没有数据时返回空列表
            communityDtos = new ArrayList<>();
        }

        // 构造分页结果对象
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, communityDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新小区城市名称信息
     * 根据小区的城市编码查询对应的省市区信息，并拼接成完整的城市名称
     *
     * @param communitys 小区数据列表，方法会修改其中的cityName属性
     */
    private void refreshCommunityCity(List<CommunityDto> communitys) {
        // 收集所有小区的城市编码
        List<String> areaCodes = new ArrayList<>();
        for (CommunityDto communityDataVo : communitys) {
            areaCodes.add(communityDataVo.getCityCode());
        }
        
        // 如果没有城市编码，直接返回
        if (ListUtil.isNull(areaCodes)) {
            return;
        }
        
        // 查询省市区信息
        AreaDto areaDto = new AreaDto();
        areaDto.setAreaCodes(areaCodes.toArray(new String[areaCodes.size()]));
        List<AreaDto> areaDtos = areaInnerServiceSMOImpl.getProvCityArea(areaDto);
        
        // 为每个小区设置城市名称
        for (CommunityDto communityDataVo : communitys) {
            boolean found = false;
            // 遍历区域信息，匹配城市编码
            for (AreaDto tmpAreaDto : areaDtos) {
                if (communityDataVo.getCityCode().equals(tmpAreaDto.getAreaCode())) {
                    // 拼接省市区名称
                    communityDataVo.setCityName(tmpAreaDto.getProvName() + tmpAreaDto.getCityName() + tmpAreaDto.getAreaName());
                    found = true;
                    break;
                }
            }
            // 如果没有找到对应的区域信息，设置为"未知"
            if (!found) {
                communityDataVo.setCityName("未知");
            }
        }
    }

}