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
package com.java110.community.cmd.activities;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.file.FileRelDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.IActivitiesV1InnerServiceSMO;
import com.java110.po.activities.ActivitiesPo;
import com.java110.po.file.FileRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：更新活动信息命令类
 * 服务编码：activities.updateActivities
 * 请求路径：/app/activities.UpdateActivities
 * 功能描述：该类负责处理活动信息的更新操作，包括活动基本信息的更新和活动头图的文件关联管理
 * add by 吴学文 at 2022-06-19 10:49:17 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "activities.updateActivities")
public class UpdateActivitiesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateActivitiesCmd.class);

    /**
     * 文件关联服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 活动信息服务接口
     */
    @Autowired
    private IActivitiesV1InnerServiceSMO activitiesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证更新活动信息所需的必填参数是否完整
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证活动ID不能为空
        Assert.hasKeyAndValue(reqJson, "activitiesId", "活动ID不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
        // 验证活动标题不能为空
        Assert.hasKeyAndValue(reqJson, "title", "必填，请填写业活动标题");
        // 验证活动类型不能为空
        Assert.hasKeyAndValue(reqJson, "typeCd", "必填，请选择活动类型");
        // 验证头部照片不能为空
        Assert.hasKeyAndValue(reqJson, "headerImg", "必填，请选择头部照片");
        // 验证活动内容不能为空
        Assert.hasKeyAndValue(reqJson, "context", "必填，请填写活动内容");
        // 验证开始时间不能为空
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择开始时间");
        // 验证结束时间不能为空
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择结束时间");
    }

    /**
     * 执行更新活动命令
     * 功能描述：处理活动信息的更新操作，包括活动头图的文件关联管理和活动基本信息的更新
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 处理活动头图文件关联逻辑
        if (reqJson.containsKey("headerImg") && !StringUtils.isEmpty(reqJson.getString("headerImg"))) {
            // 构建文件关联查询条件
            FileRelDto fileRelDto = new FileRelDto();
            fileRelDto.setRelTypeCd("70000"); // 设置关联类型为活动头图
            fileRelDto.setObjId(reqJson.getString("activitiesId")); // 设置关联对象ID为活动ID
            
            // 查询已存在的文件关联记录
            List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
            
            if (fileRelDtos == null || fileRelDtos.size() == 0) {
                // 如果没有找到文件关联记录，则创建新的文件关联
                FileRelPo fileRelPo = new FileRelPo();
                fileRelPo.setFileRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId)); // 生成文件关联ID
                fileRelPo.setFileRealName(reqJson.getString("headerImg")); // 设置文件真实名称
                fileRelPo.setFileSaveName(reqJson.getString("headerImg")); // 设置文件保存名称
                fileRelPo.setObjId(reqJson.getString("activitiesId")); // 设置关联对象ID
                fileRelPo.setSaveWay("table"); // 设置保存方式为表存储
                fileRelPo.setRelTypeCd("70000"); // 设置关联类型为活动头图
                
                // 保存文件关联记录
                int flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("保存广告失败");
                }
            } else {
                // 如果已存在文件关联记录，则更新现有记录
                FileRelPo fileRelPo = new FileRelPo();
                fileRelPo.setFileRelId(fileRelDtos.get(0).getFileRelId()); // 使用现有的文件关联ID
                fileRelPo.setFileRealName(reqJson.getString("headerImg")); // 更新文件真实名称
                fileRelPo.setFileSaveName(reqJson.getString("headerImg")); // 更新文件保存名称
                fileRelPo.setObjId(reqJson.getString("activitiesId")); // 更新关联对象ID
                
                // 更新文件关联记录
                int flag = fileRelInnerServiceSMOImpl.updateFileRel(fileRelPo);
                if (flag < 1) {
                    throw new CmdException("保存广告失败");
                }
            }
        }
        
        // 将请求参数转换为活动PO对象
        ActivitiesPo activitiesPo = BeanConvertUtil.covertBean(reqJson, ActivitiesPo.class);
        
        // 更新活动基本信息
        int flag = activitiesV1InnerServiceSMOImpl.updateActivities(activitiesPo);
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}