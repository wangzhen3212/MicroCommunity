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
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.intf.user.IOwnerCarV1InnerServiceSMO;
import com.java110.po.car.OwnerCarPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * 类表述：保存
 * 服务编码：ownerCar.saveOwnerCar
 * 请求路劲：/app/ownerCar.SaveOwnerCar
 * add by 吴学文 at 2021-10-13 16:58:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业主车辆编辑命令类
 * 负责处理业主车辆信息的编辑操作，包括参数验证、业务逻辑处理和数据库更新
 * 服务编码：owner.editOwnerCar
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-13
 */
@Java110Cmd(serviceCode = "owner.editOwnerCar")
public class EditOwnerCarCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(EditOwnerCarCmd.class);

    /** ID编码前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 业主车辆服务接口 */
    @Autowired
    private IOwnerCarV1InnerServiceSMO ownerCarV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "未包含小区ID");
        Assert.jsonObjectHaveKey(reqJson, "carNum", "请求报文中未包含carNum");
        Assert.jsonObjectHaveKey(reqJson, "carId", "请求报文中未包含carId");
        Assert.jsonObjectHaveKey(reqJson, "memberId", "请求报文中未包含memberId");
        Assert.jsonObjectHaveKey(reqJson, "carType", "请求报文中未包含carType");
        
        // 验证小区ID不能为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
    }

    /**
     * 执行命令方法
     * 处理业主车辆信息的编辑业务逻辑，包括数据验证、业务处理和数据库更新
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 构建查询条件，验证车辆信息是否存在
        OwnerCarDto ownerCarDto = new OwnerCarDto();
        ownerCarDto.setMemberId(reqJson.getString("memberId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        ownerCarDto.setCarId(reqJson.getString("carId"));
        List<OwnerCarDto> ownerCarDtos = ownerCarV1InnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 验证只能找到一条车辆记录
        Assert.listOnlyOne(ownerCarDtos, "未找到车辆信息");

        String psId = ownerCarDtos.get(0).getPsId();

        // 检查车位状态，如果车位已被释放则不允许修改
        if (StringUtil.isEmpty(psId) || "-1".equals(psId)) {
            throw new IllegalArgumentException("车位已经被释放，不允许修改车辆信息");
        }
        
        // 设置默认租赁类型为月租
        if (!reqJson.containsKey("leaseType")) {
            reqJson.put("leaseType", OwnerCarDto.LEASE_TYPE_MONTH);
        }

        // 注释掉的代码：处理非月租类型的起止时间设置
//        if (!OwnerCarDto.LEASE_TYPE_MONTH.equals(reqJson.getString("leaseType"))) {
//            reqJson.put("startTime", DateUtil.getFormatTimeString(new Date(), DateUtil.DATE_FORMATE_STRING_B));
//            reqJson.put("endTime", "2050-01-01");
//        }

        // 转换请求数据为PO对象并更新数据库
        OwnerCarPo ownerCarPo = BeanConvertUtil.covertBean(reqJson, OwnerCarPo.class);
        int flag = ownerCarV1InnerServiceSMOImpl.updateOwnerCar(ownerCarPo);

        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("修改数据失败");
        }
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());

        // 如果是成员车辆类型，直接返回
        if (OwnerCarDto.CAR_TYPE_MEMBER.equals(ownerCarDtos.get(0).getCarTypeCd())) {
            return;
        }

        // 重新查询更新后的车辆信息
        ownerCarDto = new OwnerCarDto();
        ownerCarDto.setMemberId(reqJson.getString("memberId"));
        ownerCarDto.setCommunityId(reqJson.getString("communityId"));
        ownerCarDto.setCarId(reqJson.getString("carId"));
        ownerCarDtos = ownerCarV1InnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

        // 更新关联的成员车辆的开始时间和结束时间
        OwnerCarDto tmpOwnerCarDto = new OwnerCarDto();
        tmpOwnerCarDto.setCarId(ownerCarDtos.get(0).getCarId());
        tmpOwnerCarDto.setCarTypeCd(OwnerCarDto.CAR_TYPE_MEMBER);
        List<OwnerCarDto> memberOwnerCarDtos = ownerCarV1InnerServiceSMOImpl.queryOwnerCars(tmpOwnerCarDto);

        // 如果没有成员车辆记录，直接返回
        if (ListUtil.isNull(memberOwnerCarDtos)) {
            return;
        }
        
        // 遍历所有成员车辆记录，更新起止时间
        OwnerCarPo memberOwnerCarPo = null;
        for (OwnerCarDto mOwnerCarDto : memberOwnerCarDtos) {
            memberOwnerCarPo = new OwnerCarPo();
            memberOwnerCarPo.setMemberId(mOwnerCarDto.getMemberId());
            // 设置与主车辆相同的起止时间
            memberOwnerCarPo.setStartTime(DateUtil.getFormatTimeStringA(ownerCarDtos.get(0).getStartTime()));
            memberOwnerCarPo.setEndTime(DateUtil.getFormatTimeStringA(ownerCarDtos.get(0).getEndTime()));
            ownerCarV1InnerServiceSMOImpl.updateOwnerCar(memberOwnerCarPo);
        }
    }
}