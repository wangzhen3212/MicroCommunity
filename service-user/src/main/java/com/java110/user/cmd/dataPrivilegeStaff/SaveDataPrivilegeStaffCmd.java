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
package com.java110.user.cmd.dataPrivilegeStaff;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IDataPrivilegeStaffV1InnerServiceSMO;
import com.java110.po.privilege.DataPrivilegeStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存数据权限员工关系命令类
 * 服务编码：dataPrivilegeStaff.saveDataPrivilegeStaff
 * 请求路劲：/app/dataPrivilegeStaff.SaveDataPrivilegeStaff
 * add by 吴学文 at 2022-09-28 16:48:23 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理数据权限与员工关联关系的保存操作，支持批量保存多个员工的数据权限配置
 */
@Java110Cmd(serviceCode = "dataPrivilegeStaff.saveDataPrivilegeStaff")
public class SaveDataPrivilegeStaffCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveDataPrivilegeStaffCmd.class);

    /**
     * ID生成前缀，用于生成数据权限员工关系ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 数据权限员工关系服务接口
     */
    @Autowired
    private IDataPrivilegeStaffV1InnerServiceSMO dataPrivilegeStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含dpId参数
        Assert.hasKeyAndValue(reqJson, "dpId", "请求报文中未包含dpId");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 验证请求报文中必须包含staffs参数
        Assert.hasKey(reqJson, "staffs", "未包含员工信息");
        JSONArray staffs = reqJson.getJSONArray("staffs");

        // 验证员工列表不能为空
        if(staffs == null || staffs.size()<1){
            throw new CmdException("未包含员工");
        }
    }

    /**
     * 执行保存数据权限员工关系命令
     * 将请求参数转换为数据对象并批量保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为数据权限员工关系PO对象
        DataPrivilegeStaffPo dataPrivilegeStaffPo = BeanConvertUtil.covertBean(reqJson, DataPrivilegeStaffPo.class);

        // 获取员工列表数组
        JSONArray staffs = reqJson.getJSONArray("staffs");

        // 遍历员工列表，为每个员工创建数据权限关系记录
        for(int staffIndex = 0 ; staffIndex < staffs.size(); staffIndex++) {
            // 生成唯一的数据权限员工关系ID
            dataPrivilegeStaffPo.setDpsId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置员工ID
            dataPrivilegeStaffPo.setStaffId(staffs.getJSONObject(staffIndex).getString("staffId"));
            // 设置员工姓名
            dataPrivilegeStaffPo.setStaffName(staffs.getJSONObject(staffIndex).getString("staffName"));
            
            // 调用服务保存数据权限员工关系
            int flag = dataPrivilegeStaffV1InnerServiceSMOImpl.saveDataPrivilegeStaff(dataPrivilegeStaffPo);

            // 检查保存操作是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}