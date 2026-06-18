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
package com.java110.store.cmd.resourceSupplier;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IResourceSupplierV1InnerServiceSMO;
import com.java110.po.resource.ResourceSupplierPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：resourceSupplier.saveResourceSupplier
 * 请求路劲：/app/resourceSupplier.SaveResourceSupplier
 * add by 吴学文 at 2022-08-07 16:56:05 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 资源供应商保存命令类
 * 
 * 该类负责处理资源供应商信息的保存操作，包括参数验证和数据持久化
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-07
 */
@Java110Cmd(serviceCode = "resourceSupplier.saveResourceSupplier")
public class SaveResourceSupplierCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveResourceSupplierCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 资源供应商服务接口
     */
    @Autowired
    private IResourceSupplierV1InnerServiceSMO resourceSupplierV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查和联系方式格式验证
     * 同时设置默认值处理缺失参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "supplierName", "请求报文中未包含supplierName");
        Assert.hasKeyAndValue(reqJson, "address", "请求报文中未包含address");
        Assert.hasKeyAndValue(reqJson, "tel", "请求报文中未包含tel");
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
        
        // 定义手机号和座机号的正则表达式
        String regex = "1[3-9]\\d{9}"; // 手机号正则：1开头，第二位3-9，后面9位数字
        String regex2 = "0\\d{2,3}-?[1-9]\\d{4,9}"; // 座机号正则：0开头，区号2-3位，可选横杠，号码5-10位
        String tel = reqJson.getString("tel"); // 获取联系方式
        
        // 验证联系方式格式
        if (!tel.matches(regex) && !tel.matches(regex2)) {
            throw new IllegalArgumentException("供应商联系方式格式不对！");
        }
        
        // 处理storeId缺失情况，从请求头中获取
        if (!reqJson.containsKey("storeId")) {
            String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
            reqJson.put("storeId", storeId);
        }
        
        // 设置默认用户ID
        if (!reqJson.containsKey("userId")) {
            reqJson.put("userId", "-1");
        }
        
        // 设置默认用户名
        if (!reqJson.containsKey("userName")) {
            reqJson.put("userName", "未知");
        }
    }

    /**
     * 执行保存资源供应商命令
     * 
     * 将验证通过的请求参数转换为实体对象，生成唯一ID并保存到数据库
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 设置创建用户信息
        reqJson.put("createUserId", reqJson.getString("userId"));
        reqJson.put("createUserName", reqJson.getString("userName"));
        
        // 将JSON对象转换为资源供应商实体对象
        ResourceSupplierPo resourceSupplierPo = BeanConvertUtil.covertBean(reqJson, ResourceSupplierPo.class);
        
        // 生成资源供应商ID
        resourceSupplierPo.setRsId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存数据
        int flag = resourceSupplierV1InnerServiceSMOImpl.saveResourceSupplier(resourceSupplierPo);
        
        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}