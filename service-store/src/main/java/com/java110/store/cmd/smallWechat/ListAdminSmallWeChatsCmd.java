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
package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.smallWeChat.ApiSmallWeChatDataVo;
import com.java110.vo.api.smallWeChat.ApiSmallWeChatVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：smallWechat.listSmallWechat
 * 请求路劲：/app/smallWechat.ListSmallWechat
 * add by 吴学文 at 2022-05-25 10:46:07 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 管理员小程序微信配置查询命令类
 * 负责处理管理员查询小程序微信配置列表的请求
 * 支持分页查询，并关联查询小区信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-05-25
 */
@Java110Cmd(serviceCode = "smallWeChat.listAdminSmallWeChats")
public class ListAdminSmallWeChatsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminSmallWeChatsCmd.class);
    
    /**
     * 小程序微信配置服务接口
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /**
     * 小区信息服务接口
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和管理员权限验证
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 处理查询小程序微信配置列表的业务逻辑
     * 包括条件查询、分页处理、小区信息关联查询等
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为SmallWeChatDto对象
        SmallWeChatDto smallWeChatDto = BeanConvertUtil.covertBean(reqJson, SmallWeChatDto.class);
        // 设置对象类型为小区类型
        smallWeChatDto.setObjType(SmallWeChatDto.OBJ_TYPE_COMMUNITY);
        
        // 如果请求中包含小区ID参数，则设置到查询条件中
        if (StringUtil.jsonHasKayAndValue(reqJson, "communityId")) {
            smallWeChatDto.setObjId(reqJson.getString("communityId"));
        }
        //smallWeChatDto.setWeChatType(reqJson.getString("wechatType"));
        
        // 查询符合条件的记录总数
        int count = smallWechatV1InnerServiceSMOImpl.querySmallWechatsCount(smallWeChatDto);
        List<SmallWeChatDto> smallWeChats = null;
        
        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询小程序微信配置列表数据
            smallWeChats = BeanConvertUtil.covertBeanList(smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto), SmallWeChatDto.class);
            //freshSecure(smallWeChats, appId);
        } else {
            // 如果没有查询到数据，返回空列表
            smallWeChats = new ArrayList<>();
        }

        // 刷新小区名称信息
        refreshCommunityName(smallWeChats);

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, smallWeChats);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新小区名称信息
     * 根据小区ID查询小区名称并设置到小程序微信配置对象中
     *
     * @param smallWeChats 小程序微信配置列表
     */
    private void refreshCommunityName(List<SmallWeChatDto> smallWeChats) {
        // 如果列表为空，直接返回
        if(ListUtil.isNull(smallWeChats)){
            return;
        }

        // 收集所有小区ID
        List<String> communityIds = new ArrayList<>();
        for (SmallWeChatDto smallWeChatDto : smallWeChats) {
            // 将对象ID设置为小区ID
            smallWeChatDto.setCommunityId(smallWeChatDto.getObjId());
            communityIds.add(smallWeChatDto.getObjId());
        }

        // 如果小区ID列表为空，直接返回
        if(ListUtil.isNull(communityIds)){
            return ;
        }
        
        // 构建小区查询条件
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        // 查询小区信息
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 如果未查询到小区信息，直接返回
        if(ListUtil.isNull(communityDtos)){
            return;
        }
        
        // 遍历小程序微信配置列表，设置对应的小区名称
        for (SmallWeChatDto smallWeChatDto : smallWeChats) {
            for (CommunityDto tCommunityDto : communityDtos) {
                // 匹配小区ID
                if (!smallWeChatDto.getObjId().equals(tCommunityDto.getCommunityId())) {
                    continue;
                }
                // 设置小区名称
                smallWeChatDto.setCommunityName(tCommunityDto.getName());
            }
        }
    }


}