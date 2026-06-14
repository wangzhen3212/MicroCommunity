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
 * 类表述：删除
 * 服务编码：parkingBoxArea.deleteParkingBoxArea
 * 请求路劲：/app/parkingBoxArea.DeleteParkingBoxArea
 * add by 吴学文 at 2021-10-18 00:15:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 停车场区域删除命令类
 * 
 * 该类负责处理删除停车场区域关联关系的业务逻辑，包括参数验证、业务规则检查和数据删除操作。
 * 主要功能包括：
 * 1. 验证删除请求的必填参数
 * 2. 检查待删除的停车场区域是否存在且不是默认区域
 * 3. 执行停车场区域数据的删除操作
 * 4. 更新相关停车场信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-18
 */
@Java110Cmd(serviceCode = "parkingBoxArea.deleteParkingBoxArea")
public class DeleteParkingBoxAreaCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(DeleteParkingBoxAreaCmd.class);

    /**
     * 停车场区域服务接口，用于操作停车场区域相关数据
     */
    @Autowired
    private IParkingBoxAreaV1InnerServiceSMO parkingBoxAreaV1InnerServiceSMOImpl;

    /**
     * 停车场服务接口，用于操作停车场相关数据
     */
    @Autowired
    private IParkingBoxV1InnerServiceSMO parkingBoxV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的有效性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含baId字段
        Assert.hasKeyAndValue(reqJson, "baId", "baId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除停车场区域命令
     * 
     * 该方法负责执行实际的停车场区域删除操作，包括：
     * 1. 查询待删除的停车场区域信息
     * 2. 检查是否为默认区域（默认区域不允许删除）
     * 3. 执行删除操作
     * 4. 更新相关停车场信息
     * 5. 设置操作结果响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败或业务规则不满足时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建停车场区域数据传输对象，用于查询条件
        ParkingBoxAreaDto parkingBoxAreaDto = new ParkingBoxAreaDto();
        parkingBoxAreaDto.setBaId(reqJson.getString("baId"));
        parkingBoxAreaDto.setCommunityId(reqJson.getString("communityId"));
        
        // 根据条件查询停车场区域信息
        List<ParkingBoxAreaDto> parkingBoxAreaDtos = parkingBoxAreaV1InnerServiceSMOImpl.queryParkingBoxAreas(parkingBoxAreaDto);
        
        // 验证查询结果必须且只能有一条记录
        Assert.listOnlyOne(parkingBoxAreaDtos, "数据不存在");
        
        // 检查待删除的区域是否为默认区域，默认区域不允许删除
        if (ParkingBoxAreaDto.DEFAULT_AREA_TRUE.equals(parkingBoxAreaDtos.get(0).getDefaultArea())) {
            throw new CmdException("默认停车场不能删除");
        }
        
        // 将请求参数转换为停车场区域持久化对象
        ParkingBoxAreaPo parkingBoxAreaPo = BeanConvertUtil.covertBean(reqJson, ParkingBoxAreaPo.class);
        
        // 执行删除操作，并获取影响的行数
        int flag = parkingBoxAreaV1InnerServiceSMOImpl.deleteParkingBoxArea(parkingBoxAreaPo);
        
        // 检查删除操作是否成功（影响行数应大于等于1）
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
        
        // 创建停车场持久化对象，用于更新相关停车场信息
        ParkingBoxPo parkingBoxPo = new ParkingBoxPo();
        parkingBoxPo.setBoxId(reqJson.getString("boxId"));
        // 更新停车场信息
        parkingBoxV1InnerServiceSMOImpl.updateParkingBox(parkingBoxPo);
        
        // 设置操作成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}