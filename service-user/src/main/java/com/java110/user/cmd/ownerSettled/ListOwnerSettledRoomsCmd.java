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
package com.java110.user.cmd.ownerSettled;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IOwnerSettledRoomsV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.owner.OwnerSettledRoomsDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：ownerSettledRooms.listOwnerSettledRooms
 * 请求路劲：/app/ownerSettledRooms.ListOwnerSettledRooms
 * add by 吴学文 at 2023-01-26 00:58:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业主入驻房间查询命令类
 * 负责处理业主入驻房间信息的列表查询请求，包括参数验证、数据查询和结果返回
 * 服务编码：ownerSettled.listOwnerSettledRooms
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2023-01-26
 */
@Java110Cmd(serviceCode = "ownerSettled.listOwnerSettledRooms")
public class ListOwnerSettledRoomsCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListOwnerSettledRoomsCmd.class);
    
    /**
     * 业主入驻房间服务接口，用于查询业主入驻房间相关信息
     */
    @Autowired
    private IOwnerSettledRoomsV1InnerServiceSMO ownerSettledRoomsV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和必填字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 处理业主入驻房间列表查询业务逻辑，包括数据查询、结果封装和响应设置
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为OwnerSettledRoomsDto对象
        OwnerSettledRoomsDto ownerSettledRoomsDto = BeanConvertUtil.covertBean(reqJson, OwnerSettledRoomsDto.class);

        // 查询符合条件的业主入驻房间记录总数
        int count = ownerSettledRoomsV1InnerServiceSMOImpl.queryOwnerSettledRoomssCount(ownerSettledRoomsDto);

        List<OwnerSettledRoomsDto> ownerSettledRoomsDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在记录，则查询具体的业主入驻房间列表数据
            ownerSettledRoomsDtos = ownerSettledRoomsV1InnerServiceSMOImpl.queryOwnerSettledRoomss(ownerSettledRoomsDto);
        } else {
            // 如果没有记录，返回空列表
            ownerSettledRoomsDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, ownerSettledRoomsDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}