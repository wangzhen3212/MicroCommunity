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
import com.java110.dto.complaint.ComplaintTypeDto;
import com.java110.dto.complaint.ComplaintTypeUserDto;
import com.java110.intf.store.IComplaintTypeUserV1InnerServiceSMO;
import com.java110.intf.store.IComplaintTypeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
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
 * 服务编码：complaintType.listComplaintType
 * 请求路劲：/app/complaintType.ListComplaintType
 * add by 吴学文 at 2024-02-21 12:06:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 投诉类型查询命令类
 * 
 * 该类用于处理手机端投诉类型查询请求，负责查询投诉类型信息及其关联的员工信息
 * 服务编码：complaint.queryPhoneComplaintType
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024-02-21
 */
@Java110Cmd(serviceCode = "complaint.queryPhoneComplaintType")
public class QueryPhoneComplaintTypeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(QueryPhoneComplaintTypeCmd.class);
    
    /**
     * 投诉类型服务接口
     */
    @Autowired
    private IComplaintTypeV1InnerServiceSMO complaintTypeV1InnerServiceSMOImpl;

    /**
     * 投诉类型员工关联服务接口
     */
    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 处理投诉类型查询请求，包括查询投诉类型列表和关联的员工信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为投诉类型DTO对象
        ComplaintTypeDto complaintTypeDto = BeanConvertUtil.covertBean(reqJson, ComplaintTypeDto.class);

        // 查询投诉类型总数
        int count = complaintTypeV1InnerServiceSMOImpl.queryComplaintTypesCount(complaintTypeDto);

        List<ComplaintTypeDto> complaintTypeDtos = null;

        if (count > 0) {
            // 如果有数据，查询投诉类型列表
            complaintTypeDtos = complaintTypeV1InnerServiceSMOImpl.queryComplaintTypes(complaintTypeDto);
        } else {
            // 如果没有数据，返回空列表
            complaintTypeDtos = new ArrayList<>();
        }

        // 查询每个投诉类型关联的员工信息
        toQueryStaff(complaintTypeDtos);

        // 构建返回结果
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, complaintTypeDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 查询投诉类型关联员工信息
     * 
     * 根据投诉类型列表查询每个类型关联的员工信息，并将员工信息设置到对应的投诉类型对象中
     * 
     * @param complaintTypeDtos 投诉类型列表
     */
    private void toQueryStaff(List<ComplaintTypeDto> complaintTypeDtos) {
        // 如果投诉类型列表为空，直接返回
        if (ListUtil.isNull(complaintTypeDtos)) {
            return;
        }

        // 收集所有投诉类型编码
        List<String> typeCds = new ArrayList<>();
        for (ComplaintTypeDto complaintTypeDto : complaintTypeDtos) {
            typeCds.add(complaintTypeDto.getTypeCd());
        }

        // 构建查询条件
        ComplaintTypeUserDto complaintTypeUserDto = new ComplaintTypeUserDto();
        complaintTypeUserDto.setTypeCds(typeCds.toArray(new String[typeCds.size()]));

        // 批量查询投诉类型员工关联信息
        List<ComplaintTypeUserDto> complaintTypeUserDtos = complaintTypeUserV1InnerServiceSMOImpl.queryComplaintTypeUsers(complaintTypeUserDto);

        // 如果没有关联的员工信息，直接返回
        if (ListUtil.isNull(complaintTypeUserDtos)) {
            return;
        }
        
        List<ComplaintTypeUserDto> staffs = null;
        // 遍历每个投诉类型，设置对应的员工信息
        for (ComplaintTypeDto complaintTypeDto : complaintTypeDtos) {
            staffs = new ArrayList<>();
            // 筛选当前投诉类型对应的员工信息
            for(ComplaintTypeUserDto complaintTypeUserDto1 : complaintTypeUserDtos){
                if(complaintTypeDto.getTypeCd().equals(complaintTypeUserDto1.getTypeCd())){
                    staffs.add(complaintTypeUserDto1);
                }
            }
            // 将员工信息设置到投诉类型对象中
            complaintTypeDto.setStaffs(staffs);
        }
    }
}