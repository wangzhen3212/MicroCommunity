package com.java110.fee.cmd.oweFeeCallable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.oweFeeCallable.OweFeeCallableDto;
import com.java110.dto.reportFee.ReportOweFeeDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.fee.IOweFeeCallableV1InnerServiceSMO;
import com.java110.intf.report.IReportOweFeeInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.oweFeeCallable.OweFeeCallablePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 登记催缴记录命令类
 * 该命令用于处理欠费催缴记录的登记操作，包括参数验证和催缴记录的新增
 * 通过接收前端传入的欠费信息，生成对应的催缴记录并保存到数据库
 */
@Java110Cmd(serviceCode = "oweFeeCallable.writeOweFeeCallable")
public class WriteOweFeeCallableCmd extends Cmd {

    /**
     * 欠费催缴服务接口
     */
    @Autowired
    private IOweFeeCallableV1InnerServiceSMO oweFeeCallableV1InnerServiceSMOImpl;

    /**
     * 欠费报表服务接口
     */
    @Autowired
    private IReportOweFeeInnerServiceSMO reportOweFeeInnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当参数验证失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数：小区ID和房屋ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "roomId", "未包含房屋");

        // 获取费用ID数组并验证
        JSONArray feeIds = reqJson.getJSONArray("feeIds");
        if (feeIds == null || feeIds.size() < 1) {
            throw new CmdException("未包含费用");
        }
    }

    /**
     * 执行命令方法
     * 处理催缴记录的登记操作，为每个费用ID创建催缴记录
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当业务处理失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取员工ID
        String staffId = context.getReqHeaders().get("user-id");

        // 查询员工信息
        UserDto userDto = new UserDto();
        userDto.setUserId(staffId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证员工信息唯一性
        Assert.listOnlyOne(userDtos, "员工不在");

        // 获取费用ID数组
        JSONArray feeIds = reqJson.getJSONArray("feeIds");

        // 遍历每个费用ID，创建催缴记录
        for (int feeIndex = 0; feeIndex < feeIds.size(); feeIndex++) {
            doWriteFee(feeIds.getString(feeIndex), reqJson, userDtos.get(0));
        }
    }

    /**
     * 为单个费用创建催缴记录
     * 根据费用ID查询欠费信息，生成催缴记录并保存
     *
     * @param feeId   费用ID
     * @param reqJson 请求的JSON数据对象
     * @param userDto 操作用户信息
     */
    private void doWriteFee(String feeId, JSONObject reqJson, UserDto userDto) {
        // 构建欠费查询条件
        ReportOweFeeDto reportOweFeeDto = new ReportOweFeeDto();
        reportOweFeeDto.setFeeId(feeId);
        reportOweFeeDto.setCommunityId(reqJson.getString("communityId"));
        
        // 查询欠费信息
        List<ReportOweFeeDto> reportOweFeeDtos = reportOweFeeInnerServiceSMOImpl.queryReportAllOweFees(reportOweFeeDto);
        
        // 验证欠费信息唯一性
        Assert.listOnlyOne(reportOweFeeDtos, "欠费不存在");

        // 获取业主信息，处理空值情况
        String ownerId = reportOweFeeDtos.get(0).getOwnerId();
        String ownerName = reportOweFeeDtos.get(0).getOwnerName();

        // 如果业主ID为空，设置默认值
        if (StringUtil.isEmpty(ownerId)) {
            ownerId = "-1";
        }

        // 如果业主姓名为空，设置默认值
        if (StringUtil.isEmpty(ownerName)) {
            ownerName = "无业主";
        }

        // 创建催缴记录对象
        OweFeeCallablePo oweFeeCallablePo = new OweFeeCallablePo();

        // 设置催缴记录属性
        oweFeeCallablePo.setAmountdOwed(reportOweFeeDtos.get(0).getAmountOwed()); // 欠费金额
        oweFeeCallablePo.setCallableWay(OweFeeCallableDto.CALLABLE_WAY_PRINT); // 催缴方式：打印
        oweFeeCallablePo.setOfcId(GenerateCodeFactory.getGeneratorId("11")); // 生成催缴记录ID
        oweFeeCallablePo.setFeeId(reportOweFeeDtos.get(0).getFeeId()); // 费用ID
        oweFeeCallablePo.setFeeName(reportOweFeeDtos.get(0).getFeeName()); // 费用名称
        oweFeeCallablePo.setCommunityId(reqJson.getString("communityId")); // 小区ID
        oweFeeCallablePo.setConfigId(reportOweFeeDtos.get(0).getConfigId()); // 配置ID
        oweFeeCallablePo.setOwnerId(ownerId); // 业主ID
        oweFeeCallablePo.setOwnerName(ownerName); // 业主姓名
        oweFeeCallablePo.setPayerObjId(reportOweFeeDtos.get(0).getPayerObjId()); // 缴费对象ID
        oweFeeCallablePo.setPayerObjName(reportOweFeeDtos.get(0).getPayerObjName()); // 缴费对象名称
        oweFeeCallablePo.setPayerObjType(reportOweFeeDtos.get(0).getPayerObjType()); // 缴费对象类型
        oweFeeCallablePo.setRemark(reqJson.getString("remark")); // 备注信息
        oweFeeCallablePo.setStaffId(userDto.getUserId()); // 操作员工ID
        oweFeeCallablePo.setStaffName(userDto.getName()); // 操作员工姓名
        oweFeeCallablePo.setState(OweFeeCallableDto.STATE_COMPLETE); // 状态：已完成
        oweFeeCallablePo.setStartTime(reportOweFeeDtos.get(0).getEndTime()); // 开始时间
        oweFeeCallablePo.setEndTime(reportOweFeeDtos.get(0).getDeadlineTime()); // 结束时间

        // 保存催缴记录到数据库
        int flag = oweFeeCallableV1InnerServiceSMOImpl.saveOweFeeCallable(oweFeeCallablePo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("登记失败");
        }
    }
}