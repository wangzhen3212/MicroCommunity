/**
 * 访客管理命令类 - 管理员访客列表查询
 * 
 * 该类负责处理管理员查询访客列表的请求，包括访客信息查询、业主信息关联、照片信息处理等功能
 * 通过调用多个内部服务实现完整的访客数据查询和展示
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.visit.VisitDto;
import com.java110.dto.visit.VisitSettingDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IVisitSettingV1InnerServiceSMO;
import com.java110.intf.community.IVisitV1InnerServiceSMO;
import com.java110.intf.user.IOwnerV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.visit.ApiVisitDataVo;
import com.java110.vo.api.visit.ApiVisitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "visit.listAdminVisits")
public class ListAdminVisitsCmd extends Cmd {

    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMO;

    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    //键 - 车辆免费时间配置键
    public static final String CAR_FREE_TIME = "CAR_FREE_TIME";

    //键 - 访客数量配置键
    public static final String VISIT_NUMBER = "VISIT_NUMBER";

    /**
     * 请求参数验证方法
     * 
     * 验证分页参数和管理员权限，确保请求的合法性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 验证分页参数
        super.validateAdmin(context); // 验证管理员权限
    }

    /**
     * 执行命令方法 - 查询管理员访客列表
     * 
     * 主要功能：
     * 1. 查询访客基本信息
     * 2. 关联业主信息
     * 3. 处理访客照片信息
     * 4. 构建返回结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 将请求JSON转换为访客DTO对象
        VisitDto visitDto = BeanConvertUtil.covertBean(reqJson, VisitDto.class);
        visitDto.setPhoneNumber(reqJson.getString("ownerTel")); // 设置电话号码查询条件

        // 查询访客总数
        int count = visitV1InnerServiceSMO.queryVisitsCount(visitDto);
        String visitorCode = ""; // 访客编码，当前为空

        List<ApiVisitDataVo> visits = new ArrayList<>();
        if (count > 0) {
            // 查询访客详情列表
            List<VisitDto> visitDtos = visitV1InnerServiceSMO.queryVisits(visitDto);
            for (VisitDto visit : visitDtos) {
                // 转换DTO为VO对象
                ApiVisitDataVo apiVisitDataVo = BeanConvertUtil.covertBean(visit, ApiVisitDataVo.class);
                apiVisitDataVo.setVisitorCode(visitorCode); // 设置访客编码
                // 如果存在文件保存名称，则设置URL
                if (!StringUtil.isEmpty(visit.getFileSaveName())) {
                    apiVisitDataVo.setUrl(visit.getFileSaveName());
                }
                visits.add(apiVisitDataVo);
            }
        } else {
            visits = new ArrayList<>(); // 无数据时返回空列表
        }

        // 刷新业主信息 - 关联访客对应的业主信息
        refreshOwners(visits, reqJson);
        // 刷新照片信息 - 处理访客照片URL
        refreshPhoto(visits, reqJson);
        
        // 构建返回VO对象
        ApiVisitVo apiVisitVo = new ApiVisitVo();
        apiVisitVo.setTotal(count); // 设置总记录数
        // 计算总页数
        apiVisitVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiVisitVo.setVisits(visits); // 设置访客数据列表
        
        // 构建响应实体
        responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiVisitVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity); // 设置响应
    }

    /**
     * 刷新业主信息方法
     * 
     * 根据访客列表中的业主ID，查询对应的业主信息并设置到访客VO中
     * 
     * @param visits 访客数据VO列表
     * @param reqJson 请求参数的JSON对象，包含小区ID等信息
     */
    private void refreshOwners(List<ApiVisitDataVo> visits, JSONObject reqJson) {
        // 如果访客列表为空，直接返回
        if (ListUtil.isNull(visits)) {
            return;
        }
        
        // 收集所有业主ID
        List<String> ownerIds = new ArrayList<>();
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            ownerIds.add(apiVisitDataVo.getOwnerId());
        }
        
        // 构建业主查询条件
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerIds(ownerIds.toArray(new String[ownerIds.size()])); // 设置业主ID数组
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER); // 设置业主类型为普通业主
        ownerDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        // 查询业主信息
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 遍历访客列表，关联业主信息
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            for (OwnerDto tmpOwnerDto : ownerDtos) {
                // 跳过业主ID为空的访客
                if (StringUtil.isEmpty(apiVisitDataVo.getOwnerId())) {
                    continue;
                }
                // 业主ID不匹配时跳过
                if (!apiVisitDataVo.getOwnerId().equals(tmpOwnerDto.getOwnerId())) {
                    continue;
                }
                // 设置业主姓名和电话
                apiVisitDataVo.setOwnerName(tmpOwnerDto.getName());
                apiVisitDataVo.setOwnerTel(tmpOwnerDto.getLink());
            }
        }
    }

    /**
     * 刷新照片信息方法
     * 
     * 根据访客ID查询对应的照片信息，并构建完整的照片URL地址
     * 
     * @param visits 访客数据VO列表
     * @param reqJson 请求参数的JSON对象
     */
    private void refreshPhoto(List<ApiVisitDataVo> visits, JSONObject reqJson) {
        // 收集所有访客ID
        List<String> vIds = new ArrayList<>();
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            vIds.add(apiVisitDataVo.getvId());
        }
        
        // 构建文件关系查询条件
        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjIds(vIds.toArray(new String[vIds.size()])); // 设置对象ID数组
        // 查询文件关系信息
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
        
        // 如果没有文件关系数据，直接返回
        if (fileRelDtos == null || fileRelDtos.size() < 1) {
            return;
        }
        
        // 获取图片域名配置
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 遍历访客列表，设置照片URL
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                // 访客ID不匹配时跳过
                if (!apiVisitDataVo.getvId().equals(tmpFileRelDto.getObjId())) {
                    continue;
                }
                // 如果文件保存名称已经是完整URL，直接使用；否则拼接域名
                if (tmpFileRelDto.getFileSaveName().startsWith("http")) {
                    apiVisitDataVo.setUrl(tmpFileRelDto.getFileSaveName());
                } else {
                    apiVisitDataVo.setUrl(imgUrl + tmpFileRelDto.getFileSaveName());
                }
            }
        }
    }

}