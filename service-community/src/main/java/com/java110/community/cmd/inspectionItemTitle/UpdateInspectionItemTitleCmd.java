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
package com.java110.community.cmd.inspectionItemTitle;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.inspection.InspectionItemTitleDto;
import com.java110.dto.inspection.InspectionItemTitleValueDto;
import com.java110.intf.community.IInspectionItemTitleV1InnerServiceSMO;
import com.java110.intf.community.IInspectionItemTitleValueV1InnerServiceSMO;
import com.java110.po.inspection.InspectionItemTitlePo;
import com.java110.po.inspection.InspectionItemTitleValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：更新巡检项标题命令类
 * 服务编码：inspectionItemTitle.updateInspectionItemTitle
 * 请求路劲：/app/inspectionItemTitle.UpdateInspectionItemTitle
 * 功能描述：该类用于处理巡检项标题的更新操作，包括标题基本信息的更新和关联选项值的更新
 * add by 吴学文 at 2022-03-15 11:07:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "inspectionItemTitle.updateInspectionItemTitle")
public class UpdateInspectionItemTitleCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateInspectionItemTitleCmd.class);

    @Autowired
    private IInspectionItemTitleV1InnerServiceSMO inspectionItemTitleV1InnerServiceSMOImpl;

    @Autowired
    private IInspectionItemTitleValueV1InnerServiceSMO inspectionItemTitleValueV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 功能描述：验证请求参数的有效性，确保必要的参数存在且符合业务规则
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws IllegalArgumentException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "titleId", "titleId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        
        JSONArray titleValues = null;
        // 如果标题类型不是问答题类型，则需要验证选项值
        if (!InspectionItemTitleDto.TITLE_TYPE_QUESTIONS.equals(reqJson.getString("titleType"))) {
            titleValues = reqJson.getJSONArray("titleValues");
            // 验证选项值数组不能为空
            if (titleValues.size() < 1) {
                throw new IllegalArgumentException("未包含选项");
            }
        }
    }

    /**
     * 执行命令方法
     * 功能描述：执行巡检项标题的更新操作，包括更新标题基本信息和相关的选项值
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当数据操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检项标题PO对象
        InspectionItemTitlePo inspectionItemTitlePo = BeanConvertUtil.covertBean(reqJson, InspectionItemTitlePo.class);
        // 更新巡检项标题基本信息
        int flag = inspectionItemTitleV1InnerServiceSMOImpl.updateInspectionItemTitle(inspectionItemTitlePo);

        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 如果是问答题类型，直接返回成功，不需要处理选项值
        if (InspectionItemTitleDto.TITLE_TYPE_QUESTIONS.equals(inspectionItemTitlePo.getTitleType())) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 查询该标题下现有的所有选项值
        InspectionItemTitleValueDto inspectionItemTitleValueDto = new InspectionItemTitleValueDto();
        inspectionItemTitleValueDto.setTitleId(reqJson.getString("titleId"));
        List<InspectionItemTitleValueDto> inspectionItemTitleValueDtos = inspectionItemTitleValueV1InnerServiceSMOImpl.queryInspectionItemTitleValues(inspectionItemTitleValueDto);
        
        // 如果存在现有的选项值，先删除所有旧的选项值
        if (inspectionItemTitleValueDtos != null && inspectionItemTitleValueDtos.size() > 0) {
            InspectionItemTitleValuePo deleteInspectionItemTitleValuePo = new InspectionItemTitleValuePo();
            deleteInspectionItemTitleValuePo.setTitleId(inspectionItemTitlePo.getTitleId());
            flag = inspectionItemTitleValueV1InnerServiceSMOImpl.deleteInspectionItemTitleValue(deleteInspectionItemTitleValuePo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 重新添加新的选项值
        JSONArray titleValues = reqJson.getJSONArray("titleValues");
        InspectionItemTitleValuePo reportInfoSettingTitleValuePo = null;
        // 遍历选项值数组，逐个创建并保存选项值
        for (int titleValueIndex = 0; titleValueIndex < titleValues.size(); titleValueIndex++) {
            reportInfoSettingTitleValuePo = new InspectionItemTitleValuePo();
            // 设置选项值内容
            reportInfoSettingTitleValuePo.setItemValue(titleValues.getJSONObject(titleValueIndex).getString("itemValue"));
            // 设置选项排序序号
            reportInfoSettingTitleValuePo.setSeq(titleValues.getJSONObject(titleValueIndex).getString("seq"));
            // 设置关联的标题ID
            reportInfoSettingTitleValuePo.setTitleId(inspectionItemTitlePo.getTitleId());
            // 生成选项值ID
            reportInfoSettingTitleValuePo.setValueId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_valueId));
            // 设置小区ID
            reportInfoSettingTitleValuePo.setCommunityId(inspectionItemTitlePo.getCommunityId());
            // 保存选项值到数据库
            inspectionItemTitleValueV1InnerServiceSMOImpl.saveInspectionItemTitleValue(reportInfoSettingTitleValuePo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}