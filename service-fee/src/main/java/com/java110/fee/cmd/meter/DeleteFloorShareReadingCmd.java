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
package com.java110.fee.cmd.meter;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floorShareReading.FloorShareReadingDto;
import com.java110.intf.fee.IFloorShareMeterV1InnerServiceSMO;
import com.java110.intf.fee.IFloorShareReadingV1InnerServiceSMO;
import com.java110.po.floorShareMeter.FloorShareMeterPo;
import com.java110.po.floorShareReading.FloorShareReadingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：删除
 * 服务编码：floorShareReading.deleteFloorShareReading
 * 请求路劲：/app/floorShareReading.DeleteFloorShareReading
 * add by 吴学文 at 2025-03-26 09:29:54 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 删除分摊抄表记录命令类
 * 
 * 该类负责处理删除分摊抄表记录的请求，主要功能包括：
 * 1. 验证删除请求参数的完整性
 * 2. 检查要删除的记录是否存在
 * 3. 删除指定的分摊抄表记录
 * 4. 更新关联的分摊仪表信息，将当前读数回退到上一次读数
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2025-03-26
 */
@Java110Cmd(serviceCode = "meter.deleteFloorShareReading")
public class DeleteFloorShareReadingCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteFloorShareReadingCmd.class);

    /**
     * 分摊抄表记录服务接口
     */
    @Autowired
    private IFloorShareReadingV1InnerServiceSMO floorShareReadingV1InnerServiceSMOImpl;

    /**
     * 分摊仪表服务接口
     */
    @Autowired
    private IFloorShareMeterV1InnerServiceSMO floorShareMeterV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：
     * - readingId: 抄表记录ID，用于标识要删除的记录
     * - communityId: 小区ID，用于数据权限控制
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证readingId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "readingId", "readingId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 调用父类的属性验证方法
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行删除分摊抄表记录命令
     * 
     * 该方法的主要执行流程：
     * 1. 根据readingId和communityId查询要删除的记录
     * 2. 验证记录是否存在
     * 3. 删除分摊抄表记录
     * 4. 更新关联的分摊仪表信息，将当前读数回退到上一次读数
     * 5. 返回操作结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当记录不存在或删除失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建查询条件对象
        FloorShareReadingDto floorShareReadingDto = new FloorShareReadingDto();
        floorShareReadingDto.setReadingId(reqJson.getString("readingId"));
        floorShareReadingDto.setCommunityId(reqJson.getString("communityId"));
        
        // 根据条件查询分摊抄表记录
        List<FloorShareReadingDto> floorShareReadingDtos
                = floorShareReadingV1InnerServiceSMOImpl.queryFloorShareReadings(floorShareReadingDto);
        
        // 验证查询结果是否存在
        if (ListUtil.isNull(floorShareReadingDtos)) {
            throw new CmdException("记录不存在");
        }

        // 将请求参数转换为持久化对象
        FloorShareReadingPo floorShareReadingPo = BeanConvertUtil.covertBean(reqJson, FloorShareReadingPo.class);
        // 执行删除操作
        int flag = floorShareReadingV1InnerServiceSMOImpl.deleteFloorShareReading(floorShareReadingPo);

        // 验证删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建分摊仪表更新对象
        FloorShareMeterPo floorShareMeterPo = new FloorShareMeterPo();
        // 设置仪表ID
        floorShareMeterPo.setFsmId(floorShareReadingDtos.get(0).getFsmId());
        // 将当前读数时间设置为上一次读数时间（回退操作）
        floorShareMeterPo.setCurReadingTime(floorShareReadingDtos.get(0).getPreReadingTime());
        // 将当前度数设置为上一次度数（回退操作）
        floorShareMeterPo.setCurDegree(floorShareReadingDtos.get(0).getPreDegrees());
        // 设置小区ID
        floorShareMeterPo.setCommunityId(floorShareReadingDtos.get(0).getCommunityId());
        // 更新分摊仪表信息
        floorShareMeterV1InnerServiceSMOImpl.updateFloorShareMeter(floorShareMeterPo);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}