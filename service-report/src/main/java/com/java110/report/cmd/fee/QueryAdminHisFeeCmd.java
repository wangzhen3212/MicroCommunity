package com.java110.report.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
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
 * 查询费用变更记录命令类
 * 
 * 该类负责处理费用管理历史记录的查询请求，包括参数验证和数据查询功能
 * 通过调用报表服务获取费用变更的详细信息
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode fee.queryAdminHisFee
 */
@Java110Cmd(serviceCode = "fee.queryAdminHisFee")
public class QueryAdminHisFeeCmd extends Cmd {

    /**
     * 报表社区内部服务接口，用于查询费用相关数据
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数，确保分页信息完整有效
        super.validatePageInfo(reqJson);
        // 验证管理员权限，确保当前用户具有管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行费用历史记录查询命令
     * 
     * 根据请求参数查询费用变更历史记录，并返回分页结果
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象，包含查询条件和分页信息
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取每页显示行数
        int row = reqJson.getInteger("row");
        // 将请求JSON转换为FeeDto对象，用于查询条件
        FeeDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeDto.class);

        // 查询符合条件的费用历史记录总数
        int total = reportCommunityInnerServiceSMOImpl.queryHisFeeCount(feeDto);
        
        List<FeeDto> feeDtos = null;
        
        // 根据查询结果总数决定是否查询详细数据
        if (total > 0) {
            // 查询费用历史记录详细数据
            feeDtos = reportCommunityInnerServiceSMOImpl.queryHisFees(feeDto);
        } else {
            // 如果没有数据，返回空列表
            feeDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页行数，向上取整
        // 创建响应实体，包含分页信息和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, feeDtos);
        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}