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
import com.java110.dto.maintainance.MaintainanceItemValueDto;
import com.java110.intf.community.IMaintainanceItemV1InnerServiceSMO;
import com.java110.intf.community.IMaintainanceItemValueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.maintainance.MaintainanceItemDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询
 * 服务编码：maintainance.listMaintainanceItem
 * 请求路劲：/app/maintainance.ListMaintainanceItem
 * add by 吴学文 at 2022-11-06 14:23:46 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护项查询命令类
 * 
 * 该类负责处理维护项列表查询请求，包括分页查询维护项信息，
 * 并为每个维护项关联查询其对应的维护项值信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-06
 */
@Java110Cmd(serviceCode = "maintainance.listMaintainanceItem")
public class ListMaintainanceItemCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMaintainanceItemCmd.class);

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
     * 
     * 验证请求参数的有效性，包括分页参数和必要字段的检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 执行维护项查询逻辑，包括分页查询维护项列表，
     * 并为每个维护项关联查询其对应的维护项值信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为维护项DTO对象
        MaintainanceItemDto maintainanceItemDto = BeanConvertUtil.covertBean(reqJson, MaintainanceItemDto.class);
        
        // 查询维护项总数
        int count = maintainanceItemV1InnerServiceSMOImpl.queryMaintainanceItemsCount(maintainanceItemDto);
        List<MaintainanceItemDto> maintainanceItemDtos = null;
        
        if (count > 0) {
            // 查询维护项列表
            maintainanceItemDtos = maintainanceItemV1InnerServiceSMOImpl.queryMaintainanceItems(maintainanceItemDto);
            // 为每个维护项刷新维护项值信息
            refreshTitileValues(maintainanceItemDtos);
        } else {
            // 如果没有数据，返回空列表
            maintainanceItemDtos = new ArrayList<>();
        }
        
        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, maintainanceItemDtos);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新维护项值信息
     * 
     * 为维护项列表中的每个维护项查询并设置对应的维护项值信息
     * 
     * @param maintainanceItemDtos 维护项DTO列表
     */
    private void refreshTitileValues(List<MaintainanceItemDto> maintainanceItemDtos) {
        // 如果维护项列表为空，直接返回
        if (maintainanceItemDtos == null || maintainanceItemDtos.size() < 1) {
            return;
        }
        
        // 收集所有维护项ID
        List<String> itemIds = new ArrayList<>();
        for (MaintainanceItemDto maintainanceItemDto : maintainanceItemDtos) {
            itemIds.add(maintainanceItemDto.getItemId());
        }
        
        // 构建维护项值查询条件
        MaintainanceItemValueDto maintainanceItemValueDto = new MaintainanceItemValueDto();
        maintainanceItemValueDto.setItemIds(itemIds.toArray(new String[itemIds.size()]));
        maintainanceItemValueDto.setCommunityId(maintainanceItemDtos.get(0).getCommunityId());
        
        // 批量查询维护项值信息
        List<MaintainanceItemValueDto> maintainanceItemValueDtos
                = maintainanceItemValueV1InnerServiceSMOImpl.queryMaintainanceItemValues(maintainanceItemValueDto);
        
        List<MaintainanceItemValueDto> tmpMaintainanceItemValueDtos = null;
        
        // 为每个维护项设置对应的维护项值列表
        for (MaintainanceItemDto maintainanceItemDto : maintainanceItemDtos) {
            tmpMaintainanceItemValueDtos = new ArrayList<>();
            // 筛选属于当前维护项的维护项值
            for (MaintainanceItemValueDto tmpMaintainanceItemValueDto : maintainanceItemValueDtos) {
                if (maintainanceItemDto.getItemId().equals(tmpMaintainanceItemValueDto.getItemId())) {
                    tmpMaintainanceItemValueDtos.add(tmpMaintainanceItemValueDto);
                }
            }
            // 设置维护项值列表到维护项对象中
            maintainanceItemDto.setTitleValues(tmpMaintainanceItemValueDtos);
        }
    }
}