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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionItemTitleDto;
import com.java110.dto.inspection.InspectionItemTitleValueDto;
import com.java110.intf.community.IInspectionItemTitleV1InnerServiceSMO;
import com.java110.intf.community.IInspectionItemTitleValueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：inspectionItemTitle.listInspectionItemTitle
 * 请求路劲：/app/inspectionItemTitle.ListInspectionItemTitle
 * add by 吴学文 at 2022-03-15 11:07:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 巡检项目标题查询命令类
 * 负责处理巡检项目标题的列表查询请求，包括分页查询和关联的巡检项值数据
 * 服务编码：inspectionItemTitle.listInspectionItemTitle
 * @author 吴学文
 * @version 1.0
 * @since 2022-03-15
 */
@Java110Cmd(serviceCode = "inspectionItemTitle.listInspectionItemTitle")
public class ListInspectionItemTitleCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListInspectionItemTitleCmd.class);
    
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
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson); // 调用父类方法验证分页信息
    }

    /**
     * 执行命令方法
     * 处理巡检项目标题的查询请求，包括数据查询、分页处理和关联数据加载
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检项目标题DTO对象
        InspectionItemTitleDto inspectionItemTitleDto = BeanConvertUtil.covertBean(reqJson, InspectionItemTitleDto.class);

        // 查询巡检项目标题总数
        int count = inspectionItemTitleV1InnerServiceSMOImpl.queryInspectionItemTitlesCount(inspectionItemTitleDto);

        List<InspectionItemTitleDto> inspectionItemTitleDtos = null;

        if (count > 0) {
            // 如果存在数据，查询巡检项目标题列表
            inspectionItemTitleDtos = inspectionItemTitleV1InnerServiceSMOImpl.queryInspectionItemTitles(inspectionItemTitleDto);
            // 刷新标题对应的值列表
            refreshTitileValues(inspectionItemTitleDtos);
        } else {
            // 如果没有数据，返回空列表
            inspectionItemTitleDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数/每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, inspectionItemTitleDtos);

        // 构建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新巡检项目标题对应的值列表
     * 为每个巡检项目标题加载其对应的值列表
     * @param inspectionItemTitleDtos 巡检项目标题数据列表
     */
    private void refreshTitileValues(List<InspectionItemTitleDto> inspectionItemTitleDtos) {
        // 参数校验：如果标题列表为空或没有元素，直接返回
        if (inspectionItemTitleDtos == null || inspectionItemTitleDtos.size() < 1) {
            return;
        }

        // 收集所有标题ID
        List<String> titleIds = new ArrayList<>();
        for (InspectionItemTitleDto inspectionItemTitleDto : inspectionItemTitleDtos) {
            titleIds.add(inspectionItemTitleDto.getTitleId());
        }

        // 构建查询条件：根据标题ID列表和小区ID查询对应的值
        InspectionItemTitleValueDto inspectionItemTitleValueDto = new InspectionItemTitleValueDto();
        inspectionItemTitleValueDto.setTitleIds(titleIds.toArray(new String[titleIds.size()]));
        inspectionItemTitleValueDto.setCommunityId(inspectionItemTitleDtos.get(0).getCommunityId());
        
        // 批量查询所有标题对应的值
        List<InspectionItemTitleValueDto> inspectionItemTitleValueDtos
                = inspectionItemTitleValueV1InnerServiceSMOImpl.queryInspectionItemTitleValues(inspectionItemTitleValueDto);

        List<InspectionItemTitleValueDto> tmpInspectionItemTitleValueDtos = null;
        
        // 为每个标题设置对应的值列表
        for (InspectionItemTitleDto tmpInspectionItemTitleDto : inspectionItemTitleDtos) {
            tmpInspectionItemTitleValueDtos = new ArrayList<>();
            // 遍历所有值，找到属于当前标题的值
            for (InspectionItemTitleValueDto inspectionItemTitleValueDto1 : inspectionItemTitleValueDtos) {
                if (tmpInspectionItemTitleDto.getTitleId().equals(inspectionItemTitleValueDto1.getTitleId())) {
                    tmpInspectionItemTitleValueDtos.add(inspectionItemTitleValueDto1);
                }
            }
            // 将值列表设置到标题对象中
            tmpInspectionItemTitleDto.setInspectionItemTitleValueDtos(tmpInspectionItemTitleValueDtos);
        }
    }
}