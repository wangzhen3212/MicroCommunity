/**
 * 业主报修服务命令类 - 查询维修人员列表
 * 
 * 该类负责处理业主报修相关的维修人员查询请求，包括：
 * 1. 查询维修人员信息列表
 * 2. 计算维修任务耗时
 * 3. 加载维修相关的图片信息
 * 4. 返回分页查询结果
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

@Java110Cmd(serviceCode = "ownerRepair.listRepairStaffs")
public class ListRepairStaffsCmd extends Cmd {

    /** 维修服务接口 */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /** 维修人员服务接口 */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和属性验证
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(context);
    }

    /**
     * 执行查询维修人员命令
     * 
     * 主要功能：
     * 1. 查询维修人员总数和列表
     * 2. 刷新维修人员信息（包括耗时计算和图片加载）
     * 3. 返回分页响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     * @throws ParseException 当数据解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为RepairUserDto对象
        RepairUserDto repairUserDto = BeanConvertUtil.covertBean(reqJson, RepairUserDto.class);

        // 查询维修人员总数
        int count = repairUserInnerServiceSMOImpl.queryRepairUsersCount(repairUserDto);

        List<RepairUserDto> repairUserDtos = null;
        if (count > 0) {
            // 查询维修人员列表
            repairUserDtos = repairUserInnerServiceSMOImpl.queryRepairUsers(repairUserDto);
            // 刷新维修人员信息（计算耗时和加载图片）
            refreshRepairUser(repairUserDtos);
        } else {
            // 如果没有数据，返回空列表
            repairUserDtos = new ArrayList<>();
        }

        // 计算总页数并创建响应实体
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
            count, // 总记录数
            repairUserDtos // 数据列表
        );

        // 设置响应实体
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新维修人员信息
     * 
     * 主要功能：
     * 1. 计算每个维修任务的耗时
     * 2. 加载维修任务相关的图片信息
     * 
     * @param repairUserDtos 维修人员数据列表
     */
    private void refreshRepairUser(List<RepairUserDto> repairUserDtos) {
        long duration = 0;
        // 遍历所有维修人员记录，计算耗时
        for (RepairUserDto repairUserDto : repairUserDtos) {
            if (repairUserDto.getEndTime() == null) {
                // 如果结束时间为空，计算从开始时间到当前时间的耗时
                duration = DateUtil.getCurrentDate().getTime() - repairUserDto.getStartTime().getTime();
            } else {
                // 如果结束时间不为空，计算从开始时间到结束时间的耗时
                duration = repairUserDto.getEndTime().getTime() - repairUserDto.getStartTime().getTime();
            }
            // 设置格式化后的耗时
            repairUserDto.setDuration(getCostTime(duration));
        }

        // 刷入图片信息
        List<PhotoVo> photoVos = null;  // 业主上传维修图片
        PhotoVo photoVo = null;

        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");

        // 遍历所有维修人员记录，加载图片信息
        for (RepairUserDto repairUserDto : repairUserDtos) {
            FileRelDto fileRelDto = new FileRelDto();
            // 设置维修ID作为查询条件
            fileRelDto.setObjId(repairUserDto.getRepairId());
            // 查询文件关联信息
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            photoVos = new ArrayList<>();
            // 遍历文件关联记录，筛选维修图片
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                if (tmpFileRelDto.getRelTypeCd().equals("14000")) {  // 维修图片类型代码
                    photoVo = new PhotoVo();
                    // 处理图片URL，如果不是完整URL则拼接域名
                    if(!tmpFileRelDto.getFileRealName().startsWith("http")){
                        photoVo.setUrl(imgUrl + tmpFileRelDto.getFileRealName());
                    } else {
                        photoVo.setUrl(tmpFileRelDto.getFileRealName());
                    }
                    // 设置关联类型代码
                    photoVo.setRelTypeCd(tmpFileRelDto.getRelTypeCd());
                    photoVos.add(photoVo);
                } else {
                    // 跳过非维修图片类型的文件
                    continue;
                }
            }
            // 设置维修人员对应的图片列表
            repairUserDto.setPhotoVos(photoVos);
        }
    }

    /**
     * 计算耗时并格式化为字符串
     * 
     * 将毫秒数转换为"小时:分钟"格式的字符串
     * 
     * @param time 耗时（毫秒）
     * @return 格式化后的耗时字符串，格式为"小时:分钟"
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