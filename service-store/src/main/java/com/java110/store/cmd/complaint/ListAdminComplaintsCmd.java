/**
 * 投诉管理命令类 - 管理员投诉列表查询
 * 
 * 该类用于处理管理员查询投诉列表的请求，包含投诉数据查询、照片信息刷新、处理人员查询等功能
 * 通过注解@Java110Cmd声明服务代码为"complaint.listAdminComplaints"
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.store.cmd.complaint;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.complaint.ComplaintDto;
import com.java110.dto.complaint.ComplaintTypeUserDto;
import com.java110.dto.file.FileRelDto;
import com.java110.intf.common.IComplaintUserInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.store.IComplaintTypeUserV1InnerServiceSMO;
import com.java110.intf.store.IComplaintV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "complaint.listAdminComplaints")
public class ListAdminComplaintsCmd extends Cmd {

    /** 投诉服务接口 */
    @Autowired
    private IComplaintV1InnerServiceSMO complaintV1InnerServiceSMOImpl;

    /** 房间服务接口 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /** 投诉用户服务接口 */
    @Autowired
    private IComplaintUserInnerServiceSMO complaintUserInnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /** 业主服务接口 */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /** 投诉类型用户关联服务接口 */
    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /** 员工社区关联服务接口 */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证分页信息和管理员权限
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法 - 查询管理员投诉列表
     * 
     * 主要功能：
     * 1. 查询员工管理的社区ID列表
     * 2. 查询投诉数据总数和列表
     * 3. 刷新投诉照片信息
     * 4. 查询处理人员信息
     * 5. 构建返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为投诉DTO对象
        ComplaintDto complaintDto = BeanConvertUtil.covertBean(reqJson, ComplaintDto.class);
        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询员工管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工有管理的社区，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            complaintDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 设置店铺ID为空，查询所有店铺的投诉
        complaintDto.setStoreId("");
        // 查询投诉总数
        int count = complaintV1InnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        List<ComplaintDto> complaintDtos = null;
        
        // 如果有投诉数据，查询投诉列表并刷新照片信息
        if (count > 0) {
            complaintDtos = complaintV1InnerServiceSMOImpl.queryComplaints(complaintDto);
            refreshPhotos(complaintDtos);
        } else {
            // 没有数据时返回空列表
            complaintDtos = new ArrayList<>();
        }

        // 查询投诉对应的处理人员信息
        toQueryStaff(complaintDtos);

        // 构建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, complaintDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询投诉处理人员方法
     * 
     * 根据投诉类型查询对应的处理人员信息
     * 只对未完成的投诉分配处理人员
     * 
     * @param complaintDtos 投诉数据列表
     */
    private void toQueryStaff(List<ComplaintDto> complaintDtos) {

        // 如果投诉列表为空，直接返回
        if (ListUtil.isNull(complaintDtos)) {
            return;
        }

        // 收集所有投诉的类型编码
        List<String> typeCds = new ArrayList<>();
        for (ComplaintDto complaintDto : complaintDtos) {
            typeCds.add(complaintDto.getTypeCd());
        }

        // 如果类型编码列表为空，直接返回
        if (ListUtil.isNull(typeCds)) {
            return;
        }

        // 构建投诉类型用户查询条件
        ComplaintTypeUserDto complaintTypeUserDto = new ComplaintTypeUserDto();
        complaintTypeUserDto.setTypeCds(typeCds.toArray(new String[typeCds.size()]));

        // 查询投诉类型对应的用户信息
        List<ComplaintTypeUserDto> complaintTypeUserDtos = complaintTypeUserV1InnerServiceSMOImpl.queryComplaintTypeUsers(complaintTypeUserDto);

        // 如果查询结果为空，直接返回
        if (ListUtil.isNull(complaintTypeUserDtos)) {
            return;
        }
        
        List<ComplaintTypeUserDto> staffs = null;
        // 为每个投诉分配对应的处理人员
        for (ComplaintDto complaintDto : complaintDtos) {
            staffs = new ArrayList<>();
            // 如果投诉已完成，跳过处理人员分配
            if (ComplaintDto.STATE_FINISH.equals(complaintDto.getState())) {
                continue;
            }
            // 根据投诉类型匹配处理人员
            for (ComplaintTypeUserDto complaintTypeUserDto1 : complaintTypeUserDtos) {
                if (complaintDto.getTypeCd().equals(complaintTypeUserDto1.getTypeCd())) {
                    staffs.add(complaintTypeUserDto1);
                }
            }
            // 设置处理人员列表到投诉对象
            complaintDto.setStaffs(staffs);
        }
    }

    /**
     * 刷新投诉照片信息方法
     * 
     * 为每个投诉查询关联的照片信息并设置到投诉对象中
     * 
     * @param complaints 投诉数据列表
     */
    private void refreshPhotos(List<ComplaintDto> complaints) {
        List<PhotoVo> photoVos = null;
        PhotoVo photoVo = null;
        // 遍历所有投诉记录
        for (ComplaintDto complaintDataVo : complaints) {
            // 构建文件关联查询条件
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setObjId(complaintDataVo.getComplaintId()); // 设置投诉ID作为关联对象ID
            fileRelDto.setRelTypeCd("13000"); // 设置关联类型编码
            // 查询文件关联信息
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            photoVos = new ArrayList<>();
            // 将文件信息转换为照片VO对象
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                photoVo.setUrl(tmpFileRelDto.getFileRealName()); // 设置文件真实路径作为URL
                photoVos.add(photoVo);
            }
            // 设置照片列表到投诉对象
            complaintDataVo.setPhotos(photoVos);
        }
    }
}