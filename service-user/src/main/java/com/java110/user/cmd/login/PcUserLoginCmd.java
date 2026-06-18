package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.app.AppDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserLoginDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.po.user.UserLoginPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户登录 功能
 * 请求地址为/app/login.pcUserLogin
 */

/**
 * PC端用户登录命令类
 * 处理员工和管理员的登录请求，验证用户身份并生成访问令牌
 * 支持用户名和手机号两种登录方式，会话保持时间为2小时
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "员工登录",
        description = "登录功能 主要用于 员工 或者管理员登录使用,<br/>" +
                "请求其他接口时 头信息中需要加 Authorization: Bearer token ，<br/>" +
                "token 是这个接口返回的内容<br/> " +
                "会话保持为2小时，请快要到2小时时，再次登录，保持会话</br>",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/login.pcUserLogin",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "login.pcUserLogin",
        seq = 1
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
                @Java110ParamDoc(name = "username", length = 30, remark = "用户名，物业系统分配"),
                @Java110ParamDoc(name = "passwd", length = 30, remark = "密码，物业系统分配"),
        })

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "userId", type = "String", remark = "用户ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "token", type = "String", remark = "临时票据"),
        }
)

@Java110ExampleDoc(
        reqBody = "{'username':'wuxw','passwd':'admin'}",
        resBody = "{'code':0,'msg':'成功','data':{'userId':'123123','token':'123213'}}"
)
@Java110Cmd(serviceCode = "login.pcUserLogin")
public class PcUserLoginCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(PcUserLoginCmd.class);
    
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
     * 验证登录请求的必填参数，检查登录错误次数，处理密码加密
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "username", "用户登录，未包含username节点，请检查");
        Assert.hasKeyAndValue(reqJson, "passwd", "用户登录，未包含passwd节点，请检查");

        // 检查登录错误次数限制
        AuthenticationFactory.checkLoginErrorCount(reqJson.getString("username"));

        // 根据应用类型处理密码加密
        String appId = CmdContextUtils.getAppId(cmdDataFlowContext);
        if(AppDto.PROPERTY_APP.equals(appId)){
            // 物业应用需要对密码进行MD5加密
            reqJson.put("passwd", AuthenticationFactory.passwdMd5(reqJson.getString("passwd")));
        }

    }

    /**
     * 执行登录命令
     * 处理用户登录逻辑，包括用户验证、商户状态检查、令牌生成和登录记录
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        ResponseEntity responseEntity = null;
        
        // 根据用户名和密码查询用户信息
        UserDto userDto = new UserDto();
        userDto.setName(reqJson.getString("username"));
        userDto.setPassword(reqJson.getString("passwd"));
        // 设置用户级别：管理员或员工
        userDto.setLevelCds(new String[]{UserDto.LEVEL_CD_ADMIN, UserDto.LEVEL_CD_STAFF});
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);
        
        // 如果用户名查询不到用户，尝试使用手机号查询
        if (ListUtil.isNull(userDtos)) {
            userDto.setName("");
            userDto.setTel(reqJson.getString("username"));
            userDtos = userInnerServiceSMOImpl.getUsers(userDto);
        }
        
        // 用户不存在或密码错误
        if (ListUtil.isNull(userDtos)) {
            responseEntity = new ResponseEntity<String>("用户或密码错误", HttpStatus.UNAUTHORIZED);
            // 记录登录错误次数
            AuthenticationFactory.userLoginError(reqJson.getString("username"));
            cmdDataFlowContext.setResponseEntity(responseEntity);
            return;
        }

        // 检查商户状态是否允许登录
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(userDtos.get(0).getUserId());
        List<StoreUserDto> storeUserDtos = storeInnerServiceSMOImpl.getStoreUserInfo(storeUserDto);

        if (!ListUtil.isNull(storeUserDtos)) {
            String state = storeUserDtos.get(0).getState();
            // 商户状态为48002表示限制登录
            if ("48002".equals(state)) {
                responseEntity = new ResponseEntity<String>("当前商户限制登录，请联系管理员", HttpStatus.UNAUTHORIZED);
                cmdDataFlowContext.setResponseEntity(responseEntity);
                return;
            }
        }
        
        // 获取员工详细信息
        UserDto allUserDto = new UserDto();
        allUserDto.setTel(userDtos.get(0).getTel());
        userDtos = userInnerServiceSMOImpl.getStaffs(userDto);
        if (userDtos.isEmpty()) {
            throw new CmdException("用户不存在");
        }
        
        // 生成登录令牌和用户信息
        JSONArray data = new JSONArray();
        JSONObject userInfo = null;
        for(UserDto aUserDto: userDtos) {
            try {
                Map userMap = new HashMap();
                userMap.put(CommonConstant.LOGIN_USER_ID, aUserDto.getUserId());
                userMap.put(CommonConstant.LOGIN_USER_NAME, aUserDto.getUserName());
                // 创建并保存访问令牌
                String token = AuthenticationFactory.createAndSaveToken(userMap);
                // 转换用户对象为JSON，移除敏感信息
                userInfo= BeanConvertUtil.beanCovertJson(aUserDto);
                userInfo.remove("userPwd");
                userInfo.put("token", token);
                data.add(userInfo);
            } catch (Exception e) {
                logger.error("登录异常：", e);
                throw new SMOException(ResponseConstant.RESULT_CODE_INNER_ERROR, "系统内部错误，请联系管理员");
            }
        }

        // 记录用户登录日志
        UserLoginPo userLoginPo = new UserLoginPo();
        userLoginPo.setLoginId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_loginId));
        userLoginPo.setLoginTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        userLoginPo.setPassword("******"); // 密码脱敏
        userLoginPo.setSource(UserLoginDto.SOURCE_WEB); // 登录来源：WEB端
        userLoginPo.setToken(userInfo.getString("token"));
        userLoginPo.setUserId(userInfo.getString("userId"));
        userLoginPo.setUserName(userInfo.getString("userName"));
        userLoginInnerServiceSMOImpl.saveUserLogin(userLoginPo);

        // 返回登录成功响应
        responseEntity = ResultVo.createResponseEntity(data);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

}