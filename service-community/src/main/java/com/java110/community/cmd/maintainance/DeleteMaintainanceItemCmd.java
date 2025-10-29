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
package com.java110.community.cmd.maintainance;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.maintainance.MaintainanceItemValueDto;
import com.java110.intf.community.IMaintainanceItemV1InnerServiceSMO;
import com.java110.intf.community.IMaintainanceItemValueV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainanceItemPo;
import com.java110.po.maintainance.MaintainanceItemValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：删除维护项命令类
 * 服务编码：maintainance.deleteMaintainanceItem
 * 请求路劲：/app/maintainance.DeleteMaintainanceItem
 * add by 吴学文 at 2022-11-06 14:23:46 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该类用于处理删除维护项及其相关值的业务逻辑，包括参数验证和事务性删除操作
 * 主要职责：
 * 1. 验证删除操作的必要参数
 * 2. 删除维护项主记录
 * 3. 删除关联的维护项值记录
 * 4. 返回操作结果
 */
@Java110Cmd(serviceCode = "maintainance.deleteMaintainanceItem")
public class DeleteMaintainanceItemCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteMaintainanceItemCmd.class);

    /**
     * 维护项服务接口
     */
    @Autowired
    private IMaintainanceItemV1InnerServiceSMO maintainanceItemV1InnerServiceSMOImpl;

    /**
     * 维护项值服务接口
     */
    @Autowired
    private IMaintainanceItemValueV1InnerServiceSMO maintainanceItemValueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含itemId字段
        Assert.hasKeyAndValue(reqJson, "itemId", "itemId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除维护项命令
     * 该方法在事务中执行，确保数据一致性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为维护项PO对象
        MaintainanceItemPo maintainanceItemPo = BeanConvertUtil.covertBean(reqJson, MaintainanceItemPo.class);
        
        // 调用服务删除维护项记录
        int flag = maintainanceItemV1InnerServiceSMOImpl.deleteMaintainanceItem(maintainanceItemPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建维护项值PO对象，用于删除关联的值记录
        MaintainanceItemValuePo maintainanceItemValuePo = new MaintainanceItemValuePo();
        // 设置要删除的维护项ID
        maintainanceItemValuePo.setItemId(maintainanceItemPo.getItemId());

        // 将请求JSON转换为维护项值DTO对象，用于查询关联记录数量
        MaintainanceItemValueDto tmpMaintainanceItemValueDto = BeanConvertUtil.covertBean(reqJson, MaintainanceItemValueDto.class);
        // 查询该维护项下是否存在关联的值记录
        int count = maintainanceItemValueV1InnerServiceSMOImpl.queryMaintainanceItemValuesCount(tmpMaintainanceItemValueDto);

        // 如果存在关联的值记录，则执行删除操作
        if (count > 0) {
            // 删除维护项关联的所有值记录
            flag = maintainanceItemValueV1InnerServiceSMOImpl.deleteMaintainanceItemValue(maintainanceItemValuePo);
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}