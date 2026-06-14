package com.java110.dev.cmd.app;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.app.AppDto;
import com.java110.intf.community.IAppInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.app.ApiAppDataVo;
import com.java110.vo.api.app.ApiAppVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询公共应用列表命令类
 * 服务编码：app.listPublicApps
 * 功能：查询公共可用的应用列表，并清理敏感信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "app.listPublicApps")
public class ListPublicAppsCmd extends Cmd {

    /**
     * 应用内部服务接口
     * 用于查询应用相关数据
     */
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用父类分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 查询应用列表并返回处理结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为AppDto对象
        AppDto appDto = BeanConvertUtil.covertBean(reqJson, AppDto.class);

        // 查询应用总数
        int count = appInnerServiceSMOImpl.queryAppsCount(appDto);

        // 应用数据列表
        List<ApiAppDataVo> apps = null;

        // 根据查询结果数量处理数据
        if (count > 0) {
            // 查询应用列表并转换为VO对象
            apps = BeanConvertUtil.covertBeanList(appInnerServiceSMOImpl.queryApps(appDto), ApiAppDataVo.class);
        } else {
            // 无数据时返回空列表
            apps = new ArrayList<>();
        }

        // 清理应用中的敏感信息
        cleanApps(apps);

        // 构建返回VO对象
        ApiAppVo apiAppVo = new ApiAppVo();
        apiAppVo.setTotal(count); // 设置总记录数
        apiAppVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiAppVo.setApps(apps); // 设置应用数据列表

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiAppVo), HttpStatus.OK);

        // 设置响应结果到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 清理应用中的敏感信息
     * 包括安全码、IP黑白名单等敏感字段
     * 
     * @param apps 应用数据列表
     */
    private void cleanApps(List<ApiAppDataVo> apps) {
        // 空值检查
        if(apps == null || apps.size() < 1){
            return ;
        }

        // 遍历清理每个应用的敏感信息
        for(ApiAppDataVo app: apps){
            app.setSecurityCode(""); // 清空安全码
            app.setBlackListIp("");  // 清空黑名单IP
            app.setWhileListIp("");  // 清空白名单IP
            app.setRemark("");       // 清空备注信息
        }
    }
}