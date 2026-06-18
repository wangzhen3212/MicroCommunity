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
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.resource.ResourceStoreUseRecordDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IResourceStoreUseRecordInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 类表述：查询
 * 服务编码：resourceStoreUseRecord.listResourceStoreUseRecord
 * 请求路劲：/app/resourceStoreUseRecord.ListResourceStoreUseRecord
 * add by 吴学文 at 2022-08-08 14:39:58 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 管理端资源库使用记录查询命令类
 * 负责处理管理端查询资源库使用记录的请求，包括分页查询和数据权限验证
 * 服务编码：resourceStore.listAdminStoreUseRecords
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-08
 */
@Java110Cmd(serviceCode = "resourceStore.listAdminStoreUseRecords")
public class ListAdminStoreUseRecordsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminStoreUseRecordsCmd.class);

    /**
     * 资源库使用记录内部服务接口
     */
    @Autowired
    private IResourceStoreUseRecordInnerServiceSMO resourceStoreUseRecordInnerServiceSMOImpl;

    /**
     * 菜单内部服务接口
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数和权限属性
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求上下文信息
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证权限属性
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 命令执行方法
     * 处理资源库使用记录的查询请求，包括数据查询、分页处理和结果返回
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象，用于设置响应结果
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为资源库使用记录DTO对象
        ResourceStoreUseRecordDto resourceStoreUseRecordDto = BeanConvertUtil.covertBean(reqJson, ResourceStoreUseRecordDto.class);

        // 查询物品所有使用记录权限

        // 查询符合条件的记录总数
        int count = resourceStoreUseRecordInnerServiceSMOImpl.queryResourceStoreUseRecordsCount(resourceStoreUseRecordDto);

        List<ResourceStoreUseRecordDto> resourceStoreUseRecordDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询记录列表
            resourceStoreUseRecordDtos = resourceStoreUseRecordInnerServiceSMOImpl.queryResourceStoreUseRecords(resourceStoreUseRecordDto);
        } else {
            // 如果没有记录，返回空列表
            resourceStoreUseRecordDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, resourceStoreUseRecordDtos);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }
}