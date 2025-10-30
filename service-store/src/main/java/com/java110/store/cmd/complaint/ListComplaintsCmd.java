/**
 * 投诉列表查询命令类
 * 
 * 该类用于处理投诉信息的列表查询请求，包括查询投诉数据、关联照片信息以及处理人员信息等功能
 * 通过注解@Java110Cmd(serviceCode = "complaint.listComplaints")标识为处理投诉列表查询的命令
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.complaint;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
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

@Java110Cmd(serviceCode = "complaint.listComplaints")
public class ListComplaintsCmd extends Cmd {

    /**
     * 投诉信息服务接口
     */
    @Autowired
    private IComplaintV1InnerServiceSMO complaintV1InnerServiceSMOImpl;

    /**
     * 房间信息服务接口
     */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 投诉用户信息服务接口
     */
    @Autowired
    private IComplaintUserInnerServiceSMO complaintUserInnerServiceSMOImpl;

    /**
     * 文件关联信息服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 业主信息服务接口
     */
    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    /**
     * 投诉类型用户关联信息服务接口
     */
    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区信息，并进行分页参数验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令的主方法
     * 
     * 处理投诉列表查询请求，包括查询投诉数据、刷新照片信息、查询处理人员信息，
     * 并构建响应结果返回给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为投诉数据传输对象
        ComplaintDto complaintDto = BeanConvertUtil.covertBean(reqJson, ComplaintDto.class);
        
        // 查询投诉记录总数
        int count = complaintV1InnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        List<ComplaintDto> complaintDtos = null;
        
        // 如果有投诉记录，则查询详细数据并刷新照片信息
        if (count > 0) {
            complaintDtos = complaintV1InnerServiceSMOImpl.queryComplaints(complaintDto);
            refreshPhotos(complaintDtos);
        } else {
            // 如果没有记录，返回空列表
            complaintDtos = new ArrayList<>();
        }

        // 查询投诉类型对应的处理人员信息
        toQueryStaff(complaintDtos);

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, complaintDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询投诉处理人员信息
     * 
     * 根据投诉类型查询对应的处理人员信息，并将人员信息设置到投诉对象中
     * 注意：只对未完成的投诉记录进行处理人员查询
     * 
     * @param complaintDtos 投诉数据列表
     */
    private void toQueryStaff(List<ComplaintDto> complaintDtos) {
        // 如果投诉列表为空，直接返回
        if (ListUtil.isNull(complaintDtos)) {
            return;
        }

        // 收集所有投诉类型编码
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
        // 遍历投诉记录，为每条记录设置对应的处理人员
        for (ComplaintDto complaintDto : complaintDtos) {
            staffs = new ArrayList<>();
            // 如果投诉已完结，跳过处理人员查询
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
     * 刷新投诉照片信息
     * 
     * 根据投诉ID查询关联的照片信息，并将照片URL设置到投诉对象中
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
            // 遍历文件关联信息，构建照片VO列表
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                photoVo.setUrl(tmpFileRelDto.getFileRealName()); // 设置照片URL
                photoVos.add(photoVo);
            }
            // 设置照片列表到投诉对象
            complaintDataVo.setPhotos(photoVos);
        }
    }
}