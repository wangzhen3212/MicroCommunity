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
package com.java110.user.cmd.dataPrivilegeStaff;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IDataPrivilegeStaffV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
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
 * 服务编码：dataPrivilegeStaff.listDataPrivilegeStaff
 * 请求路劲：/app/dataPrivilegeStaff.ListDataPrivilegeStaff
 * add by 吴学文 at 2022-09-28 16:48:23 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 数据权限员工查询命令类
 * 负责查询不在指定数据权限中的员工列表
 * 服务编码：dataPrivilegeStaff.listStaffNotInDataPrivilege
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-09-28
 */
@Java110Cmd(serviceCode = "dataPrivilegeStaff.listStaffNotInDataPrivilege")
public class ListStaffNotInDataPrivilegeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListStaffNotInDataPrivilegeCmd.class);
    
    /**
     * 数据权限员工服务接口
     */
    @Autowired
    private IDataPrivilegeStaffV1InnerServiceSMO dataPrivilegeStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);

        // 验证请求中必须包含dpId参数
        Assert.hasKeyAndValue(reqJson,"dpId","未包含数据权限");
    }

    /**
     * 命令执行方法
     * 查询不在指定数据权限中的员工列表
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为数据权限员工DTO对象
        DataPrivilegeStaffDto dataPrivilegeStaffDto = BeanConvertUtil.covertBean(reqJson, DataPrivilegeStaffDto.class);

        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        // 设置店铺ID到DTO对象中
        dataPrivilegeStaffDto.setStoreId(storeId);

        // 查询不在数据权限中的员工总数
        int count = dataPrivilegeStaffV1InnerServiceSMOImpl.queryStaffsNotInDataPrivilegeCount(dataPrivilegeStaffDto);

        List<UserDto> userDtos = null;

        // 根据查询结果数量决定如何处理
        if (count > 0) {
            // 如果存在记录，则查询具体的员工列表
            userDtos = dataPrivilegeStaffV1InnerServiceSMOImpl.queryStaffsNotInDataPrivilege(dataPrivilegeStaffDto);
        } else {
            // 如果没有记录，返回空列表
            userDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果VO对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, userDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}