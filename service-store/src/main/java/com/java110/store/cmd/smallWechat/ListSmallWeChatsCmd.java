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
package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.WechatFactory;
import com.java110.dto.app.AppDto;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.smallWeChat.ApiSmallWeChatDataVo;
import com.java110.vo.api.smallWeChat.ApiSmallWeChatVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.wechat.SmallWeChatDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：smallWechat.listSmallWechat
 * 请求路劲：/app/smallWechat.ListSmallWechat
 * add by 吴学文 at 2022-05-25 10:46:07 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 小程序微信配置查询命令类
 * 负责处理查询小程序微信配置列表的请求，包括参数验证、数据查询和结果返回
 * 服务编码：smallWeChat.listSmallWeChats
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-05-25
 */
@Java110Cmd(serviceCode = "smallWeChat.listSmallWeChats")
public class ListSmallWeChatsCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListSmallWeChatsCmd.class);
    
    /**
     * 小程序微信配置服务接口
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

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
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理查询小程序微信配置列表的核心业务逻辑
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 从请求头中获取应用ID
        String appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        if (StringUtil.isEmpty(appId)) {
            // 如果第一次获取失败，再次尝试获取应用ID
            appId = cmdDataFlowContext.getReqHeaders().get("app-id");
        }
        
        // 将请求JSON转换为SmallWeChatDto对象
        SmallWeChatDto smallWeChatDto = BeanConvertUtil.covertBean(reqJson, SmallWeChatDto.class);
        // 设置对象类型为小区类型
        smallWeChatDto.setObjType(SmallWeChatDto.OBJ_TYPE_COMMUNITY);
        
        // 如果请求中包含小区ID参数，则设置到查询条件中
        if(StringUtil.jsonHasKayAndValue(reqJson,"communityId")) {
            smallWeChatDto.setObjId(reqJson.getString("communityId"));
        }
        
        // 查询小程序微信配置总数
        int count = smallWechatV1InnerServiceSMOImpl.querySmallWechatsCount(smallWeChatDto);
        List<ApiSmallWeChatDataVo> smallWeChats = null;
        
        // 如果查询到数据，进行数据转换
        if (count > 0) {
            // 将查询结果转换为前端需要的VO对象列表
            smallWeChats = BeanConvertUtil.covertBeanList(smallWechatV1InnerServiceSMOImpl.querySmallWechats(smallWeChatDto), ApiSmallWeChatDataVo.class);
            // 注释掉的安全刷新方法，可根据需要启用
            //freshSecure(smallWeChats, appId);
        } else {
            // 如果没有查询到数据，返回空列表
            smallWeChats = new ArrayList<>();
        }
        
        // 构建返回结果VO对象
        ApiSmallWeChatVo apiSmallWeChatVo = new ApiSmallWeChatVo();
        // 设置总记录数
        apiSmallWeChatVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，向上取整
        apiSmallWeChatVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置小程序微信配置数据列表
        apiSmallWeChatVo.setSmallWeChats(smallWeChats);
        
        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiSmallWeChatVo), HttpStatus.OK);
        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

}