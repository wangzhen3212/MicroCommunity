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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
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

/**
 * 类表述：删除巡检项目标题命令类
 * 服务编码：inspectionItemTitle.deleteInspectionItemTitle
 * 请求路劲：/app/inspectionItemTitle.DeleteInspectionItemTitle
 * add by 吴学文 at 2022-03-15 11:07:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该类用于处理删除巡检项目标题的业务逻辑，包括删除标题及其关联的标题值数据
 */
@Java110Cmd(serviceCode = "inspectionItemTitle.deleteInspectionItemTitle")
public class DeleteInspectionItemTitleCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteInspectionItemTitleCmd.class);

    /**
     * 巡检项目标题服务接口
     */
    @Autowired
    private IInspectionItemTitleV1InnerServiceSMO inspectionItemTitleV1InnerServiceSMOImpl;
    
    /**
     * 巡检项目标题值服务接口
     */
    @Autowired
    private IInspectionItemTitleValueV1InnerServiceSMO inspectionItemTitleValueV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验请求参数中必须包含titleId
        Assert.hasKeyAndValue(reqJson, "titleId", "titleId不能为空");
        // 校验请求参数中必须包含communityId
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除巡检项目标题命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检项目标题PO对象
        InspectionItemTitlePo inspectionItemTitlePo = BeanConvertUtil.covertBean(reqJson, InspectionItemTitlePo.class);
        // 调用服务删除巡检项目标题
        int flag = inspectionItemTitleV1InnerServiceSMOImpl.deleteInspectionItemTitle(inspectionItemTitlePo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建巡检项目标题值删除对象
        InspectionItemTitleValuePo deleteInspectionItemTitleValuePo = new InspectionItemTitleValuePo();
        // 设置要删除的标题值对应的标题ID
        deleteInspectionItemTitleValuePo.setTitleId(inspectionItemTitlePo.getTitleId());

        // 将请求JSON转换为巡检项目标题值DTO对象，用于查询关联的标题值数量
        InspectionItemTitleValueDto inspectionItemTitleValueDto = BeanConvertUtil.covertBean(reqJson, InspectionItemTitleValueDto.class);
        // 查询该标题下是否存在关联的标题值数据
        int count = inspectionItemTitleValueV1InnerServiceSMOImpl.queryInspectionItemTitleValuesCount(inspectionItemTitleValueDto);

        // 如果存在关联的标题值数据，则执行删除操作
        if (count > 0) {
            // 删除该标题下所有的标题值数据
            flag = inspectionItemTitleValueV1InnerServiceSMOImpl.deleteInspectionItemTitleValue(deleteInspectionItemTitleValuePo);
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}