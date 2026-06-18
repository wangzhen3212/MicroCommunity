package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.TempCarFeeConfigDto;
import com.java110.intf.fee.ITempCarFeeConfigInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 临时车费用配置查询命令类
 * 
 * 该类用于处理查询临时车费用配置列表的请求，支持分页查询功能
 * 通过调用临时车费用配置服务获取数据并返回给前端
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode fee.listTempCarFeeConfigs
 */
@Java110Cmd(serviceCode = "fee.listTempCarFeeConfigs")
public class ListTempCarFeeConfigsCmd extends Cmd {

    /**
     * 临时车费用配置服务接口
     * 用于查询临时车费用配置信息
     */
    @Autowired
    private ITempCarFeeConfigInnerServiceSMO tempCarFeeConfigInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询临时车费用配置列表的核心业务逻辑
     * 包括参数转换、数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为临时车费用配置数据传输对象
        TempCarFeeConfigDto tempCarFeeConfigDto = BeanConvertUtil.covertBean(reqJson, TempCarFeeConfigDto.class);

        // 查询符合条件的临时车费用配置总数
        int count = tempCarFeeConfigInnerServiceSMOImpl.queryTempCarFeeConfigsCount(tempCarFeeConfigDto);

        // 定义临时车费用配置列表
        List<TempCarFeeConfigDto> tempCarFeeConfigDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在数据，则查询具体的临时车费用配置列表
            tempCarFeeConfigDtos = tempCarFeeConfigInnerServiceSMOImpl.queryTempCarFeeConfigs(tempCarFeeConfigDto);
        } else {
            // 如果没有数据，返回空列表
            tempCarFeeConfigDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        
        // 封装返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, tempCarFeeConfigDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}