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
package com.java110.dev.cmd.businessTableHis;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.dev.IBusinessTableHisV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.system.BusinessTableHisDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询业务表历史记录列表
 * 服务编码：businessTableHis.ListBusinessTableHis
 * 请求路径：/app/businessTableHis.ListBusinessTableHis
 * 
 * 功能说明：该命令类用于处理业务表历史记录的分页查询请求，支持按条件查询业务表历史记录列表
 * 
 * 主要流程：
 * 1. 验证请求参数和分页信息
 * 2. 将请求JSON转换为业务表历史记录DTO对象
 * 3. 查询符合条件的记录总数
 * 4. 根据分页信息查询业务表历史记录列表
 * 5. 构建分页响应结果并返回
 * 
 * @author 吴学文 at 2021-09-12 13:58:05 mail: 928255095@qq.com
 * @version 1.0
 * 
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "businessTableHis.listBusinessTableHis")
public class ListBusinessTableHisCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListBusinessTableHisCmd.class);
    
    /**
     * 业务表历史记录服务接口
     */
    @Autowired
    private IBusinessTableHisV1InnerServiceSMO businessTableHisV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证请求参数的有效性，包括分页参数等
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
        
        // 可以在此处添加其他自定义参数验证逻辑
        // 例如：验证查询条件参数的有效性等
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行业务表历史记录的查询操作
     * 主要步骤：
     * 1. 转换请求参数为DTO对象
     * 2. 查询记录总数
     * 3. 查询数据列表
     * 4. 构建分页响应结果
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 记录查询开始日志
        logger.debug("开始查询业务表历史记录列表，请求参数：{}", reqJson.toJSONString());

        // 步骤1：将请求JSON转换为业务表历史记录DTO对象
        BusinessTableHisDto businessTableHisDto = BeanConvertUtil.covertBean(reqJson, BusinessTableHisDto.class);

        // 步骤2：查询符合条件的记录总数
        int count = businessTableHisV1InnerServiceSMOImpl.queryBusinessTableHissCount(businessTableHisDto);

        // 步骤3：根据记录总数决定是否查询数据列表
        List<BusinessTableHisDto> businessTableHisDtos = null;
        if (count > 0) {
            // 有数据时，查询业务表历史记录列表
            businessTableHisDtos = businessTableHisV1InnerServiceSMOImpl.queryBusinessTableHiss(businessTableHisDto);
        } else {
            // 无数据时，返回空列表
            businessTableHisDtos = new ArrayList<>();
        }

        // 步骤4：构建分页响应结果
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, businessTableHisDtos);

        // 步骤5：设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
        
        // 记录查询完成日志
        logger.debug("业务表历史记录列表查询完成，共查询到{}条记录", count);
    }
}