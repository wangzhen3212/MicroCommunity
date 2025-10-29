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
package com.java110.community.cmd.parkingBoxArea;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingBoxAreaDto;
import com.java110.intf.community.IParkingBoxAreaV1InnerServiceSMO;
import com.java110.intf.community.IParkingBoxV1InnerServiceSMO;
import com.java110.po.parking.ParkingBoxPo;
import com.java110.po.parking.ParkingBoxAreaPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：更新停车箱区域信息命令类
 * 服务编码：parkingBoxArea.updateParkingBoxArea
 * 请求路劲：/app/parkingBoxArea.UpdateParkingBoxArea
 * 功能描述：该命令类用于处理停车箱区域信息的更新操作，包括更新默认区域设置、同步更新相关停车箱信息等业务逻辑
 * add by 吴学文 at 2021-10-18 00:15:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "parkingBoxArea.updateParkingBoxArea")
public class UpdateParkingBoxAreaCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateParkingBoxAreaCmd.class);

    @Autowired
    private IParkingBoxAreaV1InnerServiceSMO parkingBoxAreaV1InnerServiceSMOImpl;

    @Autowired
    private IParkingBoxV1InnerServiceSMO parkingBoxV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 功能描述：验证请求参数中是否包含必要的字段
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验baId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "baId", "baId不能为空");
        // 校验communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 功能描述：执行停车箱区域信息的更新操作，包括处理默认区域设置、更新相关记录等业务逻辑
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取请求中的默认区域标识
        String defaultArea = reqJson.getString("defaultArea");
        
        // 如果当前要设置为默认区域，则需要先将其他区域取消默认设置
        if (ParkingBoxAreaDto.DEFAULT_AREA_TRUE.equals(defaultArea)) {
            ParkingBoxAreaPo tmpParkingBoxAreaPo = new ParkingBoxAreaPo();
            // 设置停车箱ID，用于查找该停车箱下的所有区域
            tmpParkingBoxAreaPo.setBoxId(reqJson.getString("boxId"));
            // 将其他区域设置为非默认区域
            tmpParkingBoxAreaPo.setDefaultArea(ParkingBoxAreaDto.DEFAULT_AREA_FALSE);
            parkingBoxAreaV1InnerServiceSMOImpl.updateParkingBoxArea(tmpParkingBoxAreaPo);
        }
        
        // 将请求参数转换为停车箱区域持久化对象
        ParkingBoxAreaPo parkingBoxAreaPo = BeanConvertUtil.covertBean(reqJson, ParkingBoxAreaPo.class);
        // 执行停车箱区域信息更新操作
        int flag = parkingBoxAreaV1InnerServiceSMOImpl.updateParkingBoxArea(parkingBoxAreaPo);
        
        // 检查更新是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 查询当前停车箱下的所有区域信息
        ParkingBoxAreaDto parkingBoxAreaDto = new ParkingBoxAreaDto();
        parkingBoxAreaDto.setBoxId(reqJson.getString("boxId"));
        List<ParkingBoxAreaDto> parkingBoxAreaDtos = parkingBoxAreaV1InnerServiceSMOImpl.queryParkingBoxAreas(parkingBoxAreaDto);
        
        // 遍历所有区域，确保只有一个默认区域
        if (parkingBoxAreaDtos != null && parkingBoxAreaDtos.size() > 0) {
            for (ParkingBoxAreaDto parkingBoxArea : parkingBoxAreaDtos) {
                // 排除当前更新的区域，处理其他区域
                if (!parkingBoxArea.getBaId().equals(reqJson.getString("baId"))) {
                    ParkingBoxAreaPo parkingBoxAre = new ParkingBoxAreaPo();
                    parkingBoxAre.setBaId(parkingBoxArea.getBaId());
                    // 将其他区域设置为非默认区域
                    parkingBoxAre.setDefaultArea(ParkingBoxAreaDto.DEFAULT_AREA_FALSE);
                    parkingBoxAreaV1InnerServiceSMOImpl.updateParkingBoxArea(parkingBoxAre);
                }
            }
        }
        
        // 更新停车箱信息，保持数据一致性
        ParkingBoxPo parkingBoxPo = new ParkingBoxPo();
        parkingBoxPo.setBoxId(reqJson.getString("boxId"));
        parkingBoxV1InnerServiceSMOImpl.updateParkingBox(parkingBoxPo);
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}