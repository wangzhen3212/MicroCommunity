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
package com.java110.community.cmd.activities;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.activities.ActivitiesDto;
import com.java110.intf.community.IActivitiesInnerServiceSMO;
import com.java110.intf.community.IActivitiesV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.activities.ApiActivitiesDataVo;
import com.java110.vo.api.activities.ApiActivitiesVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：activities.listActivities
 * 请求路劲：/app/activities.ListActivities
 * add by 吴学文 at 2022-06-19 10:49:17 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 活动列表查询命令类
 * 
 * 该类负责处理活动列表查询请求，根据查询条件返回活动信息列表
 * 支持分页查询和H5客户端的特殊时间条件处理
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-06-19
 */
@Java110Cmd(serviceCode = "activities.listActivitiess")
public class ListActivitiessCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListActivitiessCmd.class);
    
    /**
     * 活动V1版本内部服务接口
     */
    @Autowired
    private IActivitiesV1InnerServiceSMO activitiesV1InnerServiceSMOImpl;
    
    /**
     * 活动内部服务接口
     */
    @Autowired
    private IActivitiesInnerServiceSMO activitiesInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，确保必要的参数存在且格式正确
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区信息");
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 
     * 处理活动列表查询业务逻辑，包括参数转换、条件处理、数据查询和响应构建
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为活动数据传输对象
        ActivitiesDto activitiesDto = BeanConvertUtil.covertBean(reqJson, ActivitiesDto.class);
        
        // 如果是H5客户端请求，设置当前时间为查询条件
        if (!StringUtil.isEmpty("clientType") && "H5".equals(reqJson.get("clientType"))) {
            Date day = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 设置开始时间为当前时间
            activitiesDto.setStartTime(df.format(day));
            // 设置结束时间为当前时间
            activitiesDto.setEndTime(df.format(day));
        }

        // 查询活动总数
        int count = activitiesInnerServiceSMOImpl.queryActivitiessCount(activitiesDto);

        List<ApiActivitiesDataVo> activitiess = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询活动列表并转换为视图对象
            activitiess = BeanConvertUtil.covertBeanList(activitiesInnerServiceSMOImpl.queryActivitiess(activitiesDto), ApiActivitiesDataVo.class);
        } else {
            // 没有查询到数据时返回空列表
            activitiess = new ArrayList<>();
        }

        // 构建API响应对象
        ApiActivitiesVo apiActivitiesVo = new ApiActivitiesVo();

        // 设置总记录数
        apiActivitiesVo.setTotal(count);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiActivitiesVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置活动数据列表
        apiActivitiesVo.setActivitiess(activitiess);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiActivitiesVo), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}