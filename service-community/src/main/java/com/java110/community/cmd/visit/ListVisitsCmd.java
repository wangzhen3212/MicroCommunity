/**
 * 访客查询命令类
 * 
 * 该类负责处理访客相关的查询请求，包括：
 * 1. 查询访客列表信息
 * 2. 获取访客登记相关配置参数
 * 3. 处理PC端和移动端的访客查询需求
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
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

@Java110CmdDoc(title = "查询访客",
        description = "供pc端查询访客人员",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/visit.listVisits",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "visit.listVisits",
        seq = 22
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "分页信息"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "visits", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "visits", name = "vId", type = "String", remark = "访客ID"),
                @Java110ParamDoc(parentNodeName = "visits", name = "vName", type = "String", remark = "访客名称"),
                @Java110ParamDoc(parentNodeName = "visits", name = "visitGender", type = "String", remark = "访客性别"),
                @Java110ParamDoc(parentNodeName = "visits", name = "phoneNumber", type = "String", remark = "手机号"),
                @Java110ParamDoc(parentNodeName = "visits", name = "visitTime", type = "String", remark = "访问时间"),})

@Java110ExampleDoc(
        reqBody = "ttp://localhost:3000/app/visit.listVisits?page=1&row=10&communityId=2022121921870161",
        resBody = "{\"page\":0,\"records\":0,\"rows\":0,\"total\":0,\"visits\":[]}"
)
@Java110Cmd(serviceCode = "visit.listVisits")
public class ListVisitsCmd extends Cmd {

    @Autowired
    private IVisitV1InnerServiceSMO visitV1InnerServiceSMO;

    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    @Autowired
    private IOwnerV1InnerServiceSMO ownerV1InnerServiceSMOImpl;

    // 车辆免费时长配置键
    public static final String CAR_FREE_TIME = "CAR_FREE_TIME";

    // 访客数量配置键
    public static final String VISIT_NUMBER = "VISIT_NUMBER";

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括：
     * 1. 分页参数验证
     * 2. 小区ID必填验证
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行访客查询命令
     * 
     * 根据请求类型执行不同的业务逻辑：
     * 1. 如果是初始化访客登记参数请求，返回相关配置信息
     * 2. 如果是查询访客列表请求，返回分页的访客数据
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);
        
        // 查询小区访客设置信息
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);
        
        // 判断是否为初始化访客登记参数请求
        if (reqJson.containsKey("addVisitType") && !StringUtil.isEmpty(reqJson.getString("addVisitType"))
                && "initAddVisitParameter".equals(reqJson.getString("addVisitType"))) {
            // 初始化访客登记参数逻辑
            String freeTime = ""; // 车辆免费时长
            String numStr = "";   // 访客数量限制
            
            // 从访客设置中获取配置参数
            if (visitSettingDtos != null && visitSettingDtos.size() == 1) {
                freeTime = visitSettingDtos.get(0).getCarFreeTime();
                numStr = visitSettingDtos.get(0).getVisitNumber();
            }
            
            int number = 999; // 默认访客数量限制
            if (StringUtil.isInteger(numStr)) {
                number = Integer.parseInt(numStr);
            }
            
            // 构建初始化参数返回对象
            Map initAddVisitParameter = new HashMap();
            initAddVisitParameter.put("freeTime", freeTime);
            initAddVisitParameter.put("freeTimes", number);
            
            // 返回初始化参数
            responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(initAddVisitParameter), HttpStatus.OK);
        } else {
            // 查询访客列表逻辑
            VisitDto visitDto = BeanConvertUtil.covertBean(reqJson, VisitDto.class);
            
            // PC端查询时清空创建用户ID
            if ("PC".equals(reqJson.getString("channel"))) {
                visitDto.setCreateUserId("");
            }
            
            // 查询访客总数
            int count = visitV1InnerServiceSMO.queryVisitsCount(visitDto);
            
            // 获取访客码设置
            String visitorCode = "";
            if (visitSettingDtos != null && visitSettingDtos.size() == 1) {
                visitorCode = visitSettingDtos.get(0).getVisitorCode();
            }
            
            List<ApiVisitDataVo> visits = new ArrayList<>();
            if (count > 0) {
                // 查询访客详情列表
                List<VisitDto> visitDtos = visitV1InnerServiceSMO.queryVisits(visitDto);
                for (VisitDto visit : visitDtos) {
                    // 转换数据对象
                    ApiVisitDataVo apiVisitDataVo = BeanConvertUtil.covertBean(visit, ApiVisitDataVo.class);
                    apiVisitDataVo.setVisitorCode(visitorCode);
                    
                    // 设置文件访问URL
                    if (!StringUtil.isEmpty(visit.getFileSaveName())) {
                        apiVisitDataVo.setUrl(visit.getFileSaveName());
                    }
                    visits.add(apiVisitDataVo);
                }
            } else {
                visits = new ArrayList<>();
            }
            
            // 如果没有查询到数据，直接返回
            if (visits == null || visits.size() < 1) {
                return;
            }
            
            // 补充访客相关信息
            refreshOwners(visits, reqJson);   // 刷新业主信息
            refreshSetting(visits, reqJson);  // 刷新流程设置信息
            refreshPhoto(visits, reqJson);    // 刷新照片信息
            
            // 构建返回结果
            ApiVisitVo apiVisitVo = new ApiVisitVo();
            apiVisitVo.setTotal(count);
            apiVisitVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
            apiVisitVo.setVisits(visits);
            
            responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiVisitVo), HttpStatus.OK);
        }
        
        // 设置响应结果
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新业主信息
     * 
     * 根据访客关联的业主ID，查询并设置业主姓名和联系电话
     *
     * @param visits 访客数据列表
     * @param reqJson 请求JSON对象
     */
    private void refreshOwners(List<ApiVisitDataVo> visits, JSONObject reqJson) {
        if (visits == null || visits.size() < 1) {
            return;
        }
        
        // 收集所有业主ID
        List<String> ownerIds = new ArrayList<>();
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            ownerIds.add(apiVisitDataVo.getOwnerId());
        }
        
        // 查询业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setOwnerIds(ownerIds.toArray(new String[ownerIds.size()]));
        ownerDto.setOwnerTypeCd(OwnerDto.OWNER_TYPE_CD_OWNER);
        ownerDto.setCommunityId(reqJson.getString("communityId"));
        List<OwnerDto> ownerDtos = ownerV1InnerServiceSMOImpl.queryOwners(ownerDto);

        // 将业主信息设置到访客数据中
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            for (OwnerDto tmpOwnerDto : ownerDtos) {
                // 跳过空的业主ID
                if (StringUtil.isEmpty(apiVisitDataVo.getOwnerId())) {
                    continue;
                }
                // 匹配业主ID
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
     * 刷新访客照片信息
     * 
     * 根据访客ID查询关联的照片文件，并设置照片访问URL
     *
     * @param visits 访客数据列表
     * @param reqJson 请求JSON对象
     */
    private void refreshPhoto(List<ApiVisitDataVo> visits, JSONObject reqJson) {
        // 收集所有访客ID
        List<String> vIds = new ArrayList<>();
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            vIds.add(apiVisitDataVo.getvId());
        }
        
        // 查询文件关联信息
        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjIds(vIds.toArray(new String[vIds.size()]));
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
        
        if (fileRelDtos == null || fileRelDtos.size() < 1) {
            return;
        }
        
        // 获取图片访问域名
        String imgUrl = MappingCache.getValue(MappingConstant.FILE_DOMAIN, "IMG_PATH");
        
        // 设置照片URL
        for (ApiVisitDataVo apiVisitDataVo : visits) {
            for (FileRelDto tmpFileRelDto : fileRelDtos) {
                // 匹配访客ID
                if (!apiVisitDataVo.getvId().equals(tmpFileRelDto.getObjId())) {
                    continue;
                }
                
                // 设置照片URL，处理相对路径和绝对路径
                if (tmpFileRelDto.getFileSaveName().startsWith("http")) {
                    apiVisitDataVo.setUrl(tmpFileRelDto.getFileSaveName());
                } else {
                    apiVisitDataVo.setUrl(imgUrl + tmpFileRelDto.getFileSaveName());
                }
            }
        }
    }

    /**
     * 刷新访客流程设置信息
     * 
     * 根据小区ID查询访客流程设置，并设置到访客数据中
     *
     * @param visits 访客数据列表
     * @param reqJson 请求JSON对象
     */
    private void refreshSetting(List<ApiVisitDataVo> visits, JSONObject reqJson) {
        // 查询小区访客设置
        VisitSettingDto visitSettingDto = new VisitSettingDto();
        visitSettingDto.setCommunityId(reqJson.getString("communityId"));
        List<VisitSettingDto> visitSettingDtos = visitSettingV1InnerServiceSMOImpl.queryVisitSettings(visitSettingDto);
        
        if (visitSettingDtos == null || visitSettingDtos.size() < 1) {
            return;
        }
        
        if (visits == null || visits.size() < 1) {
            return;
        }
        
        // 为每个访客设置流程ID
        for (ApiVisitDataVo visitDataVo : visits) {
            visitDataVo.setFlowId(visitSettingDtos.get(0).getFlowId());
        }
    }
}