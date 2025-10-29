/**
 * 停车场查询命令类
 * 
 * 该命令类用于处理停车场列表查询请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 分页查询停车场信息
 * 3. 返回停车场列表数据
 * 4. 支持通过小区ID查询指定小区的所有停车场
 * 
 * 适用于用户业主手机端赠送停车券等场景
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022
 */
package com.java110.community.cmd.parkingArea;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.intf.community.IParkingAreaInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.parkingArea.ApiParkingAreaDataVo;
import com.java110.vo.api.parkingArea.ApiParkingAreaVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * API文档注解 - 停车场查询接口
 */
@Java110CmdDoc(title = "查询停车场",
        description = "用户业主手机端赠送停车劵时使用，也可以用于其他的场景，根据小区ID查询，当前小区的所有停车场",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/parkingArea.listParkingAreas",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "parkingArea.listParkingAreas",
        seq = 20
)

/**
 * 请求参数文档注解
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
        @Java110ParamDoc(name = "page",type = "int",length = 11, remark = "分页页数"),
        @Java110ParamDoc(name = "row",type = "int", length = 11, remark = "分页行数"),
})

/**
 * 响应参数文档注解
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "parkingAreas", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "parkingAreas",name = "paId", type = "String", remark = "停车场ID"),
                @Java110ParamDoc(parentNodeName = "parkingAreas",name = "num", type = "String", remark = "停车场编号"),
        }
)

/**
 * 请求响应示例文档注解
 */
@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/parkingArea.listParkingAreas?num=&typeCd=&paId=&page=1&row=10&communityId=2022112555490011",
        resBody="{\"page\":0,\"parkingAreas\":[{\"attrs\":[{\"attrId\":\"112022112796270047\",\"communityId\":\"2022112555490011\",\"listShow\":\"Y\",\"paId\":\"102022112706900045\",\"page\":-1,\"records\":0,\"row\":0,\"specCd\":\"6185-17861\",\"specName\":\"外部编码\",\"specType\":\"2233\",\"statusCd\":\"0\",\"total\":0,\"value\":\"123\"}],\"createTime\":\"2022-11-27 01:48:27\",\"num\":\"A\",\"paId\":\"102022112706900045\",\"remark\":\"\",\"typeCd\":\"1001\"}],\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 命令注解 - 指定服务编码
 */
@Java110Cmd(serviceCode = "parkingArea.listParkingAreas")
public class ListParkingAreasCmd extends Cmd {

    /**
     * 停车场内部服务接口
     */
    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 主要验证逻辑：
     * 1. 验证必填参数communityId是否存在
     * 2. 调用父类方法验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必填参数 - 小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 调用父类方法验证分页参数（page和row）
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 主要执行逻辑：
     * 1. 将请求参数转换为数据传输对象
     * 2. 查询停车场总数
     * 3. 根据分页参数查询停车场列表
     * 4. 组装响应数据并返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为ParkingAreaDto对象
        ParkingAreaDto parkingAreaDto = BeanConvertUtil.covertBean(reqJson, ParkingAreaDto.class);

        // 查询停车场总数，用于分页计算
        int count = parkingAreaInnerServiceSMOImpl.queryParkingAreasCount(parkingAreaDto);

        // 声明停车场数据相关变量
        List<ApiParkingAreaDataVo> parkingAreas = null;
        ApiParkingAreaDataVo parkingAreaDataVo = null;
        List<ParkingAreaDto> parkingAreaDtos = null;

        // 如果有数据，则进行数据查询和转换
        if (count > 0) {
            parkingAreas = new ArrayList<>();
            // 查询停车场列表数据
            parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);

            // 遍历查询结果，转换为视图对象
            for (ParkingAreaDto tmpParkingAreaDto : parkingAreaDtos) {
                // 将DTO转换为VO对象
                parkingAreaDataVo = BeanConvertUtil.covertBean(tmpParkingAreaDto, ApiParkingAreaDataVo.class);
                // 设置停车场属性信息
                parkingAreaDataVo.setAttrs(tmpParkingAreaDto.getAttrs());
                // 添加到结果列表
                parkingAreas.add(parkingAreaDataVo);
            }
        } else {
            // 如果没有数据，返回空列表
            parkingAreas = new ArrayList<>();
        }

        // 创建API响应对象
        ApiParkingAreaVo apiParkingAreaVo = new ApiParkingAreaVo();

        // 设置响应数据
        apiParkingAreaVo.setTotal(count); // 总记录数
        // 计算总页数：总记录数/每页行数，向上取整
        apiParkingAreaVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiParkingAreaVo.setParkingAreas(parkingAreas); // 停车场数据列表

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
                JSONObject.toJSONString(apiParkingAreaVo), // 将响应对象转换为JSON字符串
                HttpStatus.OK // 设置HTTP状态码为200
        );

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}