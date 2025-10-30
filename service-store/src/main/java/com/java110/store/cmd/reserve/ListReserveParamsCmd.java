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
package com.java110.store.cmd.reserve;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reserve.ReserveParamsOpenTimeDto;
import com.java110.intf.store.IReserveParamsOpenTimeV1InnerServiceSMO;
import com.java110.intf.store.IReserveParamsV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.reserve.ReserveParamsDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：reserveParams.listReserveParams
 * 请求路劲：/app/reserveParams.ListReserveParams
 * add by 吴学文 at 2022-12-05 18:04:04 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 预约参数查询命令类
 * 负责处理预约参数列表查询请求，包括分页查询和关联的开放时间信息
 * @serviceCode reserve.listReserveParams
 * @author 吴学文
 * @version 1.0
 * @date 2022-12-05
 */
@Java110Cmd(serviceCode = "reserve.listReserveParams")
public class ListReserveParamsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReserveParamsCmd.class);
    
    /**
     * 预约参数服务接口
     */
    @Autowired
    private IReserveParamsV1InnerServiceSMO reserveParamsV1InnerServiceSMOImpl;
    
    /**
     * 预约参数开放时间服务接口
     */
    @Autowired
    private IReserveParamsOpenTimeV1InnerServiceSMO reserveParamsOpenTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和必要字段
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
    }

    /**
     * 命令执行方法
     * 执行预约参数查询逻辑，包括数据查询、分页处理和开放时间信息补充
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为预约参数DTO对象
        ReserveParamsDto reserveParamsDto = BeanConvertUtil.covertBean(reqJson, ReserveParamsDto.class);

        // 查询预约参数总记录数
        int count = reserveParamsV1InnerServiceSMOImpl.queryReserveParamssCount(reserveParamsDto);

        List<ReserveParamsDto> reserveParamsDtos = null;

        if (count > 0) {
            // 如果有数据，查询预约参数列表
            reserveParamsDtos = reserveParamsV1InnerServiceSMOImpl.queryReserveParamss(reserveParamsDto);
            // 刷新开放时间信息
            refreshOpenTimes(reserveParamsDtos);
        } else {
            // 如果没有数据，返回空列表
            reserveParamsDtos = new ArrayList<>();
        }

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, reserveParamsDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新开放时间信息
     * 为预约参数列表中的每个参数补充对应的开放时间段信息
     * @param reserveParamsDtos 预约参数列表
     */
    private void refreshOpenTimes(List<ReserveParamsDto> reserveParamsDtos) {
        // 参数校验：如果参数列表为空或没有元素，直接返回
        if (reserveParamsDtos == null || reserveParamsDtos.size() < 1) {
            return;
        }

        // 收集所有参数ID
        List<String> paramsIds = new ArrayList<>();
        for (ReserveParamsDto reserveParamsDto : reserveParamsDtos) {
            paramsIds.add(reserveParamsDto.getParamsId());
        }

        // 构建开放时间查询条件
        ReserveParamsOpenTimeDto reserveParamsOpenTimeDto = new ReserveParamsOpenTimeDto();
        reserveParamsOpenTimeDto.setParamsIds(paramsIds.toArray(new String[paramsIds.size()]));
        
        // 批量查询开放时间信息
        List<ReserveParamsOpenTimeDto> reserveParamsOpenTimes = reserveParamsOpenTimeV1InnerServiceSMOImpl.queryReserveParamsOpenTimes(reserveParamsOpenTimeDto);

        List<ReserveParamsOpenTimeDto> tmpReserveParamsOpenTimeDtos = null;
        // 为每个预约参数设置对应的开放时间段
        for (ReserveParamsDto reserveParamsDto : reserveParamsDtos) {
            tmpReserveParamsOpenTimeDtos = new ArrayList<>();
            // 筛选属于当前参数的开放时间段
            for (ReserveParamsOpenTimeDto tmpReserveParamsOpenTimeDto : reserveParamsOpenTimes) {
                if (tmpReserveParamsOpenTimeDto.getParamsId().equals(reserveParamsDto.getParamsId())) {
                    tmpReserveParamsOpenTimeDtos.add(tmpReserveParamsOpenTimeDto);
                }
            }
            // 设置开放时间段到预约参数对象
            reserveParamsDto.setOpenTimes(tmpReserveParamsOpenTimeDtos);
        }
    }
}