package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.wechat.SmallWechatAttrDto;
import com.java110.intf.store.ISmallWechatAttrInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 小程序微信属性列表查询命令类
 * 
 * 该类负责处理查询小程序微信属性列表的请求，包括参数验证和业务逻辑处理
 * 通过服务码 smallWechat.listSmallWechatAttrs 标识对应的业务服务
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "smallWechat.listSmallWechatAttrs")
public class ListSmallWechatAttrsCmd extends Cmd {

    /**
     * 小程序微信属性内部服务接口
     * 用于调用小程序微信属性的相关数据操作
     */
    @Autowired
    private ISmallWechatAttrInnerServiceSMO smallWechatAttrInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保后续业务逻辑能够正常执行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含wechatId字段
        Assert.hasKeyAndValue(reqJson, "wechatId", "微信ID");
        // 分页信息验证暂时注释，可根据需要开启
        // super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询小程序微信属性列表的核心业务逻辑，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务逻辑执行过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为SmallWechatAttrDto数据传输对象
        SmallWechatAttrDto smallWechatAttrDto = BeanConvertUtil.covertBean(reqJson, SmallWechatAttrDto.class);

        // 查询满足条件的小程序微信属性记录总数
        int count = smallWechatAttrInnerServiceSMOImpl.querySmallWechatAttrsCount(smallWechatAttrDto);

        // 定义小程序微信属性列表
        List<SmallWechatAttrDto> smallWechatAttrDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 如果存在记录，则查询具体的小程序微信属性列表
            smallWechatAttrDtos = smallWechatAttrInnerServiceSMOImpl.querySmallWechatAttrs(smallWechatAttrDto);
        } else {
            // 如果没有记录，返回空列表避免空指针异常
            smallWechatAttrDtos = new ArrayList<>();
        }

        // 构建成功响应的结果对象
        ResultVo resultVo = new ResultVo(ResultVo.CODE_OK, ResultVo.MSG_OK, smallWechatAttrDtos);

        // 创建HTTP响应实体，包含结果对象的字符串表示和成功状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}