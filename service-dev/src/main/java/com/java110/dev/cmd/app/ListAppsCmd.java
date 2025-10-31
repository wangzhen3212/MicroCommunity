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
 * 应用列表查询命令类
 * 
 * 功能：处理应用列表查询请求，支持分页查询应用信息
 * 服务编码：app.listApps
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "app.listApps")
public class ListAppsCmd extends Cmd {

    /**
     * 应用服务接口，用于查询应用相关信息
     */
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 主要验证分页参数的正确性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理应用列表查询业务逻辑：
     * 1. 转换请求参数为AppDto对象
     * 2. 查询应用总数
     * 3. 根据分页参数查询应用列表
     * 4. 构建返回结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON参数转换为AppDto对象，用于查询条件
        AppDto appDto = BeanConvertUtil.covertBean(reqJson, AppDto.class);

        // 查询符合条件的应用总数
        int count = appInnerServiceSMOImpl.queryAppsCount(appDto);

        // 应用数据列表
        List<ApiAppDataVo> apps = null;

        // 如果有数据，则查询应用列表；否则返回空列表
        if (count > 0) {
            // 查询应用列表并转换为ApiAppDataVo对象列表
            apps = BeanConvertUtil.covertBeanList(appInnerServiceSMOImpl.queryApps(appDto), ApiAppDataVo.class);
        } else {
            // 没有数据时返回空列表
            apps = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiAppVo apiAppVo = new ApiAppVo();
        // 设置总记录数
        apiAppVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，向上取整
        apiAppVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置应用数据列表
        apiAppVo.setApps(apps);

        // 构建HTTP响应实体，将结果转换为JSON字符串返回
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
                JSONObject.toJSONString(apiAppVo), 
                HttpStatus.OK
        );

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}