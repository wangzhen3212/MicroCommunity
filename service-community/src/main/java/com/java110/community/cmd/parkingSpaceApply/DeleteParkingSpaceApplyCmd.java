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
package com.java110.community.cmd.parkingSpaceApply;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.intf.community.IParkingSpaceApplyV1InnerServiceSMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.po.parking.ParkingSpacePo;
import com.java110.po.parking.ParkingSpaceApplyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

import java.util.List;

/**
 * 类表述：删除车位申请命令类
 * 服务编码：parkingSpaceApply.deleteParkingSpaceApply
 * 请求路劲：/app/parkingSpaceApply.DeleteParkingSpaceApply
 * 功能描述：该命令类用于处理删除车位申请的业务逻辑，包括删除申请记录和相关的车位状态更新
 * add by 吴学文 at 2021-10-18 13:00:02 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "parkingSpaceApply.deleteParkingSpaceApply")
public class DeleteParkingSpaceApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(DeleteParkingSpaceApplyCmd.class);

    @Autowired
    private IParkingSpaceApplyV1InnerServiceSMO parkingSpaceApplyV1InnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 功能描述：验证请求参数中是否包含必要的applyId字段
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验请求参数中必须包含applyId字段
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
    }

    /**
     * 执行删除车位申请命令
     * 功能描述：删除车位申请记录，并根据申请状态更新相关车位的状态
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当删除或更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为车位申请PO对象
        ParkingSpaceApplyPo parkingSpaceApplyPo = BeanConvertUtil.covertBean(reqJson, ParkingSpaceApplyPo.class);
        
        // 调用服务删除车位申请记录
        int flag = parkingSpaceApplyV1InnerServiceSMOImpl.deleteParkingSpaceApply(parkingSpaceApplyPo);
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
        
        // 检查申请状态是否为审核通过待缴费(2002)或完成状态(3003)，并且存在车位ID
        if (!StringUtil.isEmpty(reqJson.getString("state")) && (reqJson.getString("state").equals("2002")
                || reqJson.getString("state").equals("3003"))) { //审核通过待缴费状态或完成状态
            if (!StringUtil.isEmpty(reqJson.getString("psId"))) {
                // 根据车位ID查询车位信息
                ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
                parkingSpaceDto.setPsId(reqJson.getString("psId"));
                List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceV1InnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
                
                // 验证查询结果唯一性
                Assert.listOnlyOne(parkingSpaceDtos, "查询车位失败");
                
                // 更新车位状态为空闲状态
                ParkingSpacePo parkingSpacePo = new ParkingSpacePo();
                parkingSpacePo.setPsId(parkingSpaceDtos.get(0).getPsId());
                parkingSpacePo.setState("F"); //出售 S，出租 H ，空闲 F
                
                // 执行车位状态更新操作
                int i = parkingSpaceV1InnerServiceSMOImpl.updateParkingSpace(parkingSpacePo);
                if (i < 1) {
                    throw new CmdException("更新数据失败");
                }
            }
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}