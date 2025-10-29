/**
 * 业主报修管理命令类
 * 
 * 功能：处理管理员查看维修人员列表的请求，包括查询维修人员信息、计算维修时长、获取维修图片等
 * 服务码：ownerRepair.listAdminRepairStaffs
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
import com.java110.dto.repair.RepairUserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.junkRequirement.PhotoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.listAdminRepairStaffs")
public class ListAdminRepairStaffsCmd extends Cmd {

    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 功能：验证分页参数和管理员权限
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
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
     * 命令执行方法
     * 
     * 功能：查询维修人员列表，包括维修时长计算和图片信息处理
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为RepairUserDto对象
        RepairUserDto repairUserDto = BeanConvertUtil.covertBean(reqJson, RepairUserDto.class);

        // 查询维修人员总数
        int count = repairUserInnerServiceSMOImpl.queryRepairUsersCount(repairUserDto);

        List<RepairUserDto> repairUserDtos = null;
        if (count > 0) {
            // 查询维修人员列表
            repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
            // 刷新维修人员信息（计算时长、获取图片）
            refreshRepairUser(repairUserDtos);
        } else {
            // 无数据时返回空列表
            repairUserDtos = new ArrayList<>();
        }

        // 创建响应实体，包含分页信息和数据
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, repairUserDtos);

        // 设置响应实体
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新维修人员信息
     * 
     * 功能：计算维修时长并获取维修相关图片信息
     * 
     * @param repairUserDtos 维修人员数据列表
     */
    private void refreshRepairUser(List<RepairUserDto> repairUserDtos) {
        long duration = 0;
        // 遍历所有维修人员记录，计算维修时长
        for (RepairUserDto repairUserDto : repairUserDtos) {
            if (repairUserDto.getEndTime() == null) {
                // 如果维修未结束，计算从开始时间到当前时间的时长
                duration = DateUtil.getCurrentDate().getTime() - repairUserDto.getStartTime().getTime();
            } else {
                // 如果维修已结束，计算从开始时间到结束时间的时长
                duration = repairUserDto.getEndTime().getTime() - repairUserDto.getStartTime().getTime();
            }
            // 设置格式化后的时长
            repairUserDto.setDuration(getCostTime(duration));
        }

        // 刷入图片信息
        List<PhotoVo> photoVos = null;  // 业主上传维修图片
        PhotoVo photoVo = null;

        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN,"IMG_PATH");

        // 遍历维修人员记录，获取相关图片
        for (RepairUserDto repairUserDto : repairUserDtos) {
            FileRelDto fileRelDto = new FileRelDto();
            // 设置维修ID作为查询条件
            fileRelDto.setObjId(repairUserDto.getRepairId());
            // 查询文件关联关系
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            photoVos = new ArrayList<>();
            // 遍历文件关联关系，筛选维修图片
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                if (tmpFileRelDto.getRelTypeCd().equals("14000")) {  // 维修图片关联类型码
                    photoVo = new PhotoVo();
                    // 处理图片URL，如果不是完整URL则拼接域名
                    if(!tmpFileRelDto.getFileRealName().startsWith("http")){
                        photoVo.setUrl(imgUrl+tmpFileRelDto.getFileRealName());
                    }else{
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    photoVos.add(photoVo);
                } else {
                    continue;
                }
            }
            // 设置维修图片列表
            repairUserDto.setPhotoVos(photoVos);
        }
    }

    /**
     * 计算耗时方法
     * 
     * 功能：将毫秒时间转换为"时:分"格式
     * 
     * @param time 时间（毫秒）
     * @return 格式化后的时间字符串，格式为"时:分"
     */
    public String getCostTime(Long time) {
        if (time == null) {
            return "00:00";
        }
        // 计算小时数
        long hours = time / (1000 * 60 * 60);
        // 计算分钟数
        long minutes = (time - hours * (1000 * 60 * 60)) / (1000 * 60);
        String diffTime = "";
        // 格式化分钟数，确保两位数显示
        if (minutes < 10) {
            diffTime = hours + ":0" + minutes;
        } else {
            diffTime = hours + ":" + minutes;
        }
        return diffTime;
    }
}