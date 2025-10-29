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
import com.java110.intf.user.IExamineStaffProjectV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.examine.ExamineStaffProjectDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：examineStaffProject.listExamineStaffProject
 * 请求路劲：/app/examineStaffProject.ListExamineStaffProject
 * add by 吴学文 at 2023-03-07 16:09:46 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 审核员工项目管理命令类
 * 负责处理审核员工项目列表查询请求，包括参数验证、数据查询和结果返回
 * 服务编码：examine.listExamineStaffProject
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-03-07
 */
@Java110Cmd(serviceCode = "examine.listExamineStaffProject")
public class ListExamineStaffProjectCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListExamineStaffProjectCmd.class);
    
    /**
     * 审核员工项目服务接口
     */
    @Autowired
    private IExamineStaffProjectV1InnerServiceSMO examineStaffProjectV1InnerServiceSMOImpl;

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
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 处理审核员工项目列表查询业务逻辑，包括数据查询、结果封装和响应返回
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为ExamineStaffProjectDto对象
        ExamineStaffProjectDto examineStaffProjectDto = BeanConvertUtil.covertBean(reqJson, ExamineStaffProjectDto.class);

        // 查询审核员工项目总数
        int count = examineStaffProjectV1InnerServiceSMOImpl.queryExamineStaffProjectsCount(examineStaffProjectDto);

        // 声明审核员工项目列表
        List<ExamineStaffProjectDto> examineStaffProjectDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果有数据，查询具体的审核员工项目列表
            examineStaffProjectDtos = examineStaffProjectV1InnerServiceSMOImpl.queryExamineStaffProjects(examineStaffProjectDto);
        } else {
            // 如果没有数据，返回空列表
            examineStaffProjectDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, examineStaffProjectDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}