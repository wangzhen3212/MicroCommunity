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
package com.java110.community.cmd.dataPrivilegeUnit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IDataPrivilegeUnitV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.data.DataPrivilegeUnitDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询数据权限单元列表命令类
 * 服务编码：dataPrivilegeUnit.listDataPrivilegeUnit
 * 请求路径：/app/dataPrivilegeUnit.ListDataPrivilegeUnit
 * 功能描述：该命令类用于处理数据权限单元的列表查询请求，支持分页查询功能
 * add by 吴学文 at 2022-09-28 16:32:34 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "dataPrivilegeUnit.listDataPrivilegeUnit")
public class ListDataPrivilegeUnitCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListDataPrivilegeUnitCmd.class);
    
    /**
     * 数据权限单元服务接口
     */
    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

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
     * 功能描述：执行数据权限单元列表查询，包括分页查询和结果返回
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为数据权限单元DTO对象
        DataPrivilegeUnitDto dataPrivilegeUnitDto = BeanConvertUtil.covertBean(reqJson, DataPrivilegeUnitDto.class);

        // 查询数据权限单元总数
        int count = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnitsCount(dataPrivilegeUnitDto);

        // 声明数据权限单元列表
        List<DataPrivilegeUnitDto> dataPrivilegeUnitDtos = null;

        // 根据查询结果数量判断是否查询数据
        if (count > 0) {
            // 查询数据权限单元列表
            dataPrivilegeUnitDtos = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnits(dataPrivilegeUnitDto);
        } else {
            // 如果没有数据，返回空列表
            dataPrivilegeUnitDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, dataPrivilegeUnitDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}