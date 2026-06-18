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
package com.java110.user.cmd.role;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.store.StoreDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.privilege.RoleCommunityPo;
import com.java110.po.staffCommunity.StaffCommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存员工小区关联关系命令类
 * 服务编码：staffCommunity.saveStaffCommunity
 * 请求路劲：/app/staffCommunity.SaveStaffCommunity
 * 功能描述：该命令类用于处理员工与小区关联关系的保存操作，包括参数验证和业务逻辑处理
 * add by 吴学文 at 2025-03-18 14:06:45 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "role.saveStaffCommunity")
public class SaveStaffCommunityCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveStaffCommunityCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 员工小区关联服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 商户服务接口
     */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，确保必要的参数存在且格式正确
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含staffId参数
        Assert.hasKeyAndValue(reqJson, "staffId", "请求报文中未包含staffId");
        // 验证请求报文中必须包含communitys参数
        Assert.hasKey(reqJson, "communitys", "未包含小区");
        
        // 获取小区信息数组
        JSONArray communitys = reqJson.getJSONArray("communitys");
        // 验证小区信息数组不能为空
        if (ListUtil.isNull(communitys)) {
            throw new CmdException("未选择小区信息");
        }

    }

    /**
     * 执行命令方法
     * 功能描述：处理员工小区关联关系的保存业务逻辑，包括数据验证、关联关系创建等
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 从请求参数中获取小区信息数组
        JSONArray communitys = reqJson.getJSONArray("communitys");

        // 从上下文中获取当前商户ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);

        // 构建用户查询条件，验证员工是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("staffId"));
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        // 验证员工信息必须存在且唯一
        Assert.listOnlyOne(userDtos, "员工不存在");

        // 构建商户查询条件，验证商户是否存在
        StoreDto storeDto = new StoreDto();
        storeDto.setStoreId(storeId);
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);
        // 验证商户信息必须存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 员工小区关联关系对象
        StaffCommunityPo staffCommunityPo = null;
        // 遍历所有小区信息，为每个小区创建员工关联关系
        for (int communityIndex = 0; communityIndex < communitys.size(); communityIndex++) {
            staffCommunityPo = new StaffCommunityPo();
            // 设置员工姓名
            staffCommunityPo.setStaffName(userDtos.get(0).getName());
            // 设置小区ID
            staffCommunityPo.setCommunityId(communitys.getJSONObject(communityIndex).getString("communityId"));
            // 设置小区名称
            staffCommunityPo.setCommunityName(communitys.getJSONObject(communityIndex).getString("communityName"));
            // 设置商户名称
            staffCommunityPo.setStoreName(storeDtos.get(0).getName());
            // 设置商户ID
            staffCommunityPo.setStoreId(storeId);
            // 生成关联关系ID
            staffCommunityPo.setScId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置员工ID
            staffCommunityPo.setStaffId(reqJson.getString("staffId"));

            // 保存员工小区关联关系
            int flag = staffCommunityV1InnerServiceSMOImpl.saveStaffCommunity(staffCommunityPo);

            // 验证保存操作是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}