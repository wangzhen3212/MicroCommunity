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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IComplaintTypeUserV1InnerServiceSMO;
import com.java110.intf.store.IComplaintTypeV1InnerServiceSMO;
import com.java110.po.complaint.ComplaintTypePo;
import com.java110.po.complaint.ComplaintTypeUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 投诉类型保存命令类
 * 
 * 该类负责处理投诉类型的新增操作，包括投诉类型基本信息保存和关联人员信息的保存。
 * 服务编码：complaintType.saveComplaintType
 * 请求路径：/app/complaintType.SaveComplaintType
 * 
 * @author 吴学文 at 2024-02-21 12:06:00 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "complaintType.saveComplaintType")
public class SaveComplaintTypeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveComplaintTypeCmd.class);

    /**
     * 编码前缀，用于生成投诉类型ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 投诉类型服务接口
     */
    @Autowired
    private IComplaintTypeV1InnerServiceSMO complaintTypeV1InnerServiceSMOImpl;

    /**
     * 投诉类型关联人员服务接口
     */
    @Autowired
    private IComplaintTypeUserV1InnerServiceSMO complaintTypeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整有效，包括必填字段检查和关联人员信息验证
     * 
     * @param event              命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文，包含请求报文数据
     * @param reqJson            请求的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "typeName", "请求报文中未包含typeName");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "notifyWay", "请求报文中未包含notifyWay");
        Assert.hasKeyAndValue(reqJson, "appraiseReply", "请求报文中未包含appraiseReply");

        // 验证关联人员信息
        JSONArray staffs = reqJson.getJSONArray("staffs");
        if (ListUtil.isNull(staffs)) {
            throw new CmdException("未包含人员");
        }

        // 遍历所有人员信息，验证每个人员的必填字段
        JSONObject staff = null;
        for(int staffIndex = 0;staffIndex < staffs.size(); staffIndex++){
            staff = staffs.getJSONObject(staffIndex);
            Assert.hasKeyAndValue(staff, "userId", "请求报文中未包含userId");
            Assert.hasKeyAndValue(staff, "name", "请求报文中未包含userName");
        }
    }

    /**
     * 执行保存投诉类型命令
     * 
     * 该方法负责保存投诉类型的基本信息和关联的人员信息，使用事务保证数据一致性
     * 
     * @param event              命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson            请求的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为投诉类型PO对象
        ComplaintTypePo complaintTypePo = BeanConvertUtil.covertBean(reqJson, ComplaintTypePo.class);
        // 生成投诉类型编码
        complaintTypePo.setTypeCd(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 保存投诉类型基本信息
        int flag = complaintTypeV1InnerServiceSMOImpl.saveComplaintType(complaintTypePo);

        // 检查保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 处理关联的人员信息
        JSONArray staffs = reqJson.getJSONArray("staffs");
        JSONObject staff = null;
        // 遍历所有人员，保存投诉类型与人员的关联关系
        for(int staffIndex = 0;staffIndex < staffs.size(); staffIndex++){
            staff = staffs.getJSONObject(staffIndex);
            ComplaintTypeUserPo complaintTypeUserPo = new ComplaintTypeUserPo();
            // 设置投诉类型编码
            complaintTypeUserPo.setTypeCd(complaintTypePo.getTypeCd());
            // 生成关联关系ID
            complaintTypeUserPo.setTypeUserId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置人员姓名
            complaintTypeUserPo.setStaffName(staff.getString("name"));
            // 设置小区ID
            complaintTypeUserPo.setCommunityId(complaintTypePo.getCommunityId());
            // 设置人员ID
            complaintTypeUserPo.setStaffId(staff.getString("userId"));
            // 保存投诉类型与人员的关联关系
            complaintTypeUserV1InnerServiceSMOImpl.saveComplaintTypeUser(complaintTypeUserPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}