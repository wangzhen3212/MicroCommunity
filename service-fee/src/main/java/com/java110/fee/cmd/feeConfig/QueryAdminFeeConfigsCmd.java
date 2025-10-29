/**
 * 费用配置查询命令类
 * 
 * 功能：管理员查询费用配置信息的命令处理类，支持分页查询和权限验证
 * 用途：处理feeConfig.queryAdminFeeConfigs服务码的请求，返回管理员可见的费用配置列表
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.feeConfig;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.PageDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.feeConfig.ApiFeeConfigDataVo;
import com.java110.vo.api.feeConfig.ApiFeeConfigVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "feeConfig.queryAdminFeeConfigs")
public class QueryAdminFeeConfigsCmd extends Cmd {

    /**
     * 费用配置内部服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 员工社区关联服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 功能：验证请求参数的有效性，包括分页参数和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行费用配置查询业务逻辑，包括权限过滤、数据查询和结果封装
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为FeeConfigDto对象
        FeeConfigDto feeConfigDto = BeanConvertUtil.covertBean(reqJson, FeeConfigDto.class);

        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询当前用户管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有关联的社区，设置查询条件中的社区ID列表
        if (!ListUtil.isNull(communityIds)) {
            feeConfigDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 处理特殊查询标志：当isFlag为0时，不分页查询所有数据
        if (!StringUtil.isEmpty(reqJson.getString("isFlag")) && reqJson.getString("isFlag").equals("0")) {
            feeConfigDto.setPage(PageDto.DEFAULT_PAGE);
        }
        
        // 查询费用配置总数
        int count = feeConfigInnerServiceSMOImpl.queryFeeConfigsCount(feeConfigDto);
        List<ApiFeeConfigDataVo> feeConfigs = null;
        
        // 如果查询到数据，进行数据处理
        if (count > 0) {
            // 查询费用配置列表并转换为VO对象
            feeConfigs = BeanConvertUtil.covertBeanList(feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto), ApiFeeConfigDataVo.class);
            
            // 处理金额字段，去除小数点后无效的0
            for (ApiFeeConfigDataVo feeConfig : feeConfigs) {
                // 处理附加金额字段
                if (!StringUtil.isEmpty(feeConfig.getAdditionalAmount())) {
                    feeConfig.setAdditionalAmount(Double.parseDouble(feeConfig.getAdditionalAmount()) + "");
                }
                // 处理平方价格字段
                if (!StringUtil.isEmpty(feeConfig.getSquarePrice())) {
                    feeConfig.setSquarePrice(Double.parseDouble(feeConfig.getSquarePrice()) + "");
                }
            }
        } else {
            // 如果没有查询到数据，返回空列表
            feeConfigs = new ArrayList<>();
        }
        
        // 构建返回VO对象
        ApiFeeConfigVo apiFeeConfigVo = new ApiFeeConfigVo();
        apiFeeConfigVo.setTotal(count); // 设置总记录数
        apiFeeConfigVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiFeeConfigVo.setFeeConfigs(feeConfigs); // 设置费用配置列表
        
        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeConfigVo), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}