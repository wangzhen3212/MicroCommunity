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
package com.java110.community.cmd.parkingAreaText;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.parking.ParkingAreaTextDto;
import com.java110.intf.community.IParkingAreaTextV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：parkingAreaText.listParkingAreaText
 * 请求路劲：/app/parkingAreaText.ListParkingAreaText
 * add by 吴学文 at 2021-09-23 18:15:21 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 停车区域文本查询命令类
 * 
 * 该类负责处理停车区域文本信息的列表查询请求，实现分页查询功能，
 * 将查询结果封装成标准格式返回给前端
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-09-23
 */
@Java110Cmd(serviceCode = "parkingAreaText.listParkingAreaText")
public class ListParkingAreaTextCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListParkingAreaTextCmd.class);
    
    /**
     * 停车区域文本服务接口，用于查询停车区域文本数据
     */
    @Autowired
    private IParkingAreaTextV1InnerServiceSMO parkingAreaTextV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 执行停车区域文本的查询逻辑，包括数据查询、分页处理和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为停车区域文本数据传输对象
        ParkingAreaTextDto parkingAreaTextDto = BeanConvertUtil.covertBean(reqJson, ParkingAreaTextDto.class);

        // 查询符合条件的停车区域文本总数量
        int count = parkingAreaTextV1InnerServiceSMOImpl.queryParkingAreaTextsCount(parkingAreaTextDto);

        List<ParkingAreaTextDto> parkingAreaTextDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在数据，则查询具体的停车区域文本列表
            parkingAreaTextDtos = parkingAreaTextV1InnerServiceSMOImpl.queryParkingAreaTexts(parkingAreaTextDto);
        } else {
            // 如果没有数据，返回空列表
            parkingAreaTextDtos = new ArrayList<>();
        }

        // 计算总页数：总数量 / 每页行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, parkingAreaTextDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给调用方
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}