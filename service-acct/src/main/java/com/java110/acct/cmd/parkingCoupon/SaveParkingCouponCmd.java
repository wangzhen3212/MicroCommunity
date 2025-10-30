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
package com.java110.acct.cmd.parkingCoupon;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.parking.ParkingCouponDto;
import com.java110.intf.acct.IParkingCouponV1InnerServiceSMO;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.po.parking.ParkingCouponPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：保存
 * 服务编码：parkingCoupon.saveParkingCoupon
 * 请求路劲：/app/parkingCoupon.SaveParkingCoupon
 * add by 吴学文 at 2022-10-10 13:27:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 停车优惠券保存命令类
 * 负责处理停车优惠券的创建和保存业务逻辑
 * 通过服务编码 parkinCoupon.saveParkingCoupon 对外提供服务
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-10
 */
@Java110Cmd(serviceCode = "parkingCoupon.saveParkingCoupon")
public class SaveParkingCouponCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveParkingCouponCmd.class);

    /**
     * 优惠券ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 停车优惠券服务接口
     */
    @Autowired
    private IParkingCouponV1InnerServiceSMO parkingCouponV1InnerServiceSMOImpl;

    /**
     * 停车区域服务接口
     */
    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "typeCd", "请求报文中未包含typeCd");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "paId", "请求报文中未包含paId");
        
        // 如果是免费类型的优惠券，设置默认值
        if (ParkingCouponDto.TYPE_CD_FREE.equals(reqJson.getString("typeCd"))) {
            reqJson.put("value", "9999");
        }
        
        // 验证金额相关字段
        Assert.hasKeyAndValue(reqJson, "value", "请求报文中未包含value");
        Assert.hasKeyAndValue(reqJson, "valuePrice", "请求报文中未包含valuePrice");
    }

    /**
     * 执行保存停车优惠券命令
     * 处理优惠券的创建、关联停车场信息验证和保存操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存失败或业务逻辑异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为停车优惠券PO对象
        ParkingCouponPo parkingCouponPo = BeanConvertUtil.covertBean(reqJson, ParkingCouponPo.class);
        // 生成优惠券ID
        parkingCouponPo.setCouponId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));

        // 验证停车场是否存在
        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setPaId(parkingCouponPo.getPaId());
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaV1InnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);

        // 确保停车场存在且唯一
        Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
        // 设置停车场名称
        parkingCouponPo.setPaName(parkingAreaDtos.get(0).getNum());

        // 保存停车优惠券信息
        int flag = parkingCouponV1InnerServiceSMOImpl.saveParkingCoupon(parkingCouponPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}