/**
 * 费用配置查询命令类
 * 
 * 该类用于处理费用配置列表查询请求，负责验证请求参数并查询费用配置信息
 * 通过调用费用配置内部服务获取数据，并将结果封装为API响应格式返回
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.feeConfig;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.PageDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.feeConfig.ApiFeeConfigDataVo;
import com.java110.vo.api.feeConfig.ApiFeeConfigVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "feeConfig.listFeeConfigs")
public class ListFeeConfigsCmd extends Cmd {

    /**
     * 费用配置内部服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(cmdDataFlowContext);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
    }

    /**
     * 执行费用配置查询命令
     * 
     * 根据请求参数查询费用配置列表，处理数据格式并返回分页结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为费用配置DTO对象
        FeeConfigDto feeConfigDto = BeanConvertUtil.covertBean(reqJson, FeeConfigDto.class);
        
        // 如果isFlag为0，则不分页，使用默认页码
        if (!StringUtil.isEmpty(reqJson.getString("isFlag")) && reqJson.getString("isFlag").equals("0")) {
            feeConfigDto.setPage(PageDto.DEFAULT_PAGE);
        }
        
        // 查询费用配置总数
        int count = feeConfigInnerServiceSMOImpl.queryFeeConfigsCount(feeConfigDto);
        List<ApiFeeConfigDataVo> feeConfigs = null;
        
        // 如果有数据，则查询详细列表并处理数据格式
        if (count > 0) {
            // 查询费用配置列表并转换为API数据VO对象
            feeConfigs = BeanConvertUtil.covertBeanList(feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto), ApiFeeConfigDataVo.class);
            
            // 处理金额字段的小数点后无效的0
            for (ApiFeeConfigDataVo feeConfig : feeConfigs) {
                // 处理附加金额字段，去除无效的0
                if (!StringUtil.isEmpty(feeConfig.getAdditionalAmount())) {
                    feeConfig.setAdditionalAmount(Double.parseDouble(feeConfig.getAdditionalAmount()) + "");
                }
                // 处理单价字段，去除无效的0
                if (!StringUtil.isEmpty(feeConfig.getSquarePrice())) {
                    feeConfig.setSquarePrice(Double.parseDouble(feeConfig.getSquarePrice()) + "");
                }
            }
        } else {
            // 如果没有数据，返回空列表
            feeConfigs = new ArrayList<>();
        }
        
        // 构建API响应VO对象
        ApiFeeConfigVo apiFeeConfigVo = new ApiFeeConfigVo();
        // 设置总记录数
        apiFeeConfigVo.setTotal(count);
        // 计算总页数
        apiFeeConfigVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置费用配置数据列表
        apiFeeConfigVo.setFeeConfigs(feeConfigs);
        
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeConfigVo), HttpStatus.OK);
        // 设置响应实体到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}