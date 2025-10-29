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
package com.java110.community.cmd.communitySpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunitySpacePersonDto;
import com.java110.dto.community.CommunitySpacePersonTimeDto;
import com.java110.intf.community.ICommunitySpacePersonTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpacePersonV1InnerServiceSMO;
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
 * 类表述：查询社区空间人员信息命令类
 * 服务编码：communitySpacePerson.listCommunitySpacePerson
 * 请求路劲：/app/communitySpacePerson.ListCommunitySpacePerson
 * 功能描述：该命令类用于处理查询社区空间人员列表的请求，包括分页查询和关联的时间信息查询
 * add by 吴学文 at 2022-09-30 11:36:52 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "communitySpace.listCommunitySpacePerson")
public class ListCommunitySpacePersonCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunitySpacePersonCmd.class);
    
    /**
     * 社区空间人员服务接口
     */
    @Autowired
    private ICommunitySpacePersonV1InnerServiceSMO communitySpacePersonV1InnerServiceSMOImpl;

    /**
     * 社区空间人员时间服务接口
     */
    @Autowired
    private ICommunitySpacePersonTimeV1InnerServiceSMO communitySpacePersonTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数，主要验证分页参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 功能描述：执行查询社区空间人员列表的主要业务逻辑，包括数据查询、时间信息刷新和结果返回
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为CommunitySpacePersonDto对象
        CommunitySpacePersonDto communitySpacePersonDto = BeanConvertUtil.covertBean(reqJson, CommunitySpacePersonDto.class);

        // 查询社区空间人员总数
        int count = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersonsCount(communitySpacePersonDto);

        List<CommunitySpacePersonDto> communitySpacePersonDtos = null;

        if (count > 0) {
            // 如果有数据，查询社区空间人员列表
            communitySpacePersonDtos = communitySpacePersonV1InnerServiceSMOImpl.queryCommunitySpacePersons(communitySpacePersonDto);
            // 刷新开放时间信息
            refreshOpenTimes(communitySpacePersonDtos);
        } else {
            // 如果没有数据，返回空列表
            communitySpacePersonDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, communitySpacePersonDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新开放时间信息方法
     * 功能描述：为社区空间人员列表中的每个人员查询并设置相关的开放时间信息
     * @param communitySpacePersonDtos 社区空间人员列表
     */
    private void refreshOpenTimes(List<CommunitySpacePersonDto> communitySpacePersonDtos) {
        // 参数校验：如果人员列表为空或null，直接返回
        if (communitySpacePersonDtos == null || communitySpacePersonDtos.size() < 1) {
            return;
        }

        // 收集所有社区空间人员ID
        List<String> cspIds = new ArrayList<>();
        for (CommunitySpacePersonDto communitySpaceDto : communitySpacePersonDtos) {
            cspIds.add(communitySpaceDto.getCspId());
        }

        // 构建时间查询条件
        CommunitySpacePersonTimeDto communitySpaceOpenTimeDto = new CommunitySpacePersonTimeDto();
        communitySpaceOpenTimeDto.setCspIds(cspIds.toArray(new String[cspIds.size()]));
        // 设置查询状态：待确认和已完成
        communitySpaceOpenTimeDto.setStates(new String[]{
                CommunitySpacePersonTimeDto.STATE_WAIT_CONFIRM,
                CommunitySpacePersonTimeDto.STATE_FINISH
        });
        
        // 批量查询社区空间人员时间信息
        List<CommunitySpacePersonTimeDto> communitySpacePersonTimeDtos
                = communitySpacePersonTimeV1InnerServiceSMOImpl.queryCommunitySpacePersonTimes(communitySpaceOpenTimeDto);

        // 为每个社区空间人员设置对应的时间信息
        List<CommunitySpacePersonTimeDto> tmpCommunitySpaceOpenTimeDtos = null;
        for (CommunitySpacePersonDto communitySpaceDto : communitySpacePersonDtos) {
            tmpCommunitySpaceOpenTimeDtos = new ArrayList<>();
            // 遍历所有时间记录，找到与当前人员相关的记录
            for (CommunitySpacePersonTimeDto tmpCommunitySpacePersonTimeDto : communitySpacePersonTimeDtos) {
                if (tmpCommunitySpacePersonTimeDto.getCspId().equals(communitySpaceDto.getCspId())) {
                    tmpCommunitySpaceOpenTimeDtos.add(tmpCommunitySpacePersonTimeDto);
                }
            }
            // 设置人员的时间信息列表
            communitySpaceDto.setTimes(tmpCommunitySpaceOpenTimeDtos);
        }
    }
}