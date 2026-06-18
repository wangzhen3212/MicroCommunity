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
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.parking.ParkingCouponDto;
import com.java110.intf.acct.IParkingCouponV1InnerServiceSMO;
import com.java110.intf.community.IParkingAreaV1InnerServiceSMO;
import com.java110.po.parking.ParkingCouponPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：parkingCoupon.updateParkingCoupon
 * 请求路劲：/app/parkingCoupon.UpdateParkingCoupon
 * add by 吴学文 at 2022-10-10 13:27:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 停车券更新命令类
 * 负责处理停车券信息的更新操作，包括参数验证、业务逻辑处理和数据库更新
 * 服务编码：parkingCoupon.updateParkingCoupon
 * 请求路径：/app/parkingCoupon.UpdateParkingCoupon
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-10
 */
@Java110Cmd(serviceCode = "parkingCoupon.updateParkingCoupon")
public class UpdateParkingCouponCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateParkingCouponCmd.class);

    /**
     * 停车券服务接口
     */
    @Autowired
    private IParkingCouponV1InnerServiceSMO parkingCouponV1InnerServiceSMOImpl;

    /**
     * 停车场区域服务接口
     */
    @Autowired
    private IParkingAreaV1InnerServiceSMO parkingAreaV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数couponId是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "couponId", "couponId不能为空");
        // 验证必填参数communityId是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        
        // 如果券类型为免费类型，设置默认面值
        if(ParkingCouponDto.TYPE_CD_FREE.equals(reqJson.getString("typeCd"))){
            reqJson.put("value","9999"); // 免费券设置固定面值9999
        }
    }

    /**
     * 执行停车券更新命令
     * 处理停车券信息的更新逻辑，包括数据转换、停车场信息查询和数据库更新
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为停车券持久化对象
        ParkingCouponPo parkingCouponPo = BeanConvertUtil.covertBean(reqJson, ParkingCouponPo.class);

        // 如果停车场ID为空，需要查询停车场信息并设置停车场名称
        if(StringUtil.isEmpty(parkingCouponPo.getPaId())) {
            // 创建停车场查询条件对象
            ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
            parkingAreaDto.setPaId(parkingCouponPo.getPaId());
            // 查询停车场信息
            List<ParkingAreaDto> parkingAreaDtos = parkingAreaV1InnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
            // 验证查询结果唯一性
            Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
            // 设置停车场名称
            parkingCouponPo.setPaName(parkingAreaDtos.get(0).getNum());
        }

        // 调用服务层更新停车券信息
        int flag = parkingCouponV1InnerServiceSMOImpl.updateParkingCoupon(parkingCouponPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}