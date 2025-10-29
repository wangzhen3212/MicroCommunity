/**
 * 欠费查询命令类
 * 
 * 功能：处理查询欠费信息的请求，支持物业费和车位费的欠费查询
 * 根据费用类型区分处理逻辑，返回包含欠费明细和业主信息的完整数据
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.community.IFloorInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.utils.constant.FeeTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.api.ApiArrearsFeeDataVo;
import com.java110.vo.api.ApiArrearsFeeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "fee.queryArrearsFee")
public class QueryArrearsFeeCmd extends Cmd {

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    @Autowired
    private IFloorInnerServiceSMO floorInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中必须包含的字段：communityId、feeTypeCd、page、row
     * 验证page和row必须为数字格式
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必须包含的字段
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
        Assert.jsonObjectHaveKey(reqJson, "feeTypeCd", "请求中未包含feeTypeCd信息");
        Assert.jsonObjectHaveKey(reqJson, "page", "请求中未包含page信息");
        Assert.jsonObjectHaveKey(reqJson, "row", "请求中未包含row信息");

        // 验证数字格式
        Assert.isInteger(reqJson.getString("row"), "row必须为数字");
        Assert.isInteger(reqJson.getString("page"), "page必须为数字");
    }

    /**
     * 执行欠费查询命令
     * 
     * 主要逻辑：
     * 1. 构建查询条件，查询欠费数据
     * 2. 处理车位费的特殊情况（上下车位）
     * 3. 分页查询欠费记录
     * 4. 根据费用类型补充业主和房间/车位信息
     * 5. 返回完整的欠费响应数据
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为FeeDto对象
        FeeDto feeDtoParamIn = BeanConvertUtil.covertBean(reqJson, FeeDto.class);
        // 设置欠费查询的结束时间为当前时间
        feeDtoParamIn.setArrearsEndTime(DateUtil.getCurrentDate());

        // 特殊处理：当费用类型为租赁车位费时，需要查询上下车位两种费用类型
        if (FeeTypeConstant.FEE_TYPE_HIRE_PARKING_SPACE.equals(feeDtoParamIn.getFeeTypeCd())) {
            feeDtoParamIn.setFeeTypeCd(""); // 清空单一费用类型
            // 设置需要查询的多种费用类型
            feeDtoParamIn.setFeeTypeCds(new String[]{FeeTypeConstant.FEE_TYPE_HIRE_DOWN_PARKING_SPACE,
                    FeeTypeConstant.FEE_TYPE_HIRE_UP_PARKING_SPACE});
        }

        // 获取分页参数
        int page = reqJson.getInteger("page");
        int row = reqJson.getInteger("row");

        // 查询欠费记录总数
        int feeCount = feeInnerServiceSMOImpl.queryFeesCount(feeDtoParamIn);
        ApiArrearsFeeVo apiArrearsFeeVo = new ApiArrearsFeeVo();
        apiArrearsFeeVo.setTotal(feeCount);
        // 计算总页数
        apiArrearsFeeVo.setRecords((int) Math.ceil((double) feeCount / (double) row));
        ResponseEntity<String> responseEntity = null;
        
        // 如果没有欠费记录，直接返回空结果
        if (feeCount == 0) {
            responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiArrearsFeeVo), HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }

        // 查询欠费明细数据
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDtoParamIn);

        // 将FeeDto转换为前端展示用的Vo对象
        List<ApiArrearsFeeDataVo> apiFeeVo = BeanConvertUtil.covertBeanList(feeDtos, ApiArrearsFeeDataVo.class);

        // 获取所有缴费对象ID（房间ID或车位ID）
        String[] objIds = this.getObjIds(feeDtos);

        // 根据费用类型分别处理：物业费查询房间业主信息，车位费查询车位业主信息
        if (FeeTypeConstant.FEE_TYPE_PROPERTY.equals(feeDtoParamIn.getFeeTypeCd())) {
            // 物业费：查询房间对应的业主信息
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setRoomIds(objIds);
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByRoom(ownerDto);
            freshRoomAndOwnerData(apiFeeVo, ownerDtos);
        } else {
            // 车位费：查询车位对应的业主信息
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setRoomIds(objIds);
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByParkingSpace(ownerDto);
            freshParkingSpaceAndOwnerData(apiFeeVo, ownerDtos);
        }

        // 设置欠费明细数据
        apiArrearsFeeVo.setArrears(apiFeeVo);

        // 构建响应
        responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiArrearsFeeVo), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新房间和业主数据
     * 
     * 将业主信息（姓名、电话）和房间号补充到欠费数据中
     * 
     * @param apiFeeVos 欠费数据列表
     * @param ownerDtos 业主信息列表
     */
    private void freshRoomAndOwnerData(List<ApiArrearsFeeDataVo> apiFeeVos, List<OwnerDto> ownerDtos) {

        // 遍历所有欠费记录
        for (ApiArrearsFeeDataVo apiFeeVo : apiFeeVos) {
            // 在业主信息中查找对应的记录
            for (OwnerDto ownerDto : ownerDtos) {
                // 通过房间ID匹配
                if (apiFeeVo.getPayerObjId().equals(ownerDto.getRoomId())) {
                    apiFeeVo.setNum(ownerDto.getRoomNum());    // 设置房间号
                    apiFeeVo.setOwnerName(ownerDto.getName()); // 设置业主姓名
                    apiFeeVo.setTel(ownerDto.getLink());       // 设置业主电话
                }
            }
        }
    }

    /**
     * 刷新车位和业主数据
     * 
     * 将业主信息（姓名、电话）和车位编号补充到欠费数据中
     * 
     * @param apiFeeVos 欠费数据列表
     * @param ownerDtos 业主信息列表
     */
    private void freshParkingSpaceAndOwnerData(List<ApiArrearsFeeDataVo> apiFeeVos, List<OwnerDto> ownerDtos) {

        // 遍历所有欠费记录
        for (ApiArrearsFeeDataVo apiFeeVo : apiFeeVos) {
            // 在业主信息中查找对应的记录
            for (OwnerDto ownerDto : ownerDtos) {
                // 通过车位ID匹配
                if (apiFeeVo.getPayerObjId().equals(ownerDto.getPsId())) {
                    apiFeeVo.setNum(ownerDto.getNum());        // 设置车位编号
                    apiFeeVo.setOwnerName(ownerDto.getName()); // 设置业主姓名
                    apiFeeVo.setTel(ownerDto.getLink());       // 设置业主电话
                }
            }
        }
    }

    /**
     * 获取缴费对象ID数组
     * 
     * 从费用列表中提取所有的缴费对象ID（房间ID或车位ID）
     * 
     * @param feeDtos 费用数据列表
     * @return 缴费对象ID数组
     */
    private String[] getObjIds(List<FeeDto> feeDtos) {
        List<String> objIds = new ArrayList<String>();
        // 遍历费用列表，收集所有缴费对象ID
        for (FeeDto feeDto : feeDtos) {
            objIds.add(feeDto.getPayerObjId());
        }

        // 转换为数组返回
        return objIds.toArray(new String[objIds.size()]);
    }
}