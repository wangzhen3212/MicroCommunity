/**
 * 业主报修查询命令类
 * 
 * 该类用于处理业主报修列表查询请求，主要功能包括：
 * 1. 查询业主报修记录
 * 2. 计算报修评分和平均分
 * 3. 处理报修相关图片信息
 * 4. 关联维修人员信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
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

@Java110Cmd(serviceCode = "ownerRepair.listOwnerRepairs")
public class ListOwnerRepairsCmd extends Cmd {

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 验证分页信息
        super.validateProperty(context); // 验证物业信息
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息"); // 验证小区ID必填
    }

    /**
     * 执行命令方法 - 查询业主报修列表
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        hasOwnerId(reqJson); // 处理业主ID信息
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class); // 转换请求参数为RepairDto对象
        
        // 处理时间参数
        ifHasTime(ownerRepairDto);
        
        // 处理房间ID参数，支持多个房间ID查询
        if (!StringUtil.isEmpty(ownerRepairDto.getRoomId()) && ownerRepairDto.getRoomId().contains(",")) {
            String[] roomIds = ownerRepairDto.getRoomId().split(",");
            ownerRepairDto.setRoomIds(roomIds);
            ownerRepairDto.setRoomId("");
        }

        // PC电话报修模块特殊处理，只返回PC员工登记和手机端员工登记的数据
        if ("pc_mobile".equals(ownerRepairDto.getReqSource())) {
            String[] repair_channel = {RepairDto.REPAIR_CHANNEL_STAFF, RepairDto.REPAIR_CHANNEL_TEL};
            ownerRepairDto.setRepairChannels(Arrays.asList(repair_channel));
        }
        
        // 查询报修记录总数
        int count = repairInnerServiceSMOImpl.queryRepairsCount(ownerRepairDto);
        List<RepairDto> repairDtos;
        
        if (count > 0) {
            // 查询报修记录列表
            repairDtos = repairInnerServiceSMOImpl.queryRepairs(ownerRepairDto);
            computeRepairScore(repairDtos); // 计算评分
            refreshRepair(repairDtos); // 刷新报修信息（维修人员和图片）

        } else {
            repairDtos = new ArrayList<>();
        }
        
        // 构建响应结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) reqJson.getInteger("row")),
                count, repairDtos);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 计算报修评分和平均分
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
            
            // 计算三个评分的平均值，保留两位小数
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
                finishTime = DateUtil.getDateFromStringA(repairDto.getFinishTime());
            } else {
                // 如果未完成，检查是否超时
                timeout = DateUtil.getDateFromStringA(repairDto.getTimeout());
                if (finishTime.getTime() > timeout.getTime()) {
                    repairDto.setStateName(repairDto.getStateName() + "(超时)");
                }
            }
            // 计算从创建时间到完成时间的时长
            submitHour = DateUtil.calculateTimeDifference(repairDto.getCreateTime(), finishTime);
            repairDto.setSubmitHours(submitHour);

        }
    }

    /**
     * 处理时间参数
     * 
     * 如果结束时间不包含时分秒，则自动添加23:59:59
     * 
     * @param ownerRepairDto 报修查询参数对象
     */
    private void ifHasTime(RepairDto ownerRepairDto) {

        if (StringUtil.isEmpty(ownerRepairDto.getEndTime())) {
            return;
        }

        String endTime = ownerRepairDto.getEndTime();
        if (endTime.contains(":")) {
            return;
        }

        // 为日期添加时间部分，使其包含当天的最后一刻
        endTime += " 23:59:59";
        ownerRepairDto.setEndTime(endTime);
    }

    /**
     * 处理业主ID信息
     * 
     * 根据业主ID查询业主信息，并将联系电话设置到请求参数中
     * 
     * @param reqJson 请求参数JSON对象
     */
    private void hasOwnerId(JSONObject reqJson) {
        String ownerId = reqJson.getString("ownerId");
        if (StringUtil.isEmpty(ownerId)) {
            return;
        }
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setMemberId(reqJson.getString("ownerId"));
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        // 查询业主信息
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);
        if (!ListUtil.isNull(ownerDtos)) {
            // 将业主联系电话设置到请求参数中
            reqJson.put("tel", ownerDtos.get(0).getLink());
        }

    }

    /**
     * 刷新报修信息
     * 
     * 关联维修人员信息和图片信息到报修记录中
     * 
     * @param ownerRepairs 报修记录列表
     */
    private void refreshRepair(List<RepairDto> ownerRepairs) {
        if (ListUtil.isNull(ownerRepairs)) {
            return;
        }
        List<String> repairIds = new ArrayList<>();
        // 收集所有报修ID
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
        
        // 关联维修人员到报修记录
        for (RepairUserDto tmpRepairUserDto : repairUserDtos) {
            for (RepairDto apiOwnerRepairDataVo : ownerRepairs) {
                if (tmpRepairUserDto.getRepairId().equals(apiOwnerRepairDataVo.getRepairId())) {
                    apiOwnerRepairDataVo.setStaffId(tmpRepairUserDto.getUserId());
                    //apiOwnerRepairDataVo.setStatmpRepairUserDto.getUserName());
                }
            }
        }
        
        // 刷入图片信息
        List<PhotoVo> photoVos = null;
        List<PhotoVo> repairPhotos = null;  // 业主上传维修图片
        List<PhotoVo> beforePhotos = null;  // 维修前图片
        List<PhotoVo> afterPhotos = null;   // 维修后图片
        PhotoVo photoVo = null;
        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        for (RepairDto repairDto : ownerRepairs) {
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(repairDto.getRepairId());
            // 查询文件关联关系
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            photoVos = new ArrayList<>();
            repairPhotos = new ArrayList<>();
            beforePhotos = new ArrayList<>();
            afterPhotos = new ArrayList<>();
            
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                photoVo.setUrl(tmpFileRelDto.getFileRealName());
                photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                photoVos.add(photoVo);
                
                // 根据关联类型分类图片
                if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.REL_TYPE_CD_REPAIR)) {  
                    // 维修图片
                    photoVo = new PhotoVo();
                    if (!tmpFileRelDto.getFileRealName().startsWith("http")) {
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName()); // 添加域名前缀
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    repairPhotos.add(photoVo);  // 维修图片
                } else if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.BEFORE_REPAIR_PHOTOS)) {  
                    // 维修前图片
                    photoVo = new PhotoVo();
                    if (!tmpFileRelDto.getFileRealName().startsWith("http")) {
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName()); // 添加域名前缀
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    beforePhotos.add(photoVo);  // 维修前图片
                } else if (tmpFileRelDto.getRelTypeCd().equals(FileRelDto.AFTER_REPAIR_PHOTOS)) {  
                    // 维修后图片
                    photoVo = new PhotoVo();
                    if (!tmpFileRelDto.getFileRealName().startsWith("http")) {
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName()); // 添加域名前缀
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    afterPhotos.add(photoVo);
                }
            }
            
            // 设置图片信息到报修记录
            repairDto.setPhotos(photoVos);
            repairDto.setRepairPhotos(repairPhotos);
            repairDto.setBeforePhotos(beforePhotos);
            repairDto.setAfterPhotos(afterPhotos);
        }
    }
}