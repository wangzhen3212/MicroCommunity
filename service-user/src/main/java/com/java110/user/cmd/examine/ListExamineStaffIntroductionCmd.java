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
package com.java110.user.cmd.examine;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IExamineStaffIntroductionV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.examine.ExamineStaffIntroductionDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询员工介绍审核信息命令类
 * 服务编码：examineStaffIntroduction.listExamineStaffIntroduction
 * 请求路劲：/app/examineStaffIntroduction.ListExamineStaffIntroduction
 * add by 吴学文 at 2023-03-10 17:23:07 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理查询员工介绍审核信息的请求，包括参数验证和数据查询功能
 * 主要职责：
 * 1. 验证请求参数的有效性
 * 2. 查询员工介绍审核信息列表
 * 3. 返回分页查询结果
 */
@Java110Cmd(serviceCode = "examine.listExamineStaffIntroduction")
public class ListExamineStaffIntroductionCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListExamineStaffIntroductionCmd.class);
    
    /**
     * 员工介绍审核信息服务接口
     */
    @Autowired
    private IExamineStaffIntroductionV1InnerServiceSMO examineStaffIntroductionV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否符合要求，包括必填字段和分页信息
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证员工ID不能为空
        Assert.hasKeyAndValue(reqJson, "staffId", "staffId不能为空");
    }

    /**
     * 命令执行方法
     * 处理查询员工介绍审核信息的业务逻辑，包括数据查询和结果返回
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为ExamineStaffIntroductionDto对象
        ExamineStaffIntroductionDto examineStaffIntroductionDto = BeanConvertUtil.covertBean(reqJson, ExamineStaffIntroductionDto.class);

        // 查询符合条件的员工介绍审核信息总数
        int count = examineStaffIntroductionV1InnerServiceSMOImpl.queryExamineStaffIntroductionsCount(examineStaffIntroductionDto);

        List<ExamineStaffIntroductionDto> examineStaffIntroductionDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在数据，则查询具体的员工介绍审核信息列表
            examineStaffIntroductionDtos = examineStaffIntroductionV1InnerServiceSMOImpl.queryExamineStaffIntroductions(examineStaffIntroductionDto);
        } else {
            // 如果没有数据，返回空列表
            examineStaffIntroductionDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, examineStaffIntroductionDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}