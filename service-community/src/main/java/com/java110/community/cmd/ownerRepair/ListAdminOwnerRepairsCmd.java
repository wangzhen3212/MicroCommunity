/**
 * 管理员业主报修列表查询命令类
 * 
 * 该类用于处理管理员查询业主报修列表的请求，包括报修信息的查询、评分计算、社区信息刷新、图片信息处理等功能
 * 通过注解@Java110Cmd声明服务代码为"ownerRepair.listAdminOwnerRepairs"
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.listAdminOwnerRepairs")
public class ListAdminOwnerRepairsCmd extends Cmd {

    /** 报修信息服务接口 */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /** 报修用户关联服务接口 */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /** 业主信息服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 社区信息服务接口 */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /** 员工社区关联服务接口 */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和管理员权限验证
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 验证分页参数
        super.validateAdmin(context); // 验证管理员权限
    }

    /**
     * 执行命令方法
     * 
     * 处理管理员查询业主报修列表的主要业务逻辑，包括：
     * 1. 参数转换和条件设置
     * 2. 查询报修数据
     * 3. 计算报修评分
     * 4. 刷新报修相关信息
     * 5. 返回查询结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当时间解析错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为RepairDto对象
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);

        // 获取当前登录员工ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询员工管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工有管理的社区，设置查询条件
        if (!ListUtil.isNull(communityIds)) {
            ownerRepairDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 处理时间参数
        ifHasTime(ownerRepairDto);
        
        // 处理房间ID参数，支持逗号分隔的多个房间ID
        if (!StringUtil.isEmpty(ownerRepairDto.getRoomId()) && ownerRepairDto.getRoomId().contains(",")) {
            String[] roomIds = ownerRepairDto.getRoomId().split(",");
            ownerRepairDto.setRoomIds(roomIds);
            ownerRepairDto.setRoomId(""); // 清空单个房间ID
        }

        // 查询报修记录总数
        int count = repairInnerServiceSMOImpl.queryRepairsCount(ownerRepairDto);
        List<RepairDto> repairDtos;
        
        if (count > 0) {
            // 查询报修记录列表
            repairDtos = repairInnerServiceSMOImpl.queryRepairs(ownerRepairDto);
            // 计算报修评分
            computeRepairScore(repairDtos);
            // 刷新报修相关信息（维修人员、图片等）
            refreshRepair(repairDtos);
            // 刷新社区名称
            refreshCommunityName(repairDtos);
        } else {
            repairDtos = new ArrayList<>();
        }
        
        // 构建响应结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
            count, // 总记录数
            repairDtos // 数据列表
        );
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算报修评分
     * 
     * 计算每个报修记录的综合评分，包括：
     * 1. 综合评价得分
     * 2. 上门速度评分
     * 3. 维修员服务评分
     * 4. 平均分
     * 5. 提单时长
     * 
     * @param repairDtos 报修记录列表
     */
    private void computeRepairScore(List<RepairDto> repairDtos) {
        if (ListUtil.isNull(repairDtos)) {
            return;
        }
        
        Date finishTime = null;
        String submitHour;
        Date timeout = null;
        
        for (RepairDto repairDto : repairDtos) {
            // 获取综合评价得分并转换为整数
            String appraiseScoreNumber = repairDto.getAppraiseScore();
            Double appraiseScoreNum = 0.0;
            if (!StringUtil.isEmpty(appraiseScoreNumber)) {
                appraiseScoreNum = Double.parseDouble(appraiseScoreNumber);
            }
            int appraiseScore = (int) Math.ceil(appraiseScoreNum);
            
            // 获取上门速度评分并转换为整数
            String doorSpeedScoreNumber = repairDto.getDoorSpeedScore();
            Double doorSpeedScoreNum = 0.0;
            if (!StringUtil.isEmpty(doorSpeedScoreNumber)) {
                doorSpeedScoreNum = Double.parseDouble(doorSpeedScoreNumber);
            }
            int doorSpeedScore = (int) Math.ceil(doorSpeedScoreNum);
            
            // 获取维修员服务评分并转换为整数
            String repairmanServiceScoreNumber = repairDto.getRepairmanServiceScore();
            Double repairmanServiceScoreNum = 0.0;
            if (!StringUtil.isEmpty(repairmanServiceScoreNumber)) {
                repairmanServiceScoreNum = Double.parseDouble(repairmanServiceScoreNumber);
            }
            int repairmanServiceScore = (int) Math.ceil(repairmanServiceScoreNum);
            
            // 计算平均分，保留两位小数
            double averageNumber = (appraiseScoreNum + doorSpeedScoreNum + repairmanServiceScoreNum) / 3.0;
            BigDecimal averageNum = new BigDecimal(averageNumber);
            Double average = averageNum.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            
            // 设置评分结果
            repairDto.setAppraiseScore(String.valueOf(appraiseScore));
            repairDto.setDoorSpeedScore(String.valueOf(doorSpeedScore));
            repairDto.setRepairmanServiceScore(String.valueOf(repairmanServiceScore));
            repairDto.setAverage(String.valueOf(average));

            // 计算提单时长
            finishTime = DateUtil.getCurrentDate();
            if (!StringUtil.isEmpty(repairDto.getFinishTime())) {
                // 如果有完成时间，使用完成时间
                finishTime = DateUtil.getDateFromStringA(repairDto.getFinishTime());
            } else {
                // 如果没有完成时间，检查是否超时
                timeout = DateUtil.getDateFromStringA(repairDto.getTimeout());
                if (finishTime.getTime() > timeout.getTime()) {
                    repairDto.setStateName(repairDto.getStateName() + "(超时)"); // 标记超时状态
                }
            }
            
            // 计算从创建时间到完成/当前时间的时长
            submitHour = DateUtil.calculateTimeDifference(repairDto.getCreateTime(), finishTime);
            repairDto.setSubmitHours(submitHour);
        }
    }

    /**
     * 刷新社区名称
     * 
     * 根据社区ID查询社区名称并设置到报修记录中
     * 
     * @param ownerRepairs 报修记录列表
     */
    private void refreshCommunityName(List<RepairDto> ownerRepairs) {
        if(ListUtil.isNull(ownerRepairs)){
            return;
        }

        // 收集所有社区ID
        List<String> communityIds = new ArrayList<>();
        for (RepairDto repairDto : ownerRepairs) {
            communityIds.add(repairDto.getCommunityId());
        }

        if(ListUtil.isNull(communityIds)){
            return ;
        }
        
        // 查询社区信息
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        
        if(ListUtil.isNull(communityDtos)){
            return;
        }
        
        // 设置社区名称到报修记录
        for (RepairDto tmpRepairDto : ownerRepairs) {
            for (CommunityDto tCommunityDto : communityDtos) {
                if (!tmpRepairDto.getCommunityId().equals(tCommunityDto.getCommunityId())) {
                    continue;
                }
                tmpRepairDto.setCommunityName(tCommunityDto.getName());
            }
        }
    }

    /**
     * 处理时间参数
     * 
     * 如果结束时间不包含具体时间，则补充为当天的23:59:59
     * 
     * @param ownerRepairDto 报修查询条件对象
     */
    private void ifHasTime(RepairDto ownerRepairDto) {
        if (StringUtil.isEmpty(ownerRepairDto.getEndTime())) {
            return;
        }

        String endTime = ownerRepairDto.getEndTime();
        // 如果已经包含具体时间，直接返回
        if (endTime.contains(":")) {
            return;
        }

        // 补充具体时间到当天结束
        endTime += " 23:59:59";
        ownerRepairDto.setEndTime(endTime);
    }

    /**
     * 刷新报修相关信息
     * 
     * 刷新报修记录的维修人员信息和图片信息，包括：
     * 1. 维修人员信息
     * 2. 维修图片
     * 3. 维修前图片
     * 4. 维修后图片
     * 
     * @param ownerRepairs 报修记录列表
     */
    private void refreshRepair(List<RepairDto> ownerRepairs) {
        // 收集所有报修ID
        List<String> repairIds = new ArrayList<>();
        for (RepairDto apiOwnerRepairDataVo : ownerRepairs) {
            repairIds.add(apiOwnerRepairDataVo.getRepairId());
        }
        
        if (ListUtil.isNull(repairIds)) {
            return;
        }
        
        // 查询维修人员信息
        RepairUserDto repairUserDto = new RepairUserDto();
        repairUserDto.setRepairIds(repairIds.toArray(new String[repairIds.size()]));
        List<RepairUserDto> repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
        
        // 设置维修人员信息到报修记录
        for (RepairUserDto tmpRepairUserDto : repairUserDtos) {
            for (RepairDto apiOwnerRepairDataVo : ownerRepairs) {
                if (tmpRepairUserDto.getRepairId().equals(apiOwnerRepairDataVo.getRepairId())) {
                    apiOwnerRepairDataVo.setStaffId(tmpRepairUserDto.getUserId());
                    // 注释掉的代码：设置维修人员姓名
                    //apiOwnerRepairDataVo.setStatmpRepairUserDto.getUserName());
                }
            }
        }
        
        // 刷新图片信息
        List<PhotoVo> photoVos = null;
        List<PhotoVo> repairPhotos = null;  // 业主上传维修图片
        List<PhotoVo> beforePhotos = null;  // 维修前图片
        List<PhotoVo> afterPhotos = null;   // 维修后图片
        PhotoVo photoVo = null;
        
        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 处理每个报修记录的图片信息
        for (RepairDto repairDto : ownerRepairs) {
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(repairDto.getRepairId());
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            photoVos = new ArrayList<>();
            repairPhotos = new ArrayList<>();
            beforePhotos = new ArrayList<>();
            afterPhotos = new ArrayList<>();
            
            // 分类处理不同类型的图片
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                photoVo.setUrl(tmpFileRelDto.getFileRealName());
                photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                photoVos.add(photoVo);
                
                // 根据关联类型分类处理图片
                if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.REL_TYPE_CD_REPAIR)) {  
                    // 维修图片
                    photoVo = new PhotoVo();
                    if (!tmpFileRelDto.getFileRealName().startsWith("http")) {
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName()); // 拼接完整URL
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    repairPhotos.add(photoVo);  // 添加到维修图片列表
                } else if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.BEFORE_REPAIR_PHOTOS)) {  
                    // 维修前图片
                    photoVo = new PhotoVo();
                    if (!tmpFileRelDto.getFileRealName().startsWith("http")) {
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName());
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    beforePhotos.add(photoVo);  // 添加到维修前图片列表
                } else if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.AFTER_REPAIR_PHOTOS)) {  
                    // 维修后图片
                    photoVo = new PhotoVo();
                    if (!tmpFileRelDto.getFileRealName().startsWith("http")) {
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName());
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    afterPhotos.add(photoVo); // 添加到维修后图片列表
                }
            }
            
            // 设置图片信息到报修记录
            repairDto.setPhotos(photoVos);          // 所有图片
            repairDto.setRepairPhotos(repairPhotos); // 维修图片
            repairDto.setBeforePhotos(beforePhotos); // 维修前图片
            repairDto.setAfterPhotos(afterPhotos);   // 维修后图片
        }
    }
}