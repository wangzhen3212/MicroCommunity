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
import com.java110.dto.inspection.InspectionItemTitleDto;
import com.java110.dto.maintainance.MaintainanceItemDto;
import com.java110.dto.maintainance.MaintainanceItemValueDto;
import com.java110.intf.community.IMaintainanceItemV1InnerServiceSMO;
import com.java110.intf.community.IMaintainanceItemValueV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainanceItemPo;
import com.java110.po.maintainance.MaintainanceItemValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：maintainance.updateMaintainanceItem
 * 请求路劲：/app/maintainance.UpdateMaintainanceItem
 * add by 吴学文 at 2022-11-06 14:23:46 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护项更新命令类
 * 
 * 该类负责处理维护项信息的更新操作，包括维护项基本信息的更新以及相关选项值的更新。
 * 当维护项类型为非问题时，还需要处理选项值的删除和重新添加操作。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-06
 */
@Java110Cmd(serviceCode = "maintainance.updateMaintainanceItem")
public class UpdateMaintainanceItemCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateMaintainanceItemCmd.class);

    /**
     * 维护项服务接口
     */
    @Autowired
    private IMaintainanceItemV1InnerServiceSMO maintainanceItemV1InnerServiceSMOImpl;

    /**
     * 维护项选项值服务接口
     */
    @Autowired
    private IMaintainanceItemValueV1InnerServiceSMO maintainanceItemValueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整有效，包括必填字段检查和选项值验证。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "itemId", "itemId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        JSONArray titleValues = null;
        // 检查维护项类型是否为问题类型，如果不是则需要验证选项值
        if (!MaintainanceItemDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            titleValues = reqJson.getJSONArray("titleValues");
            // 验证选项值数组不能为空
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }
    }

    /**
     * 执行维护项更新命令
     * 
     * 该方法负责执行维护项的更新操作，包括：
     * 1. 更新维护项基本信息
     * 2. 对于非问题类型的维护项，删除原有选项值并重新添加新的选项值
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为维护项PO对象
        MaintainanceItemPo maintainanceItemPo = BeanConvertUtil.covertBean(reqJson, MaintainanceItemPo.class);
        // 执行维护项更新操作
        int flag = maintainanceItemV1InnerServiceSMOImpl.updateMaintainanceItem(maintainanceItemPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 如果是问题类型的维护项，直接返回成功
        if (InspectionItemTitleDto.TITLE_TYPE_QUESTIONS.equals(maintainanceItemPo.getTitleType())) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 查询维护项原有的选项值
        MaintainanceItemValueDto maintainanceItemValueDto = new MaintainanceItemValueDto();
        maintainanceItemValueDto.setItemId(reqJson.getString("itemId"));
        List<MaintainanceItemValueDto> maintainanceItemValueDtos = maintainanceItemValueV1InnerServiceSMOImpl.queryMaintainanceItemValues(maintainanceItemValueDto);
        
        // 如果存在原有选项值，则先删除
        if (!ListUtil.isNull(maintainanceItemValueDtos)) {
            MaintainanceItemValuePo deleteMaintainanceItemValuePo = new MaintainanceItemValuePo();
            deleteMaintainanceItemValuePo.setItemId(maintainanceItemPo.getItemId());
            // 执行删除操作
            flag = maintainanceItemValueV1InnerServiceSMOImpl.deleteMaintainanceItemValue(deleteMaintainanceItemValuePo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 重新添加新的选项值
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        MaintainanceItemValuePo maintainanceItemValuePo = null;
        // 遍历选项值数组，逐个添加
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            maintainanceItemValuePo = new MaintainanceItemValuePo();
            // 设置选项值内容
            maintainanceItemValuePo.setItemValue(titleValues.getJSONObject(titleValueIndex).getString("itemValue"));
            // 设置选项排序序号
            maintainanceItemValuePo.setSeq(titleValues.getJSONObject(titleValueIndex).getString("seq"));
            // 设置关联的维护项ID
            maintainanceItemValuePo.setItemId(maintainanceItemPo.getItemId());
            // 生成选项值ID
            maintainanceItemValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            // 设置小区ID
            maintainanceItemValuePo.setCommunityId(maintainanceItemPo.getCommunityId());
            // 保存选项值
            maintainanceItemValueV1InnerServiceSMOImpl.saveMaintainanceItemValue(maintainanceItemValuePo);
        }

        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}