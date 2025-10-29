/**
 * 运营团队管理员登录到物业账号命令类
 * 
 * 该命令类实现了管理员账号登录到所管理的物业系统账号的功能。
 * 主要用于运营团队管理员在后台系统中临时切换到物业账号进行操作。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.cache.Java110RedisConfig;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserLoginDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.po.user.UserLoginPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Java110CmdDoc(title = "运营团队admin登录到物业账号",
        description = "在admin账号下可以登录到管理的物业系统账号下",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/login.adminLoginProperty",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "login.adminLoginProperty",
        seq = 2
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "username", length = 30, remark = "需要登录的用户"),
        @Java110ParamDoc(name = "userId", length = 30, remark = "需要登录的用户ID"),
        @Java110ParamDoc(name = "curPasswd", length = 30, remark = "当前用户密码"),
        @Java110ParamDoc(name = "curUserName", length = 64, remark = "当前用户"),
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
        reqBody = "{'username':'admin','userId':'123','curPassWd':'admin','curUserName':'18909711443'}",
        resBody = "{'code':0,'msg':'成功','data':{'userId':'123123','token':'123213'}}"
)

@Java110Cmd(serviceCode = "login.adminLoginProperty")
public class AdminLoginPropertyCmd extends Cmd {

    @Autowired
    private IUserLoginInnerServiceSMO userLoginInnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，并对密码进行MD5加密处理
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "username", "未包含需要登录的用户名");
        Assert.hasKeyAndValue(reqJson, "userId", "未包含需要登录的用户ID");
        Assert.hasKeyAndValue(reqJson, "curPasswd", "未包含当前用户的密码");
        Assert.hasKeyAndValue(reqJson, "curUserName", "未包含当前用户的用户名");
        
        // 对当前用户密码进行MD5加密
        reqJson.put("curPasswd", AuthenticationFactory.passwdMd5(reqJson.getString("curPasswd")));
        
        // 调用父类方法验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 
     * 处理管理员登录到物业账号的核心业务逻辑，包括：
     * 1. 验证当前用户身份
     * 2. 验证目标物业账号
     * 3. 生成访问令牌
     * 4. 记录登录日志
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务逻辑执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String userId = CmdContextUtils.getUserId(context);
        ResponseEntity<String> responseEntity = null;
        
        // 验证当前用户身份
        UserDto userDto = new UserDto();
        userDto.setUserName(reqJson.getString("curUserName"));
        userDto.setPassword(reqJson.getString("curPasswd"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        if (ListUtil.isNull(userDtos)) {
            throw new CmdException("用户或密码错误");
        }

        // 校验需要登录的物业账号是否存在
        userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        userDto.setUserName(reqJson.getString("username"));
        userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 确保物业账号存在且唯一
        Assert.listOnlyOne(userDtos, "物业账号不存在");

        // 获取物业账号信息并生成令牌
        userDto = userDtos.get(0);
        JSONArray data = new JSONArray();
        JSONObject userInfo = null;
        try {
            Map userMap = new HashMap();
            // 构建用户信息映射
            userMap.put(CommonConstant.LOGIN_USER_ID, userDto.getUserId());
            userMap.put(CommonConstant.LOGIN_USER_NAME, userDto.getName());
            
            // 创建并保存访问令牌
            String token = AuthenticationFactory.createAndSaveToken(userMap);
            
            // 转换用户对象为JSON并移除敏感信息
            userInfo = BeanConvertUtil.beanCovertJson(userDto);
            userInfo.remove("userPwd"); // 移除密码字段，确保安全
            userInfo.put("token", token); // 添加令牌信息
            data.add(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SMOException(ResponseConstant.RESULT_CODE_INNER_ERROR, "系统内部错误，请联系管理员");
        }

        // 记录登录日志
        UserLoginPo userLoginPo = new UserLoginPo();
        userLoginPo.setLoginId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_loginId));
        userLoginPo.setLoginTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        userLoginPo.setPassword("******"); // 密码字段脱敏处理
        userLoginPo.setSource(UserLoginDto.SOURCE_WEB); // 设置登录来源为WEB
        userLoginPo.setToken(userInfo.getString("token"));
        userLoginPo.setUserId(userInfo.getString("userId"));
        userLoginPo.setUserName(userInfo.getString("userName"));
        userLoginInnerServiceSMOImpl.saveUserLogin(userLoginPo);

        // 构建响应结果
        responseEntity = ResultVo.createResponseEntity(data);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 清理用户缓存
     * 
     * 清除指定用户相关的缓存数据，包括：
     * 1. 商户信息缓存
     * 2. 商户入驻小区缓存
     * 3. 用户权限缓存
     * 
     * @param userId 用户ID
     */
    private void clearUserCache(String userId) {
        // 清理员工商户缓存
        String storeId = "";

        String storeInfo = CommonCache.getValue("getStoreInfo" + Java110RedisConfig.GET_STORE_INFO_EXPIRE_TIME_KEY + "::" + userId);
        if (!StringUtil.isEmpty(storeInfo)) {
            // 移除商户信息缓存
            CommonCache.removeValue("getStoreInfo" + Java110RedisConfig.GET_STORE_INFO_EXPIRE_TIME_KEY + "::" + userId);
            JSONObject storeObj = JSONObject.parseObject(storeInfo);
            storeId = storeObj.getJSONObject("msg").getString("storeId");
            // 移除商户入驻小区缓存
            CommonCache.removeValue("getStoreEnterCommunitys" + Java110RedisConfig.GET_STORE_ENTER_COMMUNITYS_EXPIRE_TIME_KEY + "::" + storeId);
        }
        // 清理员工权限缓存
        CommonCache.removeValue("getUserPrivileges" + Java110RedisConfig.DEFAULT_EXPIRE_TIME_KEY + "::" + userId);
    }
}