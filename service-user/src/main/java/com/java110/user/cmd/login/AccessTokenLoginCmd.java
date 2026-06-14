package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserLoginDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.po.user.UserLoginPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单点登录AccessToken自登陆命令类
 * 实现通过accessToken进行单点登录的功能，支持从缓存获取用户信息或通过Base64解码token获取用户凭证进行登录
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "单点登录accessToken自登陆（自用）",
        description = "1、通过接口/app/login.getAccessToken 获取到accessToken,<br/>" +
                "2、三方系统通过302 跳转的方式调转到物业系统 http://wuye.xx.com/sso.html?hcAccessToken={accessToken}&targetUrl=您要跳转的页面<br/>" +
                "3、物业系统即可完成自登陆功能</br>"+
                "注意：系统默认情况下是不启用单点登录功能的，请联系开发，配置中心中启用（SSO_SWITCH 的值改为ON）</br>",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/login.accessTokenLogin",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "login.accessTokenLogin",
        seq = 18
)

@Java110ParamsDoc(
        headers = {
                @Java110HeaderDoc(name="APP-ID",defaultValue = "通过dev账户分配应用",description = "应用APP-ID"),
                @Java110HeaderDoc(name="TRANSACTION-ID",defaultValue = "uuid",description = "交易流水号"),
                @Java110HeaderDoc(name="REQ-TIME",defaultValue = "20220917120915",description = "请求时间 YYYYMMDDhhmmss"),
                @Java110HeaderDoc(name="JAVA110-LANG",defaultValue = "zh-cn",description = "语言中文"),
                @Java110HeaderDoc(name="USER-ID",defaultValue = "-1",description = "调用用户ID 一般写-1"),
        },
        params = {
                @Java110ParamDoc(name = "hcAccessToken", length = 30, remark = "token 通过服务端调用 接口方式获取到"),
        })

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "userId", type = "String", remark = "用户ID"),
                @Java110ParamDoc(parentNodeName = "data",name = "token", type = "String", remark = "临时票据"),
        }
)

@Java110ExampleDoc(
        reqBody="{'hcAccessToken':'112sdfsdfsdfsdfsdf'}",
        resBody="{'code':0,'msg':'成功','data':{'userId':'123123','token':'123213'}}"
)

@Java110Cmd(serviceCode = "login.accessTokenLogin")
public class AccessTokenLoginCmd extends Cmd {
    
    /** 日志记录器 */
    private final static Logger logger = LoggerFactory.getLogger(AccessTokenLoginCmd.class);

    /** 用户登录服务接口 */
    @Autowired
    private IUserLoginInnerServiceSMO userLoginInnerServiceSMOImpl;

    /** 商户服务接口 */
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和系统配置状态
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中是否包含hcAccessToken
        Assert.hasKeyAndValue(reqJson, "hcAccessToken", "未包含token信息");

        // 从配置中心获取单点登录开关配置
        String ssoSwitch = MappingCache.getValue(MappingConstant.DOMAIN_SYSTEM_SWITCH, "SSO_SWITCH");

        // 检查系统是否开启了单点登录功能
        if (!"ON".equals(ssoSwitch)) {
            throw new CmdException("系统未开启单点登录功能请联系开发者");
        }
    }

    /**
     * 命令执行方法
     * 处理accessToken登录逻辑，包括用户信息获取、商户状态检查、token生成和登录记录保存
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        UserDto userDto = null;
        ResponseEntity<String> responseEntity = null;
        
        // 第一步：尝试从缓存中根据token获取用户信息
        String userInfo = CommonCache.getAndRemoveValue(reqJson.getString("hcAccessToken") + "_sso");
        if (!StringUtil.isEmpty(userInfo)) {
            // 解析缓存中的用户信息JSON字符串
            JSONObject userJson = JSONObject.parseObject(userInfo);
            // 将JSON对象转换为UserDto对象
            userDto = BeanConvertUtil.covertBean(userJson, UserDto.class);
        }

        // 第二步：如果缓存中没有用户信息，尝试通过Base64解码token获取用户名密码进行登录
        // 注意：这种方式安全性较低，建议在生产环境中移除
        if (userDto == null) {
            try {
                // Base64解码token获取用户信息字符串
                userInfo = new String(Base64Convert.base64ToByte(reqJson.getString("hcAccessToken")), "UTF-8");
                // 解析用户信息JSON
                JSONObject userJson = JSONObject.parseObject(userInfo);
                userDto = new UserDto();
                // 设置用户手机号
                userDto.setTel(userJson.getString("tel"));
                // 对密码进行MD5加密
                userDto.setPassword(AuthenticationFactory.passwdMd5(userJson.getString("passwd")));
                // 设置用户级别（管理员和员工）
                userDto.setLevelCds(new String[]{UserDto.LEVEL_CD_ADMIN, UserDto.LEVEL_CD_STAFF});
                // 查询用户信息
                List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

                // 验证查询结果，确保只有一个用户
                Assert.listOnlyOne(userDtos, "用户不存在");
                userDto = userDtos.get(0);
            } catch (IOException e) {
                throw new CmdException("登录失败");
            }
        }

        // 第三步：检查用户关联的商户状态
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(userDto.getUserId());
        List<StoreUserDto> storeUserDtos = storeInnerServiceSMOImpl.getStoreUserInfo(storeUserDto);

        // 如果用户有关联商户，检查商户状态
        if (storeUserDtos != null && storeUserDtos.size() > 0) {
            String state = storeUserDtos.get(0).getState();
            // 如果商户状态为限制登录（48002），返回未授权响应
            if ("48002".equals(state)) {
                responseEntity = new ResponseEntity<String>("当前商户限制登录，请联系管理员", HttpStatus.UNAUTHORIZED);
                context.setResponseEntity(responseEntity);
                return;
            }
        }
        
        // 第四步：生成token并记录登录信息
        try {
            Map userMap = new HashMap();
            // 设置用户ID到登录信息中
            userMap.put(CommonConstant.LOGIN_USER_ID, userDto.getUserId());
            // 设置用户名到登录信息中
            userMap.put(CommonConstant.LOGIN_USER_NAME, userDto.getUserName());
            // 创建并保存token
            String token = AuthenticationFactory.createAndSaveToken(userMap);
            
            // 转换用户对象为JSON，并移除敏感信息
            JSONObject user = BeanConvertUtil.beanCovertJson(userDto);
            user.remove("userPwd"); // 移除密码字段
            user.put("token", token); // 添加token字段
            
            // 创建登录记录
            UserLoginPo userLoginPo = new UserLoginPo();
            userLoginPo.setLoginId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_loginId));
            userLoginPo.setLoginTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
            userLoginPo.setPassword(userDto.getPassword());
            userLoginPo.setSource(UserLoginDto.SOURCE_WEB); // 设置登录来源为WEB
            userLoginPo.setToken(token);
            userLoginPo.setUserId(userDto.getUserId());
            userLoginPo.setUserName(userDto.getUserName());
            
            // 保存登录记录
            userLoginInnerServiceSMOImpl.saveUserLogin(userLoginPo);
            
            // 设置响应结果
            context.setResponseEntity(ResultVo.createResponseEntity(user));
        } catch (Exception e) {
            logger.error("登录异常：", e);
            throw new SMOException(ResponseConstant.RESULT_CODE_INNER_ERROR, "系统内部错误，请联系管理员");
        }
    }
}