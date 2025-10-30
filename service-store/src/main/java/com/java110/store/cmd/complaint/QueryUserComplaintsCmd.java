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
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询用户投诉命令类
 * 该命令类负责处理用户投诉查询请求，包括查询用户发起的投诉列表、关联的图片信息以及处理人员信息
 * 服务编码：complaint.queryUserComplaints
 */
@Java110Cmd(serviceCode = "complaint.queryUserComplaints")
public class QueryUserComplaintsCmd extends Cmd {

    @Autowired
    private IComplaintV1InnerServiceSMO complaintV1InnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IComplaintUserInnerServiceSMO complaintUserInnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理用户投诉查询请求，返回投诉列表及相关信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException   命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从上下文中获取当前用户ID
        String userId = CmdContextUtils.getUserId(context);
        
        // 将请求JSON转换为ComplaintDto对象
        ComplaintDto complaintDto = BeanConvertUtil.covertBean(reqJson, ComplaintDto.class);
        // 设置查询条件：只查询当前用户发起的投诉
        complaintDto.setStartUserId(userId);
        
        // 查询投诉总数
        int count = complaintV1InnerServiceSMOImpl.queryComplaintsCount(complaintDto);
        List<ComplaintDto> complaintDtos = null;
        
        if (count > 0) {
            // 查询投诉列表数据
            complaintDtos = complaintV1InnerServiceSMOImpl.queryComplaints(complaintDto);
            // 刷新投诉相关的图片信息
            refreshPhotos(complaintDtos);
        } else {
            // 如果没有数据，返回空列表
            complaintDtos = new ArrayList<>();
        }

        // 查询投诉类型对应的处理人员信息
        toQueryStaff(complaintDtos);

        // 构建返回结果，包括总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, complaintDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询投诉处理人员信息
     * 根据投诉类型查询对应的处理人员，并关联到投诉数据中
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

        // 构建查询条件对象
        ComplaintTypeUserDto complaintTypeUserDto = new ComplaintTypeUserDto();
        complaintTypeUserDto.setTypeCds(typeCds.toArray(new String[typeCds.size()]));

        // 查询投诉类型对应的处理人员
        List<ComplaintTypeUserDto> complaintTypeUserDtos = complaintTypeUserV1InnerServiceSMOImpl.queryComplaintTypeUsers(complaintTypeUserDto);

        // 如果没有查询到处理人员，直接返回
        if (ListUtil.isNull(complaintTypeUserDtos)) {
            return;
        }
        
        List<ComplaintTypeUserDto> staffs = null;
        // 遍历投诉列表，为每个投诉关联对应的处理人员
        for (ComplaintDto complaintDto : complaintDtos) {
            staffs = new ArrayList<>();
            // 如果投诉已完成，则不查询处理人员
            if (ComplaintDto.STATE_FINISH.equals(complaintDto.getState())) {
                continue;
            }
            // 根据投诉类型匹配处理人员
            for (ComplaintTypeUserDto complaintTypeUserDto1 : complaintTypeUserDtos) {
                if (complaintDto.getTypeCd().equals(complaintTypeUserDto1.getTypeCd())) {
                    staffs.add(complaintTypeUserDto1);
                }
            }
            // 将处理人员列表设置到投诉对象中
            complaintDto.setStaffs(staffs);
        }
    }

    /**
     * 刷新投诉图片信息
     * 查询每个投诉关联的图片信息并设置到投诉对象中
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
            // 遍历文件关联信息，构建图片VO列表
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                photoVo = new PhotoVo();
                photoVo.setUrl(tmpFileRelDto.getFileRealName()); // 设置图片URL
                photoVos.add(photoVo);
            }
            // 将图片列表设置到投诉对象中
            complaintDataVo.setPhotos(photoVos);
        }
    }
}