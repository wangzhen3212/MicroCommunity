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
package com.java110.community.cmd.propertyRightRegistration;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.PropertyRightRegistrationDetailDto;
import com.java110.intf.community.IPropertyRightRegistrationDetailV1InnerServiceSMO;
import com.java110.intf.community.IPropertyRightRegistrationV1InnerServiceSMO;
import com.java110.po.room.PropertyRightRegistrationPo;
import com.java110.po.room.PropertyRightRegistrationDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

import java.util.List;

/**
 * 类表述：删除产权登记信息命令类
 * 服务编码：propertyRightRegistration.deletePropertyRightRegistration
 * 请求路劲：/app/propertyRightRegistration.DeletePropertyRightRegistration
 * add by 吴学文 at 2021-10-09 10:34:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理删除产权登记信息的业务逻辑，包括删除产权登记主表和相关的详情记录
 * 主要职责：
 * 1. 验证删除请求参数的完整性
 * 2. 删除产权登记主表记录
 * 3. 删除关联的产权登记详情记录
 * 4. 返回操作结果
 */
@Java110Cmd(serviceCode = "propertyRightRegistration.deletePropertyRightRegistration")
public class DeletePropertyRightRegistrationCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeletePropertyRightRegistrationCmd.class);

    /**
     * 产权登记服务接口
     */
    @Autowired
    private IPropertyRightRegistrationV1InnerServiceSMO propertyRightRegistrationV1InnerServiceSMOImpl;

    /**
     * 产权登记详情服务接口
     */
    @Autowired
    private IPropertyRightRegistrationDetailV1InnerServiceSMO propertyRightRegistrationDetailV1InnerServiceSMOImpl;

    /**
     * 验证删除产权登记信息的请求参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含prrId（产权登记ID）
        Assert.hasKeyAndValue(reqJson, "prrId", "prrId不能为空");
        // 验证请求参数中必须包含communityId（小区ID）
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除产权登记信息的业务逻辑
     * 该方法使用事务注解确保数据一致性，删除操作要么全部成功，要么全部回滚
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为产权登记PO对象
        PropertyRightRegistrationPo propertyRightRegistrationPo = BeanConvertUtil.covertBean(reqJson, PropertyRightRegistrationPo.class);
        
        // 删除产权登记主表记录
        int flag = propertyRightRegistrationV1InnerServiceSMOImpl.deletePropertyRightRegistration(propertyRightRegistrationPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
        
        // 创建产权登记详情查询条件对象
        PropertyRightRegistrationDetailDto propertyRightRegistrationDetailDto = new PropertyRightRegistrationDetailDto();
        // 设置查询条件：根据产权登记ID查询相关详情记录
        propertyRightRegistrationDetailDto.setPrrId(propertyRightRegistrationPo.getPrrId());
        
        // 查询产权登记详情记录列表
        List<PropertyRightRegistrationDetailDto> propertyRightRegistrationDetailDtos = propertyRightRegistrationDetailV1InnerServiceSMOImpl.queryPropertyRightRegistrationDetails(propertyRightRegistrationDetailDto);
        
        // 检查是否存在关联的详情记录，如果存在则逐个删除
        if (propertyRightRegistrationDetailDtos != null && propertyRightRegistrationDetailDtos.size() > 0) {
            // 遍历所有详情记录
            for (PropertyRightRegistrationDetailDto propertyRightRegistrationDetail : propertyRightRegistrationDetailDtos) {
                // 创建详情删除对象
                PropertyRightRegistrationDetailPo propertyRightRegistrationDetailPo = new PropertyRightRegistrationDetailPo();
                // 设置要删除的详情记录ID
                propertyRightRegistrationDetailPo.setPrrdId(propertyRightRegistrationDetail.getPrrdId());
                // 删除产权登记详情记录
                propertyRightRegistrationDetailV1InnerServiceSMOImpl.deletePropertyRightRegistrationDetail(propertyRightRegistrationDetailPo);
            }
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}