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
package com.java110.user.cmd.carBlackWhite;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.machine.CarBlackWhiteDto;
import com.java110.dto.parking.ParkingBoxAreaDto;
import com.java110.intf.common.ICarBlackWhiteInnerServiceSMO;
import com.java110.intf.community.IParkingBoxAreaV1InnerServiceSMO;
import com.java110.intf.user.ICarBlackWhiteV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.carBlackWhite.ApiCarBlackWhiteDataVo;
import com.java110.vo.api.carBlackWhite.ApiCarBlackWhiteVo;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询车辆黑白名单列表命令类
 * 服务编码：carBlackWhite.listCarBlackWhite
 * 请求路劲：/app/carBlackWhite.ListCarBlackWhite
 * 主要功能：处理车辆黑白名单的查询请求，支持分页查询和根据停车场区域过滤
 * add by 吴学文 at 2022-07-16 23:23:24 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "carBlackWhite.listCarBlackWhites")
public class ListCarBlackWhitesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCarBlackWhitesCmd.class);
    
    @Autowired
    private ICarBlackWhiteV1InnerServiceSMO carBlackWhiteV1InnerServiceSMOImpl;

    @Autowired
    private ICarBlackWhiteInnerServiceSMO carBlackWhiteInnerServiceSMOImpl;

    @Autowired
    private IParkingBoxAreaV1InnerServiceSMO parkingBoxAreaV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证小区ID必填
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区ID");

        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行车辆黑白名单查询命令
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为车辆黑白名单DTO对象
        CarBlackWhiteDto carBlackWhiteDto = BeanConvertUtil.covertBean(reqJson, CarBlackWhiteDto.class);
        // 设置停车场区域ID数组
        carBlackWhiteDto.setPaIds(getPaIds(reqJson));

        // 查询车辆黑白名单总数
        int count = carBlackWhiteInnerServiceSMOImpl.queryCarBlackWhitesCount(carBlackWhiteDto);

        List<ApiCarBlackWhiteDataVo> carBlackWhites = null;

        if (count > 0) {
            // 如果有数据，查询车辆黑白名单列表并转换为VO对象
            carBlackWhites = BeanConvertUtil.covertBeanList(carBlackWhiteInnerServiceSMOImpl.queryCarBlackWhites(carBlackWhiteDto), ApiCarBlackWhiteDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            carBlackWhites = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiCarBlackWhiteVo apiCarBlackWhiteVo = new ApiCarBlackWhiteVo();

        apiCarBlackWhiteVo.setTotal(count);
        // 计算总页数
        apiCarBlackWhiteVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiCarBlackWhiteVo.setCarBlackWhites(carBlackWhites);

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCarBlackWhiteVo), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 根据请求参数获取停车场区域ID数组
     * 
     * @param reqJson 请求参数JSON对象
     * @return 停车场区域ID数组，如果未指定boxId则返回null
     * @throws CmdException 当未查到停车场信息时抛出异常
     */
    private String[] getPaIds(JSONObject reqJson) {
        // 检查请求参数中是否包含boxId且不为空
        if (reqJson.containsKey("boxId") && !StringUtil.isEmpty(reqJson.getString("boxId"))) {
            ParkingBoxAreaDto parkingBoxAreaDto = new ParkingBoxAreaDto();
            parkingBoxAreaDto.setBoxId(reqJson.getString("boxId"));
            parkingBoxAreaDto.setCommunityId(reqJson.getString("communityId"));
            // 查询停车场区域信息
            List<ParkingBoxAreaDto> parkingBoxAreaDtos = parkingBoxAreaV1InnerServiceSMOImpl.queryParkingBoxAreas(parkingBoxAreaDto);

            // 验证查询结果
            if (parkingBoxAreaDtos == null || parkingBoxAreaDtos.size() < 1) {
                throw new CmdException("未查到停车场信息");
            }
            
            // 提取所有停车场区域ID
            List<String> paIds = new ArrayList<>();
            for (ParkingBoxAreaDto parkingBoxAreaDto1 : parkingBoxAreaDtos) {
                paIds.add(parkingBoxAreaDto1.getPaId());
            }
            // 转换为数组返回
            String[] paIdss = paIds.toArray(new String[paIds.size()]);
            return paIdss;
        }
        return null;
    }
}