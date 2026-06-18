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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.fee.IFloorShareMeterV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.floorShareMeter.FloorShareMeterDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：floorShareMeter.listFloorShareMeter
 * 请求路劲：/app/floorShareMeter.ListFloorShareMeter
 * add by 吴学文 at 2025-03-25 15:01:11 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 楼层分摊电表查询命令类
 * 
 * 该类负责处理楼层分摊电表的列表查询请求，包括参数验证和数据查询功能
 * 通过服务编码 "meter.listFloorShareMeter" 标识，处理对应的HTTP请求
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2025-03-25
 */
@Java110Cmd(serviceCode = "meter.listFloorShareMeter")
public class ListFloorShareMeterCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListFloorShareMeterCmd.class);
    
    /**
     * 楼层分摊电表服务接口
     */
    @Autowired
    private IFloorShareMeterV1InnerServiceSMO floorShareMeterV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填字段的检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证必填字段communityId是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证属性参数
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 
     * 处理楼层分摊电表的查询请求，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为FloorShareMeterDto对象
        FloorShareMeterDto floorShareMeterDto = BeanConvertUtil.covertBean(reqJson, FloorShareMeterDto.class);

        // 查询符合条件的记录总数
        int count = floorShareMeterV1InnerServiceSMOImpl.queryFloorShareMetersCount(floorShareMeterDto);

        List<FloorShareMeterDto> floorShareMeterDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 有数据时查询详细记录列表
            floorShareMeterDtos = floorShareMeterV1InnerServiceSMOImpl.queryFloorShareMeters(floorShareMeterDto);
        } else {
            // 无数据时返回空列表
            floorShareMeterDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, floorShareMeterDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}