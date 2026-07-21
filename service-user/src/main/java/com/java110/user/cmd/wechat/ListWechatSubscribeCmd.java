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
package com.java110.user.cmd.wechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.user.StaffAppAuthDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IStaffAppAuthInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.intf.user.IWechatSubscribeV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.wechat.WechatSubscribeDto;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询微信订阅信息列表
 * 服务编码：wechatSubscribe.listWechatSubscribe
 * 请求路劲：/app/wechatSubscribe.ListWechatSubscribe
 * add by 吴学文 at 2022-02-22 16:17:23 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "wechat.listWechatSubscribe")
public class ListWechatSubscribeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListWechatSubscribeCmd.class);
    
    /**
     * 微信订阅信息服务接口
     */
    @Autowired
    private IWechatSubscribeV1InnerServiceSMO wechatSubscribeV1InnerServiceSMOImpl;

    /**
     * 小程序微信配置服务接口
     */
    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    @Autowired
    private IStaffAppAuthInnerServiceSMO staffAppAuthInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
        super.validateProperty(cmdDataFlowContext);
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区编号");
    }

    /**
     * 执行命令方法
     * 查询微信订阅信息列表，包括分页处理和结果返回
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId(reqJson.getString("communityId"));
        smallWeChatDto.setWeChatType(SmallWeChatDto.WECHAT_TYPE_PUBLIC);

        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        if (ListUtil.isNull(smallWeChatDtos)) {
            throw new CmdException("未配置公众号");
        }

        WechatSubscribeDto wechatSubscribeDto = BeanConvertUtil.covertBean(reqJson, WechatSubscribeDto.class);
        wechatSubscribeDto.setUserId("");
        wechatSubscribeDto.setAppId(smallWeChatDtos.get(0).getAppId());

        // 查询微信订阅信息总数（使用原有简单查询，避免联表笛卡尔积问题）
        int count = wechatSubscribeV1InnerServiceSMOImpl.queryWechatSubscribesCount(wechatSubscribeDto);

        List<WechatSubscribeDto> wechatSubscribeDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询微信订阅信息列表（使用原有简单查询）
            wechatSubscribeDtos = wechatSubscribeV1InnerServiceSMOImpl.queryWechatSubscribes(wechatSubscribeDto);
        } else {
            // 如果没有数据，返回空列表
            wechatSubscribeDtos = new ArrayList<>();
        }

        // 通过 openId 反查 owner_app_user 和 staff_app_auth，拿到对应的系统用户ID
        // 这样无论 wechat_subscribe.user_id 是否为空，都能关联到系统用户
        Map<String, OwnerAppUserDto> ownerMap = new HashMap<>();
        Map<String, StaffAppAuthDto> staffMap = new HashMap<>();
        Set<String> allUserIds = new HashSet<>();

        for (WechatSubscribeDto dto : wechatSubscribeDtos) {
            String openId = dto.getOpenId();
            if (StringUtil.isEmpty(openId)) {
                continue;
            }
            logger.debug("关注用户 openId={}, wechat_subscribe.userId={}", openId, dto.getUserId());

            // 查业主绑定（owner_app_user）
            OwnerAppUserDto ownerQuery = new OwnerAppUserDto();
            ownerQuery.setOpenId(openId);
            List<OwnerAppUserDto> owners = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerQuery);
            logger.debug("业主绑定查询 openId={}, 查到{}条", openId, owners == null ? 0 : owners.size());
            if (!ListUtil.isNull(owners) && !StringUtil.isEmpty(owners.get(0).getUserId())) {
                ownerMap.put(openId, owners.get(0));
                allUserIds.add(owners.get(0).getUserId());
                logger.debug("业主绑定命中 openId={}, userId={}", openId, owners.get(0).getUserId());
            }

            // 查员工绑定（staff_app_auth）
            StaffAppAuthDto staffQuery = new StaffAppAuthDto();
            staffQuery.setOpenId(openId);
            List<StaffAppAuthDto> staffs = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffQuery);
            logger.debug("员工绑定查询 openId={}, 查到{}条", openId, staffs == null ? 0 : staffs.size());
            if (!ListUtil.isNull(staffs) && !StringUtil.isEmpty(staffs.get(0).getStaffId())) {
                staffMap.put(openId, staffs.get(0));
                allUserIds.add(staffs.get(0).getStaffId());
                logger.debug("员工绑定命中 openId={}, staffId={}", openId, staffs.get(0).getStaffId());
            }

            // 同时收集 wechat_subscribe.user_id（业务侧手动绑定的系统用户ID）
            if (!StringUtil.isEmpty(dto.getUserId())) {
                allUserIds.add(dto.getUserId());
            }
        }

        logger.debug("allUserIds={}", allUserIds);

        // 批量查询 u_user 获取姓名和手机号
        Map<String, UserDto> userMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            UserDto userDto = new UserDto();
            userDto.setUserIds(allUserIds.toArray(new String[0]));
            List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
            logger.debug("queryUsers返回{}条", userDtos == null ? 0 : userDtos.size());
            if (!ListUtil.isNull(userDtos)) {
                for (UserDto u : userDtos) {
                    userMap.put(u.getUserId(), u);
                }
            }
        }
        logger.debug("userMap.keys={}", userMap.keySet());

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (WechatSubscribeDto subscribeDto : wechatSubscribeDtos) {
            Map<String, Object> item = new HashMap<>();
            item.put("subId", subscribeDto.getSubId());
            item.put("openId", subscribeDto.getOpenId());
            item.put("unionId", subscribeDto.getUnionId());
            // 微信返回的备注与标签ID列表（独立字段，原 nickname/headimgurl 维持不变）
            item.put("remark", subscribeDto.getRemark() == null ? "" : subscribeDto.getRemark());
            item.put("tagidList", subscribeDto.getTagidList() == null ? "" : subscribeDto.getTagidList());
            item.put("createTime", subscribeDto.getCreateTime());

            // 确定有效 userId：优先业主绑定，其次员工绑定，最后 wechat_subscribe.user_id
            String openId = subscribeDto.getOpenId();
            String effectiveUserId = "";
            String ownerUserId = ownerMap.containsKey(openId) ? ownerMap.get(openId).getUserId() : "";
            String staffUserId = staffMap.containsKey(openId) ? staffMap.get(openId).getStaffId() : "";
            String subscribeUserId = subscribeDto.getUserId();

            if (!StringUtil.isEmpty(ownerUserId)) {
                effectiveUserId = ownerUserId;
            } else if (!StringUtil.isEmpty(staffUserId)) {
                effectiveUserId = staffUserId;
            } else if (!StringUtil.isEmpty(subscribeUserId)) {
                effectiveUserId = subscribeUserId;
            }

            if (!StringUtil.isEmpty(effectiveUserId) && userMap.containsKey(effectiveUserId)) {
                UserDto userDto = userMap.get(effectiveUserId);
                item.put("userId", effectiveUserId);
                item.put("phone", userDto.getTel() == null ? "" : userDto.getTel());
                item.put("name", userDto.getName() == null ? "" : userDto.getName());
            } else {
                item.put("userId", effectiveUserId == null ? "" : effectiveUserId);
                item.put("phone", "");
                item.put("name", "");
            }

            // 根据 openId 在 owner_app_user 和 staff_app_auth 表中的存在情况确定身份
            boolean isOwner = ownerMap.containsKey(openId);
            boolean isStaff = staffMap.containsKey(openId);
            if (isOwner && isStaff) {
                item.put("identityType", "业主,员工");
            } else if (isOwner) {
                item.put("identityType", "业主");
            } else if (isStaff) {
                item.put("identityType", "员工");
            } else {
                item.put("identityType", "未绑定");
            }

            resultList.add(item);
        }

        // 构建返回结果对象，包含分页信息
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, resultList);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}
