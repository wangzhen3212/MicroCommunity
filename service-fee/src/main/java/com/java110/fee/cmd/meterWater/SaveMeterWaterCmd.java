/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.fee.cmd.meterWater;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.feeMonth.IPayFeeMonth;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.meter.MeterWaterPo;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 抄表数据保存命令类
 * 主要用于处理水电表抄表数据的保存，包括抄表记录的生成和对应费用的创建
 * 服务编码：meterWater.saveMeterWater
 * 请求路径：/app/meterWater.SaveMeterWater
 * add by 吴学文 at 2022-07-21 09:17:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "meterWater.saveMeterWater")
public class SaveMeterWaterCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMeterWaterCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 房屋信息服务接口 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /** 配置域常量 */
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    /** 水费黑名单配置键 */
    public static final String WATER_BLACK_LIST = "WATER_BLACK_LIST";

    /** 电费黑名单配置键 */
    public static final String ELECTRIC_BLACK_LIST = "ELECTRIC_BLACK_LIST";

    /** 业主信息服务接口 */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /** 费用配置服务接口 */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /** 费用批次服务接口 */
    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    /** 用户信息服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 抄表记录服务接口 */
    @Autowired
    private IMeterWaterV1InnerServiceSMO meterWaterV1InnerServiceSMOImpl;

    /** 费用服务接口 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /** 费用属性服务接口 */
    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和合法
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含费用项");
        Assert.hasKeyAndValue(reqJson, "objType", "请求报文中未包含objType");
        Assert.hasKeyAndValue(reqJson, "objId", "请求报文中未包含objId");
        Assert.hasKeyAndValue(reqJson, "preDegrees", "请求报文中未包含preDegrees");
        Assert.hasKeyAndValue(reqJson, "curDegrees", "请求报文中未包含curDegrees");
        Assert.hasKeyAndValue(reqJson, "preReadingTime", "请求报文中未包含preReadingTime");
        Assert.hasKeyAndValue(reqJson, "curReadingTime", "请求报文中未包含curReadingTime");
        Assert.hasKeyAndValue(reqJson, "objType", "请求报文中未包含objType");
        Assert.hasKeyAndValue(reqJson, "meterType", "请求报文中未包含抄表类型");

        // 验证当前读数是否小于上期读数
        if (reqJson.getDoubleValue("curDegrees") < reqJson.getDoubleValue("preDegrees")) {
            throw new CmdException("当前读数小于上期读数");
        }
    }

    /**
     * 命令执行方法
     * 处理抄表数据的保存逻辑，包括费用生成和抄表记录创建
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeConfigDto> feeConfigDtos = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigs(feeConfigDto);
        if (ListUtil.isNull(feeConfigDtos)) {
            throw new CmdException("费用项不存在");
        }
        reqJson.put("feeTypeCd", feeConfigDtos.get(0).getFeeTypeCd());
        
        // 查询房屋信息
        String objId = reqJson.getString("objId");
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomId(objId);
        List<RoomDto> roomList = roomInnerServiceSMOImpl.queryRooms(roomDto);
        Assert.listOnlyOne(roomList, "查询房屋信息错误！");
        
        // 生成费用批次
        generatorBatch(reqJson);
        
        // 获取抄表对象所属小区ID
        String communityId = roomList.get(0).getCommunityId();
        // 获取表类型
        String meterType = reqJson.getString("meterType");
        
        // 获取水费黑名单配置
        String waterRemark = MappingCache.getRemark(DOMAIN_COMMON, WATER_BLACK_LIST);
        List<String> waterRemarkList = new ArrayList<>();
        if (!StringUtil.isEmpty(waterRemark)) {
            String[] waterSplit = waterRemark.split(",");
            // 将数组转成list集合(水费黑名单集合)
            waterRemarkList = Arrays.asList(waterSplit);
        }
        
        // 获取电费黑名单配置
        String electricRemark = MappingCache.getRemark(DOMAIN_COMMON, ELECTRIC_BLACK_LIST);
        List<String> electricRemarkList = new ArrayList<>();
        if (!StringUtil.isEmpty(electricRemark)) {
            String[] electricSplit = electricRemark.split(",");
            // 将数组转成list集合(电费黑名单集合)
            electricRemarkList = Arrays.asList(electricSplit);
        }

        // 判断是否在黑名单中，如果是则只生成抄表记录不生成费用
        if (waterRemarkList.contains(communityId)
                && FeeConfigDto.FEE_TYPE_CD_METER.equals(reqJson.getString("feeTypeCd"))) {
            // 水费黑名单小区，只生成抄表记录
            reqJson.put("feeId", "-1");
            addMeterWater(reqJson, roomList.get(0));
        } else if (electricRemarkList.contains(communityId)
                && FeeConfigDto.FEE_TYPE_CD_WATER.equals(reqJson.getString("feeTypeCd"))) {
            // 电费黑名单小区，只生成抄表记录
            reqJson.put("feeId", "-1");
            addMeterWater(reqJson, roomList.get(0));
        } else {
            // 正常流程：生成费用和抄表记录
            PayFeePo payFeePo = BeanConvertUtil.covertBean(reqJson, PayFeePo.class);
            payFeePo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
            payFeePo.setIncomeObjId(reqJson.getString("storeId"));
            payFeePo.setAmount("-1"); // 费用金额设为-1，表示待计算
            payFeePo.setStartTime(reqJson.getString("preReadingTime"));
            payFeePo.setEndTime(reqJson.getString("preReadingTime"));
            payFeePo.setPayerObjId(reqJson.getString("objId"));
            //payFeePo.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM);
            payFeePo.setbId("-1");
            payFeePo.setPayerObjType(reqJson.getString("objType"));
            payFeePo.setFeeFlag(FeeDto.FEE_FLAG_ONCE); // 一次性费用标志
            payFeePo.setState(FeeDto.STATE_DOING); // 费用状态为进行中
            payFeePo.setBatchId(reqJson.getString("batchId"));
            payFeePo.setUserId("-1");

            // 先保存抄表记录，否则查询费用时可能查不到
            reqJson.put("feeId", payFeePo.getFeeId());
            addMeterWater(reqJson, roomList.get(0));

            // 保存费用截止时间属性
            FeeAttrPo feeAttrPo = new FeeAttrPo();
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME);
            feeAttrPo.setValue(reqJson.getString("curReadingTime"));
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
            int flag = feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }

            // 保存房屋名称属性
            feeAttrPo = new FeeAttrPo();
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME);
            feeAttrPo.setValue(roomList.get(0).getFloorNum() + "-" + roomList.get(0).getUnitNum() + "-" + roomList.get(0).getRoomNum());
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
             flag = feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }

            // 查询业主信息并保存相关属性
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setCommunityId(reqJson.getString("communityId"));
            ownerDto.setRoomId(reqJson.getString("objId"));
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByRoom(ownerDto);

            if (!ListUtil.isNull(ownerDtos)) {
                // 保存业主ID属性
                feeAttrPo = new FeeAttrPo();
                feeAttrPo.setCommunityId(reqJson.getString("communityId"));
                feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_OWNER_ID);
                feeAttrPo.setValue(ownerDtos.get(0).getOwnerId());
                feeAttrPo.setFeeId(payFeePo.getFeeId());
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                flag = feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }

                // 保存业主联系方式属性
                feeAttrPo = new FeeAttrPo();
                feeAttrPo.setCommunityId(reqJson.getString("communityId"));
                feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_OWNER_LINK);
                feeAttrPo.setValue(ownerDtos.get(0).getLink());
                feeAttrPo.setFeeId(payFeePo.getFeeId());
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                flag = feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }

                // 保存业主姓名属性
                feeAttrPo = new FeeAttrPo();
                feeAttrPo.setCommunityId(reqJson.getString("communityId"));
                feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_OWNER_NAME);
                feeAttrPo.setValue(ownerDtos.get(0).getName());
                feeAttrPo.setFeeId(payFeePo.getFeeId());
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                flag = feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
            
            // 保存费用主记录
             flag = payFeeV1InnerServiceSMOImpl.savePayFee(payFeePo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 添加抄表记录
     * 将抄表数据保存到数据库
     * @param paramInJson 接口调用方传入参数
     * @param roomDto 房屋信息DTO
     */
    public void addMeterWater(JSONObject paramInJson, RoomDto roomDto) {
        // 转换参数为抄表记录PO对象
        MeterWaterPo meterWaterPo = BeanConvertUtil.covertBean(paramInJson, MeterWaterPo.class);
        if (StringUtil.isEmpty(meterWaterPo.getbId())) {
            meterWaterPo.setbId("-1");
        }
        // 生成抄表记录ID
        meterWaterPo.setWaterId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置对象名称（楼层-单元-房间号）
        meterWaterPo.setObjName(roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum());
        // 保存抄表记录
        int flag = meterWaterV1InnerServiceSMOImpl.saveMeterWater(meterWaterPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
    }

    /**
     * 生成批次号
     * 为本次抄表操作生成唯一的批次标识
     * @param reqJson 请求参数JSON对象
     */
    private void generatorBatch(JSONObject reqJson) {
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        // 生成批次ID
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12"));
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL); // 批次状态为正常
        payFeeBatchPo.setMsg("正常");
        
        // 保存批次信息
        int flag = payFeeBatchV1InnerServiceSMOImpl.savePayFeeBatch(payFeeBatchPo);

        if (flag < 1) {
            throw new IllegalArgumentException("生成批次失败");
        }

        // 将批次ID设置到请求参数中
        reqJson.put("batchId", payFeeBatchPo.getBatchId());
    }

}