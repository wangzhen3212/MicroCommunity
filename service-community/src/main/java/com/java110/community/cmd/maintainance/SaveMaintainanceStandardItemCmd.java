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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.maintainance.MaintainanceStandardItemDto;
import com.java110.intf.community.IMaintainanceStandardItemV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainanceStandardItemPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存维护标准项命令类
 * 服务编码：maintainanceStandardItem.saveMaintainanceStandardItem
 * 请求路劲：/app/maintainanceStandardItem.SaveMaintainanceStandardItem
 * 功能描述：该类用于处理维护标准项的保存操作，包括参数验证和业务逻辑执行
 * add by 吴学文 at 2022-11-06 17:20:01 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "maintainance.saveMaintainanceStandardItem")
public class SaveMaintainanceStandardItemCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveMaintainanceStandardItemCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 维护标准项服务接口
     */
    @Autowired
    private IMaintainanceStandardItemV1InnerServiceSMO maintainanceStandardItemV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括必填字段检查和业务逻辑验证
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "standardId", "请求报文中未包含standardId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 检查是否包含检查项数组
        if(!reqJson.containsKey("items")){
            throw new CmdException("未包含检查项");
        }

        JSONArray items = reqJson.getJSONArray("items");

        // 验证检查项数组是否为空
        if(items.size() < 1){
            throw new CmdException("未包含检查项");
        }
        
        String itemId = "";
        // 创建查询条件对象
        MaintainanceStandardItemDto maintainanceStandardItemDto = new MaintainanceStandardItemDto();
        maintainanceStandardItemDto.setStandardId(reqJson.getString("standardId"));
        List<MaintainanceStandardItemDto> maintainanceStandardItemDtos = null;
        
        // 遍历所有检查项，验证是否已存在
        for(int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            itemId = items.getString(itemIndex);
            maintainanceStandardItemDto.setItemId(itemId);
            // 查询是否已存在相同的维护标准项
            maintainanceStandardItemDtos = maintainanceStandardItemV1InnerServiceSMOImpl.queryMaintainanceStandardItems(maintainanceStandardItemDto);

            // 如果已存在，则抛出异常
            if (maintainanceStandardItemDtos != null && maintainanceStandardItemDtos.size() >0) {
                throw new CmdException(maintainanceStandardItemDtos.get(0).getItemTitle()+"已经添加");
            }
        }

    }

    /**
     * 命令执行方法
     * 功能描述：执行维护标准项的保存操作，支持批量保存多个检查项
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为PO对象
        MaintainanceStandardItemPo maintainanceStandardItemPo = BeanConvertUtil.covertBean(reqJson, MaintainanceStandardItemPo.class);
        JSONArray items = reqJson.getJSONArray("items");
        String itemId = "";
        int flag = 0;
        
        // 遍历所有检查项，逐个保存
        for(int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            itemId = items.getString(itemIndex);
            maintainanceStandardItemPo.setItemId(itemId);
            // 生成主键ID
            maintainanceStandardItemPo.setMsiId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 保存维护标准项
            flag = maintainanceStandardItemV1InnerServiceSMOImpl.saveMaintainanceStandardItem(maintainanceStandardItemPo);

            // 检查保存结果，失败则抛出异常
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}