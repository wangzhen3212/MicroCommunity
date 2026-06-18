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
package com.java110.fee.cmd.ownerCarNew;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.intf.fee.IOwnerCarNewV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：ownerCarNew.listOwnerCarNew
 * 请求路劲：/app/ownerCarNew.ListOwnerCarNew
 * add by 吴学文 at 2021-12-06 22:09:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 业主车辆信息查询命令类
 * 
 * 该类负责处理业主车辆信息的列表查询请求，包括分页查询和数据返回功能
 * 通过服务编码"ownerCarNew.listOwnerCarNew"与前端请求进行映射
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-12-06
 */
@Java110Cmd(serviceCode = "ownerCarNew.listOwnerCarNew")
public class ListOwnerCarNewCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListOwnerCarNewCmd.class);
    
    /**
     * 业主车辆信息服务接口，用于查询业主车辆相关信息
     */
    @Autowired
    private IOwnerCarNewV1InnerServiceSMO ownerCarNewV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 主要验证请求中的分页参数是否合法，包括页码和每页记录数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证页码和每页记录数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行查询业主车辆信息命令
     * 
     * 该方法负责处理业主车辆信息的查询业务逻辑，包括：
     * 1. 参数转换
     * 2. 查询总记录数
     * 3. 查询数据列表
     * 4. 构建响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为OwnerCarDto对象，用于查询条件
        OwnerCarDto ownerCarNewDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);

        // 查询符合条件的业主车辆信息总记录数
        int count = ownerCarNewV1InnerServiceSMOImpl.queryOwnerCarNewsCount(ownerCarNewDto);

        // 定义业主车辆信息列表
        List<OwnerCarDto> ownerCarNewDtos = null;

        // 根据查询结果数量判断是否需要进行数据查询
        if (count > 0) {
            // 如果存在数据，则查询业主车辆信息列表
            ownerCarNewDtos = ownerCarNewV1InnerServiceSMOImpl.queryOwnerCarNews(ownerCarNewDto);
        } else {
            // 如果没有数据，返回空列表
            ownerCarNewDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，并向上取整
        // 构建包含分页信息和数据列表的结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, ownerCarNewDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}