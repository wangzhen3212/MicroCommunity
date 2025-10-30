package com.java110.report.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询收费项目变更记录命令类
 * 
 * 该类负责处理查询收费项目历史配置变更记录的请求，包括参数验证和业务逻辑处理
 * 通过调用报表服务获取收费项目的变更历史数据，并返回分页查询结果
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode fee.queryHisFeeConfig
 */
@Java110Cmd(serviceCode = "fee.queryHisFeeConfig")
public class QueryHisFeeConfigCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于获取收费项目变更记录数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区ID信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 处理查询收费项目变更记录的核心业务逻辑，包括：
     * 1. 参数转换和分页处理
     * 2. 调用服务层获取数据总数和详细记录
     * 3. 构建分页响应结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页参数：每页显示行数
        int row = reqJson.getInteger("row");
        
        // 将请求JSON对象转换为FeeConfigDto对象，便于后续查询
        FeeConfigDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeConfigDto.class);

        // 调用服务层接口，查询符合条件的收费项目变更记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisFeeConfigCount(feeDto);
        
        // 定义收费项目配置列表
        List<FeeConfigDto> feeConfigDtos = null;
        
        // 根据查询结果总数判断是否有数据
        if (total > 0) {
            // 如果有数据，则查询详细的收费项目变更记录列表
            feeConfigDtos = reportCommunityInnerServiceSMOImpl.queryHisFeeConfigs(feeDto);
        } else {
            // 如果没有数据，返回空列表
            feeConfigDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        // 创建响应实体，包含总页数、总记录数和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, feeConfigDtos);
        
        // 将响应实体设置到上下文中，返回给调用方
        context.setResponseEntity(responseEntity);
    }
}