/**
 * 小区查询命令类
 * 
 * 该类负责处理小区列表查询请求，支持分页查询和条件筛选功能。
 * 通过注解配置了API文档信息，包括请求参数、响应格式和示例数据。
 * 主要功能包括：参数验证、小区数据查询、城市信息刷新和响应数据封装。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.area.AreaDto;
import com.java110.dto.community.CommunityDto;
import com.java110.intf.common.IAreaInnerServiceSMO;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.api.community.ApiCommunityDataVo;
import com.java110.vo.api.community.ApiCommunityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110CmdDoc(title = "查询小区",
        description = "查询系统中的所有小区信息",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/community.listCommunitys",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "community.listCommunitys",
        seq = 4
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "分页页数"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "分页行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "communitys", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "communitys", name = "communityId", type = "String", remark = "小区ID"),
                @Java110ParamDoc(parentNodeName = "communitys", name = "name", type = "String", remark = "小区名称"),
                @Java110ParamDoc(parentNodeName = "communitys", name = "state", type = "String", remark = "小区状态 1100 审核完成"),
                @Java110ParamDoc(parentNodeName = "communitys", name = "cityCode", type = "String", remark = "小区状态 城市编码"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/community.listCommunitys?name=&cityCode=&communityId=&page=1&row=10",
        resBody = "{\"communitys\":[{\"address\":\"青海省西宁市城西区国投广场\",\"cityCode\":\"630104\",\"cityName\":\"青海省西宁市城西区\",\"communityAttrDtos\":[{\"attrId\":\"112022081566950487\",\"communityId\":\"2022081539020475\",\"listShow\":\"Y\",\"page\":-1,\"records\":0,\"row\":0,\"specCd\":\"9329000004\",\"specName\":\"社区编码\",\"statusCd\":\"0\",\"total\":0,\"value\":\"123456\"}],\"communityId\":\"2022081539020475\",\"mapX\":\"101.33\",\"mapY\":\"101.33\",\"name\":\"测试小区\",\"nearbyLandmarks\":\"国投广场\",\"state\":\"1100\",\"stateName\":\"审核完成\",\"tel\":\"18909711443\"}],\"page\":0,\"records\":1,\"rows\":0,\"total\":2}"
)
@Java110Cmd(serviceCode = "community.listCommunitys")
public class ListCommunitysCmd extends Cmd {

    @Autowired
    private IAreaInnerServiceSMO areaInnerServiceSMOImpl; // 区域信息服务接口

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl; // 小区信息服务接口

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要进行分页参数验证
     * 
     * @param event 命令事件对象，包含请求上下文信息
     * @param context 数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 调用父类方法验证分页信息
    }

    /**
     * 命令执行方法
     * 
     * 处理小区查询请求，包括数据查询、结果封装和响应返回
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为CommunityDto对象
        CommunityDto communityDto = BeanConvertUtil.covertBean(reqJson, CommunityDto.class);

        // 查询小区总数
        int count = communityInnerServiceSMOImpl.queryCommunitysCount(communityDto);

        List<ApiCommunityDataVo> communitys = null; // 小区数据列表
        ApiCommunityDataVo apiCommunityDataVo = null; // 单个小区数据对象
        
        // 如果查询到数据，则处理数据列表
        if (count > 0) {
            communitys = new ArrayList<>();
            // 查询小区详细信息列表
            List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
            
            // 遍历小区列表，转换数据格式
            for (CommunityDto tmpCommunityDto : communityDtos) {
                // 将CommunityDto转换为ApiCommunityDataVo
                apiCommunityDataVo = BeanConvertUtil.covertBean(tmpCommunityDto, ApiCommunityDataVo.class);
                // 设置小区属性信息
                apiCommunityDataVo.setCommunityAttrDtos(tmpCommunityDto.getCommunityAttrDtos());
                communitys.add(apiCommunityDataVo);
            }
            // 刷新小区的城市名称信息
            refreshCommunityCity(communitys);
        } else {
            // 没有查询到数据时返回空列表
            communitys = new ArrayList<>();
        }
        
        // 构建响应对象
        ApiCommunityVo apiCommunityVo = new ApiCommunityVo();
        apiCommunityVo.setTotal(count); // 设置总记录数
        // 计算总页数：总记录数 / 每页行数，向上取整
        apiCommunityVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiCommunityVo.setCommunitys(communitys); // 设置小区数据列表
        
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCommunityVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity); // 设置响应到上下文
    }

    /**
     * 刷新小区城市名称信息
     * 
     * 根据城市编码查询对应的省市区名称，并设置到小区数据中
     * 
     * @param communitys 小区数据列表，需要刷新城市名称的小区数据
     */
    private void refreshCommunityCity(List<ApiCommunityDataVo> communitys) {
        List<String> areaCodes = new ArrayList<>(); // 城市编码列表
        
        // 收集所有小区的城市编码
        for (ApiCommunityDataVo communityDataVo : communitys) {
            areaCodes.add(communityDataVo.getCityCode());
        }
        
        // 如果城市编码列表为空，直接返回
        if (ListUtil.isNull(areaCodes)) {
            return;
        }
        
        // 构建区域查询条件
        AreaDto areaDto = new AreaDto();
        areaDto.setAreaCodes(areaCodes.toArray(new String[areaCodes.size()]));
        // 查询区域信息
        List<AreaDto> areaDtos = areaInnerServiceSMOImpl.getProvCityArea(areaDto);
        
        // 遍历小区列表，设置城市名称
        for (ApiCommunityDataVo communityDataVo : communitys) {
            boolean found = false; // 标记是否找到对应的区域信息
            // 遍历区域信息，匹配城市编码
            for (AreaDto tmpAreaDto : areaDtos) {
                if (communityDataVo.getCityCode().equals(tmpAreaDto.getAreaCode())) {
                    // 拼接省市区名称
                    communityDataVo.setCityName(tmpAreaDto.getProvName() + tmpAreaDto.getCityName() + tmpAreaDto.getAreaName());
                    found = true; // 标记已找到
                    break; // 找到后跳出内层循环
                }
            }
            // 如果没有找到对应的区域信息，设置默认值
            if (!found) {
                communityDataVo.setCityName("未知");
            }
        }
    }

}