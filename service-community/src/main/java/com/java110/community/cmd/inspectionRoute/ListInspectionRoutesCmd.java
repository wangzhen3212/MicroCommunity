package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionRouteDto;
import com.java110.intf.community.IInspectionRouteInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.inspectionRoute.ApiInspectionRouteDataVo;
import com.java110.vo.api.inspectionRoute.ApiInspectionRouteVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检路线查询命令类
 * 
 * 该类用于处理巡检路线列表查询请求，包括参数验证、数据查询和结果返回等功能
 * 通过注解@Java110Cmd指定服务代码为"inspectionRoute.listInspectionRoutes"
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionRoute.listInspectionRoutes")
public class ListInspectionRoutesCmd extends Cmd {

    /**
     * 巡检路线内部服务接口
     */
    @Autowired
    private IInspectionRouteInnerServiceSMO inspectionRouteInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要字段的检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(context);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 处理巡检路线查询业务逻辑，包括数据查询、结果封装和响应返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检路线数据传输对象
        InspectionRouteDto inspectionRouteDto = BeanConvertUtil.covertBean(reqJson, InspectionRouteDto.class);

        // 查询巡检路线总数
        int count = inspectionRouteInnerServiceSMOImpl.queryInspectionRoutesCount(inspectionRouteDto);

        // 初始化巡检路线列表
        List<ApiInspectionRouteDataVo> inspectionRoutes = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果有数据，查询巡检路线列表并转换为视图对象
            inspectionRoutes = BeanConvertUtil.covertBeanList(
                inspectionRouteInnerServiceSMOImpl.queryInspectionRoutes(inspectionRouteDto), 
                ApiInspectionRouteDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            inspectionRoutes = new ArrayList<>();
        }

        // 创建巡检路线视图对象
        ApiInspectionRouteVo apiInspectionRouteVo = new ApiInspectionRouteVo();

        // 设置总记录数
        apiInspectionRouteVo.setTotal(count);
        // 计算总页数：总记录数除以每页行数，向上取整
        apiInspectionRouteVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置巡检路线数据列表
        apiInspectionRouteVo.setInspectionRoutes(inspectionRoutes);

        // 创建HTTP响应实体，将视图对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionRouteVo), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}