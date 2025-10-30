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
package com.java110.store.cmd.complaintAppraise;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IComplaintAppraiseV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.complaint.ComplaintAppraiseDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询投诉评价列表命令类
 * 服务编码：complaintAppraise.listComplaintAppraise
 * 请求路劲：/app/complaintAppraise.ListComplaintAppraise
 * add by 吴学文 at 2024-02-21 13:04:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理投诉评价列表查询请求的命令类，负责验证请求参数并查询投诉评价数据
 */
@Java110Cmd(serviceCode = "complaintAppraise.listComplaintAppraise")
public class ListComplaintAppraiseCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListComplaintAppraiseCmd.class);
    
    /**
     * 投诉评价服务接口
     */
    @Autowired
    private IComplaintAppraiseV1InnerServiceSMO complaintAppraiseV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证属性参数
        super.validateProperty(cmdDataFlowContext);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行查询投诉评价列表命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为投诉评价DTO对象
        ComplaintAppraiseDto complaintAppraiseDto = BeanConvertUtil.covertBean(reqJson, ComplaintAppraiseDto.class);

        // 查询投诉评价总数
        int count = complaintAppraiseV1InnerServiceSMOImpl.queryComplaintAppraisesCount(complaintAppraiseDto);

        List<ComplaintAppraiseDto> complaintAppraiseDtos = null;

        if (count > 0) {
            // 如果有数据，查询投诉评价列表
            complaintAppraiseDtos = complaintAppraiseV1InnerServiceSMOImpl.queryComplaintAppraises(complaintAppraiseDto);
        } else {
            // 如果没有数据，返回空列表
            complaintAppraiseDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, complaintAppraiseDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应数据到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}