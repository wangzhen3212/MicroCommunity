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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.meter.MeterWaterDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IMeterWaterInnerServiceSMO;
import com.java110.intf.fee.IMeterWaterV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：查询水表读数管理命令类
 * 服务编码：meterWater.listMeterWater
 * 请求路劲：/app/meterWater.ListMeterWater
 * add by 吴学文 at 2022-07-21 09:17:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：管理员水表读数查询命令类，用于处理管理员查询水表读数列表的请求
 * 主要职责包括参数验证、数据查询和结果返回
 */
@Java110Cmd(serviceCode = "meterWater.listAdminMeterWaters")
public class ListAdminMeterWatersCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminMeterWatersCmd.class);

    @Autowired
    private IMeterWaterV1InnerServiceSMO meterWaterV1InnerServiceSMOImpl;

    @Autowired
    private IMeterWaterInnerServiceSMO meterWaterInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 主要验证分页参数和管理员权限
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数，确保page和row参数存在且有效
        super.validatePageInfo(reqJson);
        // 验证管理员权限，确保当前用户具有管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行水表读数查询命令
     * 根据查询条件获取水表读数列表，并返回分页结果
     *
     * @param event 命令事件对象
     * @param context 数据流上下文，用于设置响应结果
     * @param reqJson 请求参数的JSON对象，包含查询条件和分页信息
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为MeterWaterDto对象，用于查询条件
        MeterWaterDto meterWaterDto = BeanConvertUtil.covertBean(reqJson, MeterWaterDto.class);

        // 查询符合条件的水表读数总数，用于分页计算
        int count = meterWaterInnerServiceSMOImpl.queryMeterWatersCount(meterWaterDto);

        List<MeterWaterDto> meterWaterDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果存在数据，则查询具体的水表读数列表
            meterWaterDtos = meterWaterInnerServiceSMOImpl.queryMeterWaters(meterWaterDto);
        } else {
            // 如果没有数据，返回空列表
            meterWaterDtos = new ArrayList<>();
        }

        // 构建分页结果对象，计算总页数
        // Math.ceil用于向上取整，确保分页正确
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, meterWaterDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应结果设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}