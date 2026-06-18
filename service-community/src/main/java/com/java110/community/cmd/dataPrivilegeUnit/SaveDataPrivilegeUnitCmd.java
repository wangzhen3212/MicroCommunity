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
package com.java110.community.cmd.dataPrivilegeUnit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.IDataPrivilegeUnitV1InnerServiceSMO;
import com.java110.po.privilege.DataPrivilegeUnitPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据权限单元保存命令类
 * 
 * 该类负责处理数据权限单元的保存操作，包括参数验证和批量保存数据权限单元信息。
 * 通过注解标识服务编码，接收前端请求并处理数据权限单元的新增功能。
 * 
 * 类表述：保存
 * 服务编码：dataPrivilegeUnit.saveDataPrivilegeUnit
 * 请求路劲：/app/dataPrivilegeUnit.SaveDataPrivilegeUnit
 * add by 吴学文 at 2022-09-28 16:32:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dataPrivilegeUnit.saveDataPrivilegeUnit")
public class SaveDataPrivilegeUnitCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveDataPrivilegeUnitCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 数据权限单元服务接口
     */
    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含dpId参数
        Assert.hasKeyAndValue(reqJson, "dpId", "请求报文中未包含dpId");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中必须包含units参数
        Assert.hasKey(reqJson,"units","不包含单元");
    }

    /**
     * 执行保存数据权限单元命令
     * 
     * 将请求参数转换为数据对象，并批量保存数据权限单元信息
     * 使用事务保证数据一致性，如果保存失败则回滚事务
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为数据权限单元PO对象
        DataPrivilegeUnitPo dataPrivilegeUnitPo = BeanConvertUtil.covertBean(reqJson, DataPrivilegeUnitPo.class);

        // 获取单元列表数组
        JSONArray units = reqJson.getJSONArray("units");

        // 遍历所有单元，逐个保存数据权限单元信息
        for(int unitIndex = 0 ; unitIndex < units.size(); unitIndex++) {
            // 生成数据权限单元ID
            dataPrivilegeUnitPo.setDpUnitId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置单元ID
            dataPrivilegeUnitPo.setUnitId(units.getJSONObject(unitIndex).getString("unitId"));
            // 设置单元编号
            dataPrivilegeUnitPo.setUnitNum(units.getJSONObject(unitIndex).getString("unitNum"));
            // 设置楼层ID
            dataPrivilegeUnitPo.setFloorId(units.getJSONObject(unitIndex).getString("floorId"));
            // 设置楼层编号
            dataPrivilegeUnitPo.setFloorNum(units.getJSONObject(unitIndex).getString("floorNum"));
            
            // 调用服务保存数据权限单元信息
            int flag = dataPrivilegeUnitV1InnerServiceSMOImpl.saveDataPrivilegeUnit(dataPrivilegeUnitPo);

            // 检查保存结果，如果失败则抛出异常
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}