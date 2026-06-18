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
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.store.IResourceStoreUseRecordInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.resource.ResourceStoreUseRecordDto;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询物品使用记录的命令类
 * 服务编码：resourceStoreUseRecord.listResourceStoreUseRecord
 * 请求路劲：/app/resourceStoreUseRecord.ListResourceStoreUseRecord
 * add by 吴学文 at 2022-08-08 14:39:58 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.listResourceStoreUseRecords")
public class ListResourceStoreUseRecordsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListResourceStoreUseRecordsCmd.class);

    /**
     * 物品使用记录内部服务接口
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
     * 验证请求参数的有效性，包括分页信息和属性验证
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行命令方法
     * 处理查询物品使用记录的请求，包括权限验证和数据查询
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为ResourceStoreUseRecordDto对象
        ResourceStoreUseRecordDto resourceStoreUseRecordDto = BeanConvertUtil.covertBean(reqJson, ResourceStoreUseRecordDto.class);

        // 检查用户是否拥有查看所有物品使用记录的权限
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        basePrivilegeDto.setResource("/allResourceStoreUseRecord"); // 设置资源路径
        basePrivilegeDto.setUserId(reqJson.getString("userId")); // 设置用户ID
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 根据权限设置查询条件
        if (ListUtil.isNull(privileges)) {
            // 如果没有权限，只能查询当前用户自己的使用记录
            resourceStoreUseRecordDto.setUserId(reqJson.getString("userId"));
            resourceStoreUseRecordDto.setUserName(reqJson.getString("userName"));
        } else {
            // 如果有权限，可以查询所有用户的使用记录
            resourceStoreUseRecordDto.setUserId("");
            resourceStoreUseRecordDto.setUserName("");
        }

        // 查询符合条件的记录总数
        int count = resourceStoreUseRecordInnerServiceSMOImpl.queryResourceStoreUseRecordsCount(resourceStoreUseRecordDto);

        List<ResourceStoreUseRecordDto> resourceStoreUseRecordDtos = null;

        if (count > 0) {
            // 如果有记录，查询具体的记录列表
            resourceStoreUseRecordDtos = resourceStoreUseRecordInnerServiceSMOImpl.queryResourceStoreUseRecords(resourceStoreUseRecordDto);
        } else {
            // 如果没有记录，返回空列表
            resourceStoreUseRecordDtos = new ArrayList<>();
        }

        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, resourceStoreUseRecordDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}