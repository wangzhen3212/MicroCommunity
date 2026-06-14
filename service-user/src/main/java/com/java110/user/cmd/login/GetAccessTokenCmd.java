package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 单点登录获取访问令牌命令类
 * 
 * 该类主要用于处理第三方系统单点登录到物业系统的请求，通过验证用户名和密码生成临时的accessToken，
 * 第三方系统可以使用该token实现自动登录到物业系统的功能
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "单点登录获取accessToken",
        description = "主要用于其他系统单点登录到物业系统,<br/>" +
                "1、三方系统通过物业员工的 手机号 和 员工的密码 通过此接口获取accessToken,注意默认有效期为5分钟，<br/>" +
                "2、三方系统通过302 跳转的方式调转到物业系统 http://wuye.xx.com/sso.html?hcAccessToken={accessToken}&targetUrl=您要跳转的页面<br/> " +
                "3、物业系统即可完成自登陆功能</br>"+
                "注意：系统默认情况下是不启用单点登录功能的，请联系开发，配置中心中启用（SSO_SWITCH 的值改为ON）</br>",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/login.getAccessToken",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "login.getAccessToken",
        seq = 17
)

@Java110ParamsDoc(
        headers = {
                @Java110HeaderDoc(name = "APP-ID", defaultValue = "通过dev账户分配应用", description = "应用APP-ID"),
                @Java110HeaderDoc(name = "TRANSACTION-ID", defaultValue = "uuid", description = "交易流水号"),
                @Java110HeaderDoc(name = "REQ-TIME", defaultValue = "20220917120915", description = "请求时间 YYYYMMDDhhmmss"),
                @Java110HeaderDoc(name = "JAVA110-LANG", defaultValue = "zh-cn", description = "语言中文"),
                @Java110HeaderDoc(name = "USER-ID", defaultValue = "-1", description = "调用用户ID 一般写-1"),
        },
        params = {
                @Java110ParamDoc(name = "tel", length = 30, remark = "用户名，物业系统分配"),
                @Java110ParamDoc(name = "passwd", length = 30, remark = "密码，物业系统分配"),        })

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "accessToken", type = "String", remark = "临时票据"),
                @Java110ParamDoc(parentNodeName = "data", name = "expiresIn", type = "String", remark = "票据过期时间单位秒"),
        }
)

@Java110ExampleDoc(
        reqBody = "{'username':'wuxw','passwd':'admin'}",
        resBody = "{'code':0,'msg':'成功','data':{'accessToken':'ssss333333','expiresIn':'300'}}"
)

@Java110Cmd(serviceCode = "login.getAccessToken")
public class GetAccessTokenCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(GetAccessTokenCmd.class);

    /**
     * 用户登录服务接口
     */
    @Autowired
    private IUserLoginInnerServiceSMO userLoginInnerServiceSMOImpl;

    /**
     * 商户服务接口
     */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，并检查系统是否开启了单点登录功能
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败或系统未开启单点登录时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含tel和passwd字段
        Assert.jsonObjectHaveKey(reqJson, "tel", "用户登录，未包含username节点，请检查" + reqJson);
        Assert.jsonObjectHaveKey(reqJson, "passwd", "用户登录，未包含passwd节点，请检查" + reqJson);

        // 从配置中心获取单点登录功能开关状态
        String ssoSwitch = MappingCache.getValue(MappingConstant.DOMAIN_SYSTEM_SWITCH, "SSO_SWITCH");

        // 检查系统是否开启了单点登录功能，如果未开启则抛出异常
        if (!"ON".equals(ssoSwitch)) {
            throw new CmdException("系统未开启单点登录功能请联系开发者");
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理单点登录请求，验证用户凭据，生成访问令牌并返回给调用方
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当用户验证失败或商户状态异常时抛出
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 根据AppId查询是否有登录的服务，查询登录地址调用
        UserDto userDto = new UserDto();
        // 设置用户手机号
        userDto.setTel(reqJson.getString("tel"));
        // 设置用户密码
        userDto.setPassword(reqJson.getString("passwd"));
        // 设置用户级别，只允许管理员和员工登录
        userDto.setLevelCds(new String[]{UserDto.LEVEL_CD_ADMIN, UserDto.LEVEL_CD_STAFF});
        // 调用用户服务查询用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 验证用户是否存在
        if (userDtos == null || userDtos.size() < 1) {
            throw new CmdException("用户名密码错误");
        }

        // 检查商户状态，确保用户所属商户可以正常登录
        StoreUserDto storeUserDto = new StoreUserDto();
        // 设置用户ID
        storeUserDto.setUserId(userDtos.get(0).getUserId());
        // 查询商户用户信息
        List<StoreUserDto> storeUserDtos = storeInnerServiceSMOImpl.getStoreUserInfo(storeUserDto);

        // 如果用户有关联商户，检查商户状态
        if (storeUserDtos != null && storeUserDtos.size() > 0) {
            String state = storeUserDtos.get(0).getState();
            // 如果商户状态为限制登录状态，抛出异常
            if ("48002".equals(state)) {
                throw new CmdException("当前商户限制登录，请联系管理员");
            }
        }
        
        // 将用户信息转换为JSON字符串
        String userInfo = JSONObject.toJSONString(userDtos.get(0));
        // 生成唯一的访问令牌
        String accessToken  = GenerateCodeFactory.getUUID();
        // 将用户信息缓存到Redis中，key为accessToken_sso，设置默认过期时间
        CommonCache.setValue(accessToken + "_sso", userInfo, CommonCache.defaultExpireTime);

        // 构建响应数据
        JSONObject data = new JSONObject();
        // 设置访问令牌
        data.put("accessToken", accessToken);
        // 设置令牌过期时间
        data.put("expiresIn", CommonCache.defaultExpireTime);
        // 设置响应实体
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}