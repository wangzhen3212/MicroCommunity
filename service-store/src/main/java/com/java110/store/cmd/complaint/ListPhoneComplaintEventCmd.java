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
package com.java110.store.cmd.complaint;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.complaint.ComplaintEventDto;
import com.java110.intf.store.IComplaintEventV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
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
 * 服务编码：complaintEvent.listComplaintEvent
 * 请求路劲：/app/complaintEvent.ListComplaintEvent
 * add by 吴学文 at 2024-02-21 13:01:48 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 手机端投诉事件查询命令类
 * 
 * 该类负责处理手机端投诉事件列表查询请求，包括参数验证和业务逻辑处理
 * 通过服务编码"complaint.listPhoneComplaintEvent"与前端请求进行映射
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024-02-21
 */
@Java110Cmd(serviceCode = "complaint.listPhoneComplaintEvent")
public class ListPhoneComplaintEventCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListPhoneComplaintEventCmd.class);
    
    /**
     * 投诉事件服务接口，用于查询投诉事件数据
     */
    @Autowired
    private IComplaintEventV1InnerServiceSMO complaintEventV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否符合要求，包括分页参数和必填字段的验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证投诉ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "complaintId", "complaintId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 处理投诉事件查询业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑处理出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为投诉事件DTO对象
        ComplaintEventDto complaintEventDto = BeanConvertUtil.covertBean(reqJson, ComplaintEventDto.class);

        // 查询投诉事件总记录数
        int count = complaintEventV1InnerServiceSMOImpl.queryComplaintEventsCount(complaintEventDto);

        // 定义投诉事件列表
        List<ComplaintEventDto> complaintEventDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在记录，则查询投诉事件详情列表
            complaintEventDtos = complaintEventV1InnerServiceSMOImpl.queryComplaintEvents(complaintEventDto);
        } else {
            // 如果没有记录，返回空列表
            complaintEventDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果VO对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, complaintEventDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}