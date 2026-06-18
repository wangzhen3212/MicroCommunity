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
 * 巡检路线管理命令类
 * 
 * 该类负责处理管理员巡检路线列表查询请求，包括参数验证、数据查询和结果返回等功能。
 * 通过注解@Java110Cmd指定服务代码，用于路由请求到对应的命令处理器。
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionRoute.listAdminInspectionRoutes")
public class ListAdminInspectionRoutesCmd extends Cmd {

    /**
     * 巡检路线内部服务接口，用于调用巡检路线相关的业务逻辑
     */
    @Autowired
    private IInspectionRouteInnerServiceSMO inspectionRouteInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数验证和管理员权限验证。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数，确保请求中包含必要的分页信息
        super.validatePageInfo(reqJson);
        // 验证管理员权限，确保当前用户具有管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 
     * 处理巡检路线列表查询请求，包括数据查询、结果封装和响应返回。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检路线数据传输对象
        InspectionRouteDto inspectionRouteDto = BeanConvertUtil.covertBean(reqJson, InspectionRouteDto.class);

        // 查询巡检路线总记录数
        int count = inspectionRouteInnerServiceSMOImpl.queryInspectionRoutesCount(inspectionRouteDto);

        // 初始化巡检路线数据列表
        List<ApiInspectionRouteDataVo> inspectionRoutes = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果有数据，查询巡检路线列表并转换为前端展示对象
            inspectionRoutes = BeanConvertUtil.covertBeanList(
                inspectionRouteInnerServiceSMOImpl.queryInspectionRoutes(inspectionRouteDto), 
                ApiInspectionRouteDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            inspectionRoutes = new ArrayList<>();
        }

        // 创建巡检路线响应对象
        ApiInspectionRouteVo apiInspectionRouteVo = new ApiInspectionRouteVo();

        // 设置总记录数
        apiInspectionRouteVo.setTotal(count);
        // 计算总页数：总记录数 / 每页显示行数，向上取整
        apiInspectionRouteVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置巡检路线数据列表
        apiInspectionRouteVo.setInspectionRoutes(inspectionRoutes);

        // 创建HTTP响应实体，将响应对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionRouteVo), 
            HttpStatus.OK
        );

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}