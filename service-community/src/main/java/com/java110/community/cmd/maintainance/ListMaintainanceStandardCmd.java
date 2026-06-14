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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.maintainance.MaintainanceStandardItemDto;
import com.java110.intf.community.IMaintainanceStandardItemV1InnerServiceSMO;
import com.java110.intf.community.IMaintainanceStandardV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.maintainance.MaintainanceStandardDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询
 * 服务编码：maintainanceStandard.listMaintainanceStandard
 * 请求路劲：/app/maintainanceStandard.ListMaintainanceStandard
 * add by 吴学文 at 2022-11-06 17:08:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护标准查询命令类
 * 
 * 该类负责处理维护标准列表查询请求，包括查询维护标准信息及其关联的项目数量
 * 主要功能包括参数验证、分页查询维护标准数据、关联查询维护项目数量并返回结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-06
 */
@Java110Cmd(serviceCode = "maintainance.listMaintainanceStandard")
public class ListMaintainanceStandardCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMaintainanceStandardCmd.class);

    /**
     * 维护标准服务接口
     */
    @Autowired
    private IMaintainanceStandardV1InnerServiceSMO maintainanceStandardV1InnerServiceSMOImpl;

    /**
     * 维护标准项目服务接口
     */
    @Autowired
    private IMaintainanceStandardItemV1InnerServiceSMO maintainanceStandardItemV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必要字段的检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "小区不存在");
    }

    /**
     * 执行查询维护标准命令
     * 
     * 根据查询条件获取维护标准列表，并关联查询每个标准的项目数量，
     * 最后将结果封装成标准响应格式返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为维护标准DTO对象
        MaintainanceStandardDto maintainanceStandardDto = BeanConvertUtil.covertBean(reqJson, MaintainanceStandardDto.class);
        
        // 查询符合条件的维护标准总数
        int count = maintainanceStandardV1InnerServiceSMOImpl.queryMaintainanceStandardsCount(maintainanceStandardDto);
        
        List<MaintainanceStandardDto> maintainanceStandardDtos = null;
        
        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询维护标准列表数据
            maintainanceStandardDtos = maintainanceStandardV1InnerServiceSMOImpl.queryMaintainanceStandards(maintainanceStandardDto);
            // 刷新每个维护标准的项目数量
            refreshItemCount(maintainanceStandardDtos);
        } else {
            // 如果没有查询到数据，返回空列表
            maintainanceStandardDtos = new ArrayList<>();
        }
        
        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, maintainanceStandardDtos);
        
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        // 设置响应数据到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新维护标准项目数量
     * 
     * 根据维护标准列表查询每个标准对应的项目数量，并设置到对应的DTO对象中
     * 
     * @param maintainanceStandardDtos 维护标准DTO列表
     */
    private void refreshItemCount(List<MaintainanceStandardDto> maintainanceStandardDtos) {
        // 参数有效性检查
        if (maintainanceStandardDtos == null || maintainanceStandardDtos.size() < 1) {
            return;
        }
        
        // 收集所有维护标准ID
        List<String> standardIds = new ArrayList<>();
        for (MaintainanceStandardDto maintainanceStandardDto : maintainanceStandardDtos) {
            standardIds.add(maintainanceStandardDto.getStandardId());
        }
        
        // 构建维护标准项目查询条件
        MaintainanceStandardItemDto maintainanceStandardItemDto = new MaintainanceStandardItemDto();
        // 设置标准ID数组
        maintainanceStandardItemDto.setStandardIds(standardIds.toArray(new String[standardIds.size()]));
        // 设置小区ID（取第一个标准的小区ID）
        maintainanceStandardItemDto.setCommunityId(maintainanceStandardDtos.get(0).getCommunityId());
        
        // 查询维护标准项目的分组统计信息（按标准ID分组统计项目数量）
        List<MaintainanceStandardItemDto> maintainanceStandardItemDtos = maintainanceStandardItemV1InnerServiceSMOImpl.queryMaintainanceStandardItemsGroupCount(maintainanceStandardItemDto);
        
        // 遍历维护标准列表，设置每个标准的项目数量
        for (MaintainanceStandardDto maintainanceStandardDto : maintainanceStandardDtos) {
            for (MaintainanceStandardItemDto tmpMaintainanceStandardItemDto : maintainanceStandardItemDtos) {
                // 匹配标准ID，设置项目数量
                if (maintainanceStandardDto.getStandardId().equals(tmpMaintainanceStandardItemDto.getStandardId()))
                    maintainanceStandardDto.setItemCount(tmpMaintainanceStandardItemDto.getItemCount());
            }
        }
    }
}