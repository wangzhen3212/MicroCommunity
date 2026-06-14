package com.java110.report.cmd.car;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * 车位结构图查询命令类
 * 
 * 该类负责处理车位结构图查询请求，通过服务码"car.listCarStructure"标识，
 * 主要功能包括参数验证和车位结构数据查询
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "car.listCarStructure")
public class ListCarStructureCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询车位结构信息
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的单元信息和社区信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理数据流
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含单元ID
        Assert.hasKeyAndValue(reqJson, "unitId", "未传入单元信息");
        // 验证请求参数中必须包含社区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未传入房屋信息");
    }

    /**
     * 执行车位结构查询命令
     * 
     * 将请求参数转换为车位查询对象，调用服务层查询车位结构数据，
     * 并将查询结果设置到响应实体中返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为OwnerCarDto对象，用于查询条件
        OwnerCarDto carDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);
        
        // 调用报表服务查询车位结构信息
        List<OwnerCarDto> ownerCarDtos = reportCommunityInnerServiceSMOImpl.queryCarStructures(carDto);
        
        // 将查询结果封装为响应实体并设置到上下文中
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(ownerCarDtos));
    }
}