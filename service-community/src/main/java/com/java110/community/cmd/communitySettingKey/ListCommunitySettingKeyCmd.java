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
package com.java110.community.cmd.communitySettingKey;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunitySettingDto;
import com.java110.dto.community.CommunitySettingKeyDto;
import com.java110.intf.community.ICommunitySettingInnerServiceSMO;
import com.java110.intf.community.ICommunitySettingKeyV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询小区设置键列表的命令类
 * 服务编码：communitySettingKey.listCommunitySettingKey
 * 请求路劲：/app/communitySettingKey.ListCommunitySettingKey
 * add by 吴学文 at 2021-10-10 10:56:18 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "communitySettingKey.listCommunitySettingKey")
public class ListCommunitySettingKeyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunitySettingKeyCmd.class);
    
    /**
     * 小区设置键服务接口
     */
    @Autowired
    private ICommunitySettingKeyV1InnerServiceSMO communitySettingKeyV1InnerServiceSMOImpl;

    /**
     * 小区设置服务接口
     */
    @Autowired
    private ICommunitySettingInnerServiceSMO communitySettingInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数（当前注释掉）
        //super.validatePageInfo(reqJson);
        
        // 验证必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        // 验证必须包含设置类型
        Assert.hasKeyAndValue(reqJson, "settingType", "未包含类型");
    }

    /**
     * 执行查询小区设置键列表命令
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为CommunitySettingKeyDto对象
        CommunitySettingKeyDto communitySettingKeyDto = BeanConvertUtil.covertBean(reqJson, CommunitySettingKeyDto.class);

        // 查询小区设置键总数
        int count = communitySettingKeyV1InnerServiceSMOImpl.queryCommunitySettingKeysCount(communitySettingKeyDto);

        List<CommunitySettingKeyDto> communitySettingKeyDtos = null;

        if (count > 0) {
            // 如果存在数据，查询小区设置键列表
            communitySettingKeyDtos = communitySettingKeyV1InnerServiceSMOImpl.queryCommunitySettingKeys(communitySettingKeyDto);
        } else {
            // 如果没有数据，返回空列表
            communitySettingKeyDtos = new ArrayList<>();
        }

        // 计算并设置每个设置键的值
        computeKeyValue(communitySettingKeyDtos, reqJson.getString("communityId"), reqJson.getString("settingType"));

        // 设置默认分页行数
        if (!reqJson.containsKey("row")) {
            reqJson.put("row", 10);
        }

        // 计算总页数并创建结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, communitySettingKeyDtos);

        // 创建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 计算并设置小区设置键的值
     * 根据小区ID和设置类型，从小区设置表中获取对应的值并设置到设置键对象中
     * 
     * @param communitySettingKeyDtos 小区设置键列表
     * @param communityId 小区ID
     * @param settingType 设置类型
     */
    private void computeKeyValue(List<CommunitySettingKeyDto> communitySettingKeyDtos, String communityId, String settingType) {
        // 如果设置键列表为空，直接返回
        if (communitySettingKeyDtos == null || communitySettingKeyDtos.size() < 1) {
            return;
        }

        // 构建查询条件
        CommunitySettingDto communitySettingDto = new CommunitySettingDto();
        communitySettingDto.setSettingType(settingType);
        communitySettingDto.setCommunityId(communityId);
        
        // 查询小区设置信息
        List<CommunitySettingDto> communitySettingDtos = communitySettingInnerServiceSMOImpl.queryCommunitySettings(communitySettingDto);
        
        // 如果没有小区设置信息，直接返回
        if (communitySettingDtos == null || communitySettingDtos.size() < 1) {
            return;
        }

        // 遍历所有设置键，匹配对应的设置值
        for (CommunitySettingKeyDto communitySettingKeyDto : communitySettingKeyDtos) {
            for (CommunitySettingDto tmpCommunitySettingDto : communitySettingDtos) {
                // 如果设置键不匹配，继续查找
                if (!communitySettingKeyDto.getSettingKey().equals(tmpCommunitySettingDto.getSettingKey())) {
                    continue;
                }
                // 设置对应的值
                communitySettingKeyDto.setSettingValue(tmpCommunitySettingDto.getSettingValue());
            }
        }
    }
}