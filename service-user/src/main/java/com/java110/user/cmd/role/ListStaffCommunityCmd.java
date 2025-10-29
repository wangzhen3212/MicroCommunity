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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.staffCommunity.StaffCommunityDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：staffCommunity.listStaffCommunity
 * 请求路劲：/app/staffCommunity.ListStaffCommunity
 * add by 吴学文 at 2025-03-18 14:06:45 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 员工社区查询命令类
 * 负责处理员工社区信息的查询请求，包括分页查询和数据统计
 * 实现Cmd接口，处理具体的业务逻辑
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2025-03-18
 */
@Java110Cmd(serviceCode = "role.listStaffCommunity")
public class ListStaffCommunityCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListStaffCommunityCmd.class);
    
    /**
     * 员工社区服务接口，用于查询员工社区相关信息
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页信息和属性验证
     *
     * @param event CmdEvent对象，包含命令事件信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息参数
        super.validatePageInfo(reqJson);
        // 验证属性参数
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 处理员工社区查询业务逻辑，包括数据查询、分页处理和响应构建
     *
     * @param event CmdEvent对象，包含命令事件信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取店铺ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);
        
        // 将请求JSON转换为StaffCommunityDto对象
        StaffCommunityDto staffCommunityDto = BeanConvertUtil.covertBean(reqJson, StaffCommunityDto.class);
        // 设置店铺ID到查询条件中
        staffCommunityDto.setStoreId(storeId);
        
        // 查询符合条件的员工社区记录总数
        int count = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitysCount(staffCommunityDto);

        List<StaffCommunityDto> staffCommunityDtos = null;

        if (count > 0) {
            // 如果有数据，则查询具体的员工社区列表
            staffCommunityDtos = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunitys(staffCommunityDto);
        } else {
            // 如果没有数据，返回空列表
            staffCommunityDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, staffCommunityDtos);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}