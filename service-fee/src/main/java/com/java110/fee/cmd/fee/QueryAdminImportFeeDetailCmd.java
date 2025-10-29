package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.importData.ImportFeeDetailDto;
import com.java110.intf.fee.IImportFeeDetailInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询管理员导入费用明细命令类
 * 
 * 该类用于处理管理员查询导入费用明细的请求，包括参数验证和数据查询功能
 * 通过服务码"fee.queryAdminImportFeeDetail"标识，继承自Cmd基类
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "fee.queryAdminImportFeeDetail")
public class QueryAdminImportFeeDetailCmd extends Cmd {

    /**
     * 导入费用明细内部服务接口
     * 用于调用费用明细相关的数据查询服务
     */
    @Autowired
    private IImportFeeDetailInnerServiceSMO importFeeDetailInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证管理员权限和分页信息，确保请求的合法性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询导入费用明细的业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为ImportFeeDetailDto对象
        ImportFeeDetailDto importFeeDetailDto = BeanConvertUtil.covertBean(reqJson, ImportFeeDetailDto.class);
        
        // 查询符合条件的费用明细总数
        int count = importFeeDetailInnerServiceSMOImpl.queryImportFeeDetailsCount(importFeeDetailDto);

        List<ImportFeeDetailDto> importFeeDetailDtos = null;
        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在数据，则查询具体的费用明细列表
            importFeeDetailDtos = importFeeDetailInnerServiceSMOImpl.queryImportFeeDetails(importFeeDetailDto);
        } else {
            // 如果没有数据，返回空列表
            importFeeDetailDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) importFeeDetailDto.getRow());
        // 封装返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, importFeeDetailDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        context.setResponseEntity(responseEntity);
    }
}