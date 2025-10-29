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
package com.java110.community.cmd.repair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.repair.RepairTypeUserDto;
import com.java110.intf.community.IRepairTypeUserInnerServiceSMO;
import com.java110.intf.community.IRepairTypeUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：repairTypeUser.listRepairTypeUser
 * 请求路劲：/app/repairTypeUser.ListRepairTypeUser
 * add by 吴学文 at 2022-08-06 12:27:40 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维修类型用户关系查询命令类
 * 
 * 该类负责处理维修类型用户关系列表查询请求，实现分页查询维修类型用户关系数据的功能
 * 通过调用维修类型用户关系服务接口获取数据，并返回标准格式的响应结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-06
 */
@Java110Cmd(serviceCode = "repair.listRepairTypeUsers")
public class ListRepairTypeUserCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListRepairTypeUserCmd.class);
    
    /**
     * 维修类型用户关系服务接口
     */
    @Autowired
    private IRepairTypeUserInnerServiceSMO repairTypeUserInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数是否合法
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
     * 执行维修类型用户关系查询业务逻辑，包括：
     * 1. 参数转换
     * 2. 查询数据总数
     * 3. 查询数据列表
     * 4. 构建响应结果
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON参数转换为RepairTypeUserDto对象，便于后续查询
        RepairTypeUserDto repairTypeUserDto = BeanConvertUtil.covertBean(reqJson, RepairTypeUserDto.class);

        // 查询满足条件的维修类型用户关系总数，用于分页计算
        int count = repairTypeUserInnerServiceSMOImpl.queryRepairTypeUsersCount(repairTypeUserDto);

        List<RepairTypeUserDto> repairTypeUserDtos = null;

        // 根据查询结果数量决定如何处理数据列表
        if (count > 0) {
            // 如果存在数据，则查询具体的数据列表
            repairTypeUserDtos = repairTypeUserInnerServiceSMOImpl.queryRepairTypeUsers(repairTypeUserDto);
        } else {
            // 如果没有数据，返回空列表避免null值
            repairTypeUserDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        // 创建标准结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, repairTypeUserDtos);

        // 构建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应结果设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}