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
package com.java110.store.cmd.classes;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IClassesV1InnerServiceSMO;
import com.java110.po.classes.ClassesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.classes.ClassesDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：classes.listClasses
 * 请求路劲：/app/classes.ListClasses
 * add by 吴学文 at 2022-11-01 00:27:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 班级列表查询命令类
 * 负责处理班级信息的查询请求，支持分页查询功能
 * 通过服务编码"classes.listClasses"标识，处理/app/classes.ListClasses路径的请求
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-01
 */
@Java110Cmd(serviceCode = "classes.listClasses")
public class ListClassesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListClassesCmd.class);
    
    /**
     * 班级服务接口，用于调用班级相关的业务逻辑
     */
    @Autowired
    private IClassesV1InnerServiceSMO classesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数的有效性
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理班级列表查询的核心业务逻辑，包括参数转换、数据查询和结果返回
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 从请求头中获取店铺ID，用于数据权限控制
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 将请求JSON对象转换为班级数据传输对象
        ClassesDto classesDto = BeanConvertUtil.covertBean(reqJson, ClassesDto.class);
        // 设置店铺ID到查询条件中
        classesDto.setStoreId(storeId);

        // 查询符合条件的班级总数
        int count = classesV1InnerServiceSMOImpl.queryClassessCount(classesDto);

        List<ClassesDto> classesDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 如果存在数据，则查询班级列表
            classesDtos = classesV1InnerServiceSMOImpl.queryClassess(classesDto);
        } else {
            // 如果没有数据，返回空列表
            classesDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        // 创建结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, classesDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}