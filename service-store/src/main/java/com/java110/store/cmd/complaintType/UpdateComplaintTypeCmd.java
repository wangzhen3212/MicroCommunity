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
 * 类表述：更新投诉类型命令类
 * 服务编码：complaintType.updateComplaintType
 * 请求路劲：/app/complaintType.UpdateComplaintType
 * 功能描述：该类用于处理投诉类型更新请求，包括更新投诉类型信息和关联的处理人员信息
 * add by 吴学文 at 2024-02-21 12:06:00 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "complaintType.updateComplaintType")
public class UpdateComplaintTypeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateComplaintTypeCmd.class);

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
     * 功能描述：验证请求参数的有效性，确保必要的参数存在且格式正确
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "typeCd", "typeCd不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        // 获取并验证处理人员列表
        JSONArray staffs = reqJson.getJSONArray("staffs");
        if (ListUtil.isNull(staffs)) {
            throw new CmdException("未包含人员");
        }

        // 遍历验证每个处理人员的信息
        JSONObject staff = null;
        for(int staffIndex = 0;staffIndex < staffs.size(); staffIndex++){
            staff = staffs.getJSONObject(staffIndex);
            // 验证每个人员必须包含userId和name字段
            Assert.hasKeyAndValue(staff, "userId", "请求报文中未包含userId");
            Assert.hasKeyAndValue(staff, "name", "请求报文中未包含userName");
        }
    }

    /**
     * 执行命令方法
     * 功能描述：处理投诉类型更新业务逻辑，包括更新投诉类型信息和重新关联处理人员
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为投诉类型PO对象
        ComplaintTypePo complaintTypePo = BeanConvertUtil.covertBean(reqJson, ComplaintTypePo.class);
        // 更新投诉类型信息
        int flag = complaintTypeV1InnerServiceSMOImpl.updateComplaintType(complaintTypePo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 删除原有的投诉类型关联人员信息
        ComplaintTypeUserPo complaintTypeUserPo = new ComplaintTypeUserPo();
        complaintTypeUserPo.setTypeCd(complaintTypePo.getTypeCd());
        complaintTypeUserV1InnerServiceSMOImpl.deleteComplaintTypeUser(complaintTypeUserPo);

        // 重新添加新的投诉类型关联人员信息
        JSONArray staffs = reqJson.getJSONArray("staffs");
        JSONObject staff = null;
        for(int staffIndex = 0;staffIndex < staffs.size(); staffIndex++){
            staff = staffs.getJSONObject(staffIndex);
            // 创建新的投诉类型关联人员对象
            complaintTypeUserPo = new ComplaintTypeUserPo();
            complaintTypeUserPo.setTypeCd(complaintTypePo.getTypeCd());
            // 生成唯一ID
            complaintTypeUserPo.setTypeUserId(GenerateCodeFactory.getGeneratorId("11"));
            complaintTypeUserPo.setStaffName(staff.getString("name"));
            complaintTypeUserPo.setCommunityId(complaintTypePo.getCommunityId());
            complaintTypeUserPo.setStaffId(staff.getString("userId"));
            // 保存关联人员信息
            complaintTypeUserV1InnerServiceSMOImpl.saveComplaintTypeUser(complaintTypeUserPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}