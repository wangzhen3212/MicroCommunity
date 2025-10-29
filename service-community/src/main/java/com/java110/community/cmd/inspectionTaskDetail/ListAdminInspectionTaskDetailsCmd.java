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

/**
 * 巡检任务详情管理命令类
 * 
 * 该类用于处理巡检任务详情的查询请求，支持管理员查看巡检任务详情列表，
 * 包括任务详情的基本信息和相关照片信息
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode inspectionTaskDetail.listAdminInspectionTaskDetails
 */
@Java110Cmd(serviceCode = "inspectionTaskDetail.listAdminInspectionTaskDetails")
public class ListAdminInspectionTaskDetailsCmd extends Cmd {

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
     * 验证请求参数的有效性，包括分页参数验证和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 
     * 处理巡检任务详情的查询请求，返回分页的巡检任务详情列表，
     * 包括任务详情的基本信息和相关照片信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检任务详情DTO对象
        InspectionTaskDetailDto inspectionTaskDetailDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDetailDto.class);
        
        // 查询巡检任务详情总数
        int count = inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetailsCount(inspectionTaskDetailDto);
        
        List<ApiInspectionTaskDetailDataVo> inspectionTaskDetails = null;
        
        // 如果存在数据，则查询详情列表并刷新照片信息
        if (count > 0) {
            inspectionTaskDetails = BeanConvertUtil.covertBeanList(
                inspectionTaskDetailInnerServiceSMOImpl.queryInspectionTaskDetails(inspectionTaskDetailDto), 
                ApiInspectionTaskDetailDataVo.class
            );
            // 刷新照片信息
            refreshPhotos(inspectionTaskDetails);
        } else {
            // 如果没有数据，返回空列表
            inspectionTaskDetails = new ArrayList<>();
        }
        
        // 构建返回VO对象
        ApiInspectionTaskDetailVo apiInspectionTaskDetailVo = new ApiInspectionTaskDetailVo();
        apiInspectionTaskDetailVo.setTotal(count); // 设置总记录数
        apiInspectionTaskDetailVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiInspectionTaskDetailVo.setInspectionTaskDetails(inspectionTaskDetails); // 设置详情列表
        
        // 构建响应实体并设置到上下文
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionTaskDetailVo), 
            HttpStatus.OK
        );
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新照片信息方法
     * 
     * 为巡检任务详情列表中的每个任务详情加载关联的照片信息，
     * 只有当任务状态为"20200407"时才加载照片
     * 
     * @param inspectionTaskDetails 巡检任务详情数据VO列表
     */
    private void refreshPhotos(List<ApiInspectionTaskDetailDataVo> inspectionTaskDetails) {
        List<PhotoVo> photoVos = null; // 照片VO列表
        PhotoVo photoVo = null; // 单个照片VO对象
        
        // 从缓存中获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 遍历所有巡检任务详情
        for (ApiInspectionTaskDetailDataVo inspectionTaskDetail : inspectionTaskDetails) {
            // 只有当状态为"20200407"时才处理照片信息
            if (!"20200407".equals(inspectionTaskDetail.getState())) {
                continue; // 跳过不符合状态的任务详情
            }
            
            // 构建文件关联查询条件
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(inspectionTaskDetail.getTaskDetailId()); // 设置关联对象ID为任务详情ID
            
            // 查询文件关联信息
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            photoVos = new ArrayList<>(); // 初始化照片列表
            
            // 遍历文件关联信息，构建照片VO列表
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                // 设置照片URL：如果已经是完整URL则直接使用，否则拼接域名
                photoVo.setUrl(
                    tmpFileRelDto.getFileRealName().startsWith("http") 
                        ? tmpFileRelDto.getFileRealName() 
                        : imgUrl + tmpFileRelDto.getFileRealName()
                );
                photoVos.add(photoVo); // 添加到照片列表
            }
            
            // 将照片列表设置到任务详情中
            inspectionTaskDetail.setPhotos(photoVos);
        }
    }
}