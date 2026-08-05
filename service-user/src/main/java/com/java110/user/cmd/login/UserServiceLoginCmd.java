/**
 * 用户服务登录命令类
 * 
 * 该类负责处理用户登录请求，包括参数验证、用户身份认证、商户状态检查、
 * 生成登录令牌和记录登录日志等功能。实现了Cmd接口，通过注解标识服务代码。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.store.StoreDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserLoginDto;
import com.java110.intf.store.IStoreInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserLoginInnerServiceSMO;
import com.java110.po.user.UserLoginPo;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.ServiceCodeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.SMOException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "user.service.login")
public class UserServiceLoginCmd extends Cmd{

    // 日志记录器
    private final static Logger logger = LoggerFactory.getLogger(UserServiceLoginCmd.class);

    // 查询服务接口
    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    // 用户登录服务接口
    @Autowired
    private IUserLoginInnerServiceSMO userLoginInnerServiceSMOImpl;

    // 商户服务接口
    @Autowired
    private IStoreInnerServiceSMO storeInnerServiceSMOImpl;

    // 用户服务接口
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;
    
    /**
     * 验证请求参数
     * 
     * 检查请求JSON中是否包含必要的用户名和密码字段
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证用户名参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "username", "用户登录，未包含username节点，请检查" + reqJson);
        // 验证密码参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "passwd", "用户登录，未包含passwd节点，请检查" + reqJson);
    }

    /**
     * 执行用户登录命令
     * 
     * 处理用户登录逻辑，包括用户认证、商户状态检查、生成令牌和记录登录日志
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象，包含用户名和密码
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 创建数据查询对象
        DataQuery dataQuery = new DataQuery();
        // 设置查询服务代码
        dataQuery.setServiceCode(ServiceCodeConstant.SERVICE_CODE_QUERY_USER_LOGIN);
        
        // 构建查询参数
        JSONObject param = new JSONObject();
        param.put("userCode", reqJson.getString("username"));  // 用户名
        param.put("pwd", reqJson.getString("passwd"));         // 密码
        param.put("levelCdTag","1");                           // 用户级别标签
        dataQuery.setRequestParams(param);
        
        // 执行用户登录查询
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> responseEntity = dataQuery.getResponseEntity();
        
        // 检查查询响应状态
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(new ResponseEntity<>("初始化商户", HttpStatus.FORBIDDEN));
            return;
        }
        
        // 验证返回结果是否为有效的JSON格式
        Assert.isJsonObject(responseEntity.getBody(), "调用登录查询异常,返回报文有误，不是有效的json格式 " + responseEntity.getBody());

        // 解析查询结果
        JSONObject resultInfo = JSONObject.parseObject(responseEntity.getBody());
        // 检查返回结果中是否包含必要的用户信息
        if (!resultInfo.containsKey("user") || !resultInfo.getJSONObject("user").containsKey("userPwd")
                || !resultInfo.getJSONObject("user").containsKey("userId")) {
            // 业务错误返回 HTTP 200 + 业务码，避免 dealCmd 层 4xx 抛异常记 ERROR 日志
            responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_UNAUTHORIZED, "用户或密码错误");
            context.setResponseEntity(responseEntity);
            return;
        }

        // 获取用户信息并验证密码
        JSONObject userInfo = resultInfo.getJSONObject("user");
        String userPwd = userInfo.getString("userPwd");
        // 比较输入的密码和数据库中的密码
        if (!userPwd.equals(reqJson.getString("passwd"))) {
            // 业务错误返回 HTTP 200 + 业务码，避免 dealCmd 层 4xx 抛异常记 ERROR 日志
            responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_UNAUTHORIZED, "密码错误");
            context.setResponseEntity(responseEntity);
            return;
        }

        // 检查商户状态
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(userInfo.getString("userId"));  // 设置用户ID
        List<StoreUserDto> storeUserDtos = storeInnerServiceSMOImpl.getStoreUserInfo(storeUserDto);

        // 如果用户关联了商户，检查商户状态
        if (storeUserDtos != null && storeUserDtos.size() > 0) {
            String state = storeUserDtos.get(0).getState();
            // 检查商户是否被限制登录（状态码48002表示限制登录）
            if ("48002".equals(state)) {
                // 业务错误返回 HTTP 200 + 业务码，避免 dealCmd 层 4xx 抛异常记 ERROR 日志
                responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_UNAUTHORIZED, "当前商户限制登录，请联系管理员");
                context.setResponseEntity(responseEntity);
                return;
            }

            // 获取商户详细信息
            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(storeUserDtos.get(0).getStoreId());
            List<StoreDto> storeDtos = storeInnerServiceSMOImpl.getStores(storeDto);
            if (storeDtos != null && storeDtos.size() > 0) {
                // 将商户类型添加到用户信息中
                userInfo.put("storeType", storeDtos.get(0).getStoreTypeCd());
            }
        }

        try {
            // 准备用户信息映射，用于生成令牌
            Map userMap = new HashMap();
            userMap.put(CommonConstant.LOGIN_USER_ID, userInfo.getString("userId"));      // 用户ID
            userMap.put(CommonConstant.LOGIN_USER_NAME, userInfo.getString("userName")); // 用户名
            
            // 创建并保存认证令牌
            String token = AuthenticationFactory.createAndSaveToken(userMap);
            
            // 从用户信息中移除密码，确保安全
            userInfo.remove("userPwd");
            // 将令牌添加到用户信息中
            userInfo.put("token", token);
            
            // 记录用户登录日志
            UserLoginPo userLoginPo = new UserLoginPo();
            userLoginPo.setLoginId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_loginId));  // 生成登录ID
            userLoginPo.setLoginTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));                           // 设置登录时间
            userLoginPo.setPassword(userPwd);                                                                    // 记录密码（加密后）
            userLoginPo.setSource(UserLoginDto.SOURCE_WEB);                                                      // 设置登录来源为WEB
            userLoginPo.setToken(token);                                                                         // 记录令牌
            userLoginPo.setUserId(userInfo.getString("userId"));                                                 // 用户ID
            userLoginPo.setUserName(userInfo.getString("userName"));                                             // 用户名
            userLoginInnerServiceSMOImpl.saveUserLogin(userLoginPo);                                             // 保存登录记录
            
            // 返回登录成功的响应
            responseEntity = new ResponseEntity<String>(userInfo.toJSONString(), HttpStatus.OK);
            context.setResponseEntity(responseEntity);
        } catch (Exception e) {
            // 记录登录异常日志
            logger.error("登录异常：", e);
            // 抛出系统内部错误异常
            throw new SMOException(ResponseConstant.RESULT_CODE_INNER_ERROR, "系统内部错误，请联系管理员");
        }
    }

    /**
     * 刷新请求参数
     * 
     * 对传入的参数进行预处理，设置默认的用户ID和级别编码
     * 
     * @param paramIn 原始请求参数字符串
     * @return 处理后的JSONObject参数对象
     */
    private JSONObject refreshParamIn(String paramIn) {
        // 解析原始参数
        JSONObject paramObj = JSONObject.parseObject(paramIn);
        // 设置默认用户ID
        paramObj.put("userId", "-1");
        // 设置默认级别编码
        paramObj.put("levelCd", "0");

        return paramObj;
    }
}