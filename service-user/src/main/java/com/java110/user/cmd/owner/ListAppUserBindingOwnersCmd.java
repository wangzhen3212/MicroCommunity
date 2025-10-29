package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询APP用户绑定的业主信息命令类
 * 该命令用于查询移动端APP用户绑定的业主信息列表，包括业主基本信息、所属小区信息和头像信息等
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode owner.listAppUserBindingOwners
 */
@Java110Cmd(serviceCode = "owner.listAppUserBindingOwners")
public class ListAppUserBindingOwnersCmd extends Cmd {

    /** 业主APP用户关系内部服务接口 */
    @Autowired
    private IOwnerAppUserInnerServiceSMO ownerAppUserInnerServiceSMOImpl;

    /** 小区内部服务接口 */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /** 用户内部服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 文件关联内部服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 当前方法不需要特殊验证，直接通过
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 该方法当前无需特殊参数验证
    }

    /**
     * 执行命令方法 - 查询APP用户绑定的业主信息列表
     * 根据用户ID或openId查询绑定的业主信息，并返回分页结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象，包含查询条件
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 获取请求头信息
        Map<String, String> headers = context.getReqHeaders();

        // 设置默认分页参数
        if (!reqJson.containsKey("page")) {
            reqJson.put("page", 1); // 默认第一页
        }
        if (!reqJson.containsKey("row")) {
            reqJson.put("row", 10); // 默认每页10条
        }

        // 从请求头中获取用户ID并设置到请求参数中
        if (!reqJson.containsKey("userId") && headers.containsKey("userid")) {
            reqJson.put("userId", headers.get("userid"));
        }

        // 如果通过openId查询，则清空userId参数
        if(reqJson.containsKey("openId") && !StringUtil.isEmpty(reqJson.getString("openId"))){
            reqJson.put("userId", "");
        }

        // 将请求参数转换为DTO对象
        OwnerAppUserDto ownerAppUserDto = BeanConvertUtil.covertBean(reqJson, OwnerAppUserDto.class);

        // 查询符合条件的记录总数
        int count = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsersCount(ownerAppUserDto);

        List<OwnerAppUserDto> ownerAppUserDtos = null;
        if (count > 0) {
            // 查询业主APP用户绑定信息列表
            ownerAppUserDtos = ownerAppUserInnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
            // 刷新小区区域信息和头像信息
            refreshCommunityArea(ownerAppUserDtos);
        } else {
            // 无数据时返回空列表
            ownerAppUserDtos = new ArrayList<>();
        }
        
        // 计算总页数并设置响应结果
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        context.setResponseEntity(ResultVo.createResponseEntity(totalPage, count, ownerAppUserDtos));
    }

    /**
     * 刷新小区区域信息和业主头像信息
     * 为业主绑定信息补充小区区域编码、名称和业主头像URL
     * 
     * @param ownerAppUserDtos 业主APP用户绑定信息列表
     */
    private void refreshCommunityArea(List<OwnerAppUserDto> ownerAppUserDtos) {
        // 获取所有小区ID
        String[] communityIds = getCommunityIds(ownerAppUserDtos);
        if (communityIds == null || communityIds.length < 1) {
            return; // 没有小区ID时直接返回
        }
        
        // 查询小区详细信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds);
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);

        // 为每个业主绑定信息设置小区区域信息
        for (CommunityDto tmpCommunityDto : communityDtos) {
            for (OwnerAppUserDto ownerAppUserDto : ownerAppUserDtos) {
                if (ownerAppUserDto.getCommunityId().equals(tmpCommunityDto.getCommunityId())) {
                    // 设置区域编码和名称
                    ownerAppUserDto.setAreaCode(tmpCommunityDto.getAreaCode());
                    ownerAppUserDto.setAreaName(tmpCommunityDto.getAreaName());
                    ownerAppUserDto.setParentAreaCode(tmpCommunityDto.getParentAreaCode());
                    ownerAppUserDto.setParentAreaName(tmpCommunityDto.getParentAreaName());
                    ownerAppUserDto.setCommunityQrCode(tmpCommunityDto.getQrCode());
                }
            }
        }

        // 收集需要查询头像的业主ID
        List<String> ownerIds = new ArrayList<>();
        for(OwnerAppUserDto ownerAppUserDto : ownerAppUserDtos){
            if(StringUtil.isEmpty(ownerAppUserDto.getMemberId()) || "-1".equals(ownerAppUserDto.getMemberId())){
                continue; // 跳过无效的业主ID
            }
            ownerIds.add(ownerAppUserDto.getMemberId());
        }

        if(ownerIds.size() < 1){
            return; // 没有有效的业主ID时直接返回
        }

        // 查询业主头像文件关联信息
        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjIds(ownerIds.toArray(new String[ownerIds.size()]));
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);

        if(fileRelDtos == null || fileRelDtos.size() < 1){
            return ; // 没有头像文件时直接返回
        }
        
        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN,"IMG_PATH");
        
        // 为每个业主设置头像URL
        for(OwnerAppUserDto ownerAppUserDto : ownerAppUserDtos){
            for(FileRelDto tmpFileRelDto : fileRelDtos){
                if(!ownerAppUserDto.getMemberId().equals(tmpFileRelDto.getObjId())){
                    continue; // 业主ID不匹配时跳过
                }
                // 拼接完整的头像URL
                ownerAppUserDto.setHeadImgUrl(imgUrl +tmpFileRelDto.getFileSaveName() );
            }
        }
    }

    /**
     * 从业主绑定信息列表中提取小区ID数组
     * 
     * @param ownerAppUserDtos 业主绑定信息列表
     * @return 小区ID数组，如果没有有效的小区ID则返回空数组
     */
    private String[] getCommunityIds(List<OwnerAppUserDto> ownerAppUserDtos) {
        List<String> communityIds = new ArrayList<String>();
        for (OwnerAppUserDto ownerAppUserDto : ownerAppUserDtos) {
            // 过滤无效的小区ID
            if (StringUtil.isEmpty(ownerAppUserDto.getCommunityId()) || "-1".equals(ownerAppUserDto.getCommunityId())) {
                continue;
            }
            communityIds.add(ownerAppUserDto.getCommunityId());
        }

        return communityIds.toArray(new String[communityIds.size()]);
    }
}