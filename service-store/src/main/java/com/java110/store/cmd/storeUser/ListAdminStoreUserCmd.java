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
package com.java110.store.cmd.storeUser;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.store.StoreUserDto;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：storeUser.listStoreUser
 * 请求路劲：/app/storeUser.ListStoreUser
 * add by 吴学文 at 2021-10-08 16:05:30 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 管理员店铺用户查询命令类
 * 
 * 该类用于处理管理员查询店铺用户列表的请求，继承自Cmd基类，
 * 通过注解@Java110Cmd指定服务编码为"storeUser.listAdminStoreUser"
 * 主要功能包括参数验证和查询店铺用户信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-08
 */
@Java110Cmd(serviceCode = "storeUser.listAdminStoreUser")
public class ListAdminStoreUserCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListAdminStoreUserCmd.class);
    
    /**
     * 店铺用户服务接口，用于调用店铺用户相关的业务方法
     */
    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括分页参数验证和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求上下文信息
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
        // 调用父类的管理员权限验证方法，验证当前用户是否具有管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责执行查询店铺用户列表的核心业务逻辑，
     * 包括参数转换、数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为StoreUserDto对象
        StoreUserDto storeUserDto = BeanConvertUtil.covertBean(reqJson, StoreUserDto.class);
        // 设置用户ID为请求中的staffId参数
        storeUserDto.setUserId(reqJson.getString("staffId"));
        // 清空店铺ID，查询所有店铺的用户信息
        storeUserDto.setStoreId("");
        
        // 查询符合条件的店铺用户总数
        int count = storeUserV1InnerServiceSMOImpl.queryStoreUsersCount(storeUserDto);

        // 店铺用户数据列表
        List<StoreUserDto> storeUserDtos = null;

        // 根据查询结果数量判断是否进行数据查询
        if (count > 0) {
            // 查询店铺用户详细信息列表
            storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);
        } else {
            // 如果没有查询到数据，返回空列表
            storeUserDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, storeUserDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}