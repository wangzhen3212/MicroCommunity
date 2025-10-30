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
package com.java110.store.cmd.storeUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IStoreStaffLeaderV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.storeStaffLeader.StoreStaffLeaderDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询店铺员工领导关系列表命令类
 * 服务编码：storeStaffLeader.listStoreStaffLeader
 * 请求路劲：/app/storeStaffLeader.ListStoreStaffLeader
 * add by 吴学文 at 2025-06-03 16:25:11 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理查询店铺员工领导关系列表的请求，包括参数验证和数据查询功能
 */
@Java110Cmd(serviceCode = "storeUser.listStoreStaffLeader")
public class ListStoreStaffLeaderCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListStoreStaffLeaderCmd.class);
    
    /**
     * 店铺员工领导关系服务接口
     */
    @Autowired
    private IStoreStaffLeaderV1InnerServiceSMO storeStaffLeaderV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数验证
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理查询店铺员工领导关系列表的业务逻辑
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);

        // 将请求JSON转换为StoreStaffLeaderDto对象
        StoreStaffLeaderDto storeStaffLeaderDto = BeanConvertUtil.covertBean(reqJson, StoreStaffLeaderDto.class);
        // 设置店铺ID到查询条件中
        storeStaffLeaderDto.setStoreId(storeId);

        // 查询符合条件的记录总数
        int count = storeStaffLeaderV1InnerServiceSMOImpl.queryStoreStaffLeadersCount(storeStaffLeaderDto);

        List<StoreStaffLeaderDto> storeStaffLeaderDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果存在记录，则查询具体的数据列表
            storeStaffLeaderDtos = storeStaffLeaderV1InnerServiceSMOImpl.queryStoreStaffLeaders(storeStaffLeaderDto);
        } else {
            // 如果没有记录，返回空列表
            storeStaffLeaderDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, storeStaffLeaderDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}