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
package com.java110.store.cmd.complaintType;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IComplaintTypeUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.complaint.ComplaintTypeUserDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询投诉类型用户关联信息
 * 服务编码：complaintTypeUser.listComplaintTypeUser
 * 请求路劲：/app/complaintTypeUser.ListComplaintTypeUser
 * add by 吴学文 at 2024-02-21 12:58:40 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 类功能说明：处理投诉类型用户关联信息的查询命令，包括参数验证和数据查询
 */
@Java110Cmd(serviceCode = "complaintType.listComplaintTypeUser")
public class ListComplaintTypeUserCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListComplaintTypeUserCmd.class);
    
    /**
     * 投诉类型用户关联服务接口
     */
    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
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
     * 处理投诉类型用户关联信息的查询请求，返回分页查询结果
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为ComplaintTypeUserDto对象
        ComplaintTypeUserDto complaintTypeUserDto = BeanConvertUtil.covertBean(reqJson, ComplaintTypeUserDto.class);

        // 查询符合条件的投诉类型用户关联记录总数
        int count = complaintTypeUserV1InnerServiceSMOImpl.queryComplaintTypeUsersCount(complaintTypeUserDto);

        // 声明投诉类型用户关联数据列表
        List<ComplaintTypeUserDto> complaintTypeUserDtos = null;

        // 根据查询结果数量判断是否进行数据查询
        if (count > 0) {
            // 查询投诉类型用户关联数据列表
            complaintTypeUserDtos = complaintTypeUserV1InnerServiceSMOImpl.queryComplaintTypeUsers(complaintTypeUserDto);
        } else {
            // 如果没有数据，返回空列表
            complaintTypeUserDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, complaintTypeUserDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}