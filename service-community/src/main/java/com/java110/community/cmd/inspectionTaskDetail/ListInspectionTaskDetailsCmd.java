/**
 * 巡检任务详情查询命令类
 * 
 * 该类负责处理巡检任务详情的列表查询请求，包括分页查询、数据转换和照片信息刷新等功能
 * 通过注解@Java110Cmd声明服务代码，与前端请求进行映射
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.inspectionTaskDetail;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.inspection.InspectionTaskDetailDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IInspectionTaskDetailInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.inspectionTaskDetail.ApiInspectionTaskDetailDataVo;
import com.java110.vo.api.inspectionTaskDetail.ApiInspectionTaskDetailVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "inspectionTaskDetail.listInspectionTaskDetails")
public class ListInspectionTaskDetailsCmd extends Cmd {

    /**
     * 巡检任务详情内部服务接口
     */
    @Autowired
    private IInspectionTaskDetailInnerServiceSMO inspectionTaskDetailInnerServiceSMOImpl;

    /**
     * 文件关联内部服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中的必填参数，包括分页信息和小区ID
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);

        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 命令执行方法
     * 
     * 处理巡检任务详情的查询请求，包括数据查询、分页处理和照片信息刷新
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检任务详情数据传输对象
        InspectionTaskDetailDto inspectionTaskDetailDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDetailDto.class);
        
        // 查询巡检任务详情总数
        int count = inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetailsCount(inspectionTaskDetailDto);
        
        List<ApiInspectionTaskDetailDataVo> inspectionTaskDetails = null;
        
        // 如果查询到数据，则进行数据转换和照片刷新
        if (count > 0) {
            // 将查询结果转换为API响应对象列表
            inspectionTaskDetails = BeanConvertUtil.covertBeanList(
                inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(inspectionTaskDetailDto), 
                ApiInspectionTaskDetailDataVo.class);
            // 刷新照片信息
            refreshPhotos(inspectionTaskDetails);
        } else {
            // 如果没有数据，返回空列表
            inspectionTaskDetails = new ArrayList<>();
        }
        
        // 构建API响应对象
        ApiInspectionTaskDetailVo apiInspectionTaskDetailVo = new ApiInspectionTaskDetailVo();
        apiInspectionTaskDetailVo.setTotal(count); // 设置总记录数
        apiInspectionTaskDetailVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiInspectionTaskDetailVo.setInspectionTaskDetails(inspectionTaskDetails); // 设置详情数据列表
        
        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionTaskDetailVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新照片信息方法
     * 
     * 为巡检任务详情数据刷新关联的照片信息，只处理状态为"20200407"的记录
     * 构建完整的图片URL地址
     * 
     * @param inspectionTaskDetails 巡检任务详情数据列表
     */
    private void refreshPhotos(List<ApiInspectionTaskDetailDataVo> inspectionTaskDetails) {
        List<PhotoVo> photoVos = null; // 照片值对象列表
        PhotoVo photoVo = null; // 单个照片值对象
        
        // 从缓存中获取图片域名配置，用于构建完整的图片URL
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 遍历所有巡检任务详情
        for (ApiInspectionTaskDetailDataVo inspectionTaskDetail : inspectionTaskDetails) {
            // 只处理状态为"20200407"的记录（已完成状态的巡检任务）
            if (!"20200407".equals(inspectionTaskDetail.getState())) {
                continue; // 跳过不符合状态的记录
            }
            
            // 构建文件关联查询条件
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(inspectionTaskDetail.getTaskDetailId()); // 设置对象ID为任务详情ID
            
            // 查询文件关联信息
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            photoVos = new ArrayList<>(); // 初始化照片列表
            
            // 遍历文件关联信息，构建照片URL
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                // 构建完整的图片URL：如果已经是完整URL则直接使用，否则拼接域名
                String fileUrl = tmpFileRelDto.getFileRealName().startsWith("http") ? 
                    tmpFileRelDto.getFileRealName() : imgUrl + tmpFileRelDto.getFileRealName();
                photoVo.setUrl(fileUrl);
                photoVos.add(photoVo);
            }
            
            // 设置照片列表到巡检任务详情对象
            inspectionTaskDetail.setPhotos(photoVos);
        }
    }
}