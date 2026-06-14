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
import com.java110.dto.complaint.ComplaintTypeUserDto;
import com.java110.intf.store.IComplaintTypeUserV1InnerServiceSMO;
import com.java110.intf.store.IComplaintTypeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.complaint.ComplaintTypeDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询投诉类型列表的命令类
 * 服务编码：complaintType.listComplaintType
 * 请求路劲：/app/complaintType.ListComplaintType
 * add by 吴学文 at 2024-02-21 12:06:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "complaintType.listComplaintType")
public class ListComplaintTypeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListComplaintTypeCmd.class);
    
    /**
     * 投诉类型服务接口
     */
    @Autowired
    private IComplaintTypeV1InnerServiceSMO complaintTypeV1InnerServiceSMOImpl;

    /**
     * 投诉类型用户关联服务接口
     */
    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(cmdDataFlowContext);
        // 验证必填参数communityId是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 查询投诉类型列表并返回结果
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为投诉类型DTO对象
        ComplaintTypeDto complaintTypeDto = BeanConvertUtil.covertBean(reqJson, ComplaintTypeDto.class);

        // 查询投诉类型总数
        int count = complaintTypeV1InnerServiceSMOImpl.queryComplaintTypesCount(complaintTypeDto);

        List<ComplaintTypeDto> complaintTypeDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询投诉类型列表
            complaintTypeDtos = complaintTypeV1InnerServiceSMOImpl.queryComplaintTypes(complaintTypeDto);
        } else {
            // 如果没有数据，返回空列表
            complaintTypeDtos = new ArrayList<>();
        }

        // 查询每个投诉类型对应的员工信息
        toQueryStaff(complaintTypeDtos);

        // 构建返回结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, complaintTypeDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 查询投诉类型对应的员工信息
     * 为每个投诉类型设置关联的员工列表
     *
     * @param complaintTypeDtos 投诉类型数据列表
     */
    private void toQueryStaff(List<ComplaintTypeDto> complaintTypeDtos) {
        // 如果投诉类型列表为空，直接返回
        if (ListUtil.isNull(complaintTypeDtos)) {
            return;
        }

        // 收集所有投诉类型的编码
        List<String> typeCds = new ArrayList<>();
        for (ComplaintTypeDto complaintTypeDto : complaintTypeDtos) {
            typeCds.add(complaintTypeDto.getTypeCd());
        }

        // 构建投诉类型用户查询条件
        ComplaintTypeUserDto complaintTypeUserDto = new ComplaintTypeUserDto();
        complaintTypeUserDto.setTypeCds(typeCds.toArray(new String[typeCds.size()]));

        // 查询投诉类型用户关联信息
        List<ComplaintTypeUserDto> complaintTypeUserDtos = complaintTypeUserV1InnerServiceSMOImpl.queryComplaintTypeUsers(complaintTypeUserDto);

        // 如果没有关联用户信息，直接返回
        if (ListUtil.isNull(complaintTypeUserDtos)) {
            return;
        }
        
        List<ComplaintTypeUserDto> staffs = null;
        // 为每个投诉类型设置对应的员工列表
        for (ComplaintTypeDto complaintTypeDto : complaintTypeDtos) {
            staffs = new ArrayList<>();
            // 遍历用户关联信息，匹配当前投诉类型
            for(ComplaintTypeUserDto complaintTypeUserDto1 : complaintTypeUserDtos){
                if(complaintTypeDto.getTypeCd().equals(complaintTypeUserDto1.getTypeCd())){
                    staffs.add(complaintTypeUserDto1);
                }
            }
            // 设置投诉类型对应的员工列表
            complaintTypeDto.setStaffs(staffs);
        }
    }
}