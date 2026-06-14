/**
 * 特来电获取Token命令类
 * 
 * 该类主要用于特来电平台的身份认证，生成访问令牌(Token)
 * 通过验证用户凭证后生成临时访问令牌，用于后续接口调用的身份验证
 * 令牌有效期为2小时，客户端需要在令牌过期前重新获取
 * 
 * @author 吴学文
 * @version 1.0
 * @see Cmd
 * @see IUserV1InnerServiceSMO
 */
package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.utils.util.TeldUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Java110CmdDoc(title = "特来电获取token",
        description = "主要用于 特来电平台使用,<br/>" +
                "请求其他接口时 头信息中需要加 Authorization: Bearer token ，<br/>" +
                "token 是这个接口返回的内容<br/> " +
                "会话保持为2小时，请快要到2小时时，再次登录，保持会话</br>",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/ext/{appId}/query_token",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "login.getTokenForTeld",
        seq = 1
)

@Java110ParamsDoc(
        headers = {
                @Java110HeaderDoc(name = "Content-Type", defaultValue = "application/json", description = "application/json"),
        },
        params = {
                @Java110ParamDoc(name = "OperatorID", length = 30, remark = "调用方的组织机构代码"),
                @Java110ParamDoc(name = "OperatorSecret", length = 30, remark = "服务方分配的唯一识别密钥"),
        })

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "Ret", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "Msg", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "Data", type = "Object", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "TokenAvailableTime", type = "int", remark = "有效期"),
                @Java110ParamDoc(parentNodeName = "data", name = "AccessToken", type = "String", remark = "临时票据"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\n" +
                "  \"OperatorID\": \"123456789\",\n" +
                "  \"Data\": \"mYvffpNoFf4E/ZTC1tOw41TC5OlkEobfAYCm5N8hEusaLUaUIqOrXtdbMrSck0DSmfM7mRuOGMoCQzH0nWPGuw==\",\n" +
                "  \"TimeStamp\": \"20180120165755\",\n" +
                "  \"Seq\": \"0001\",\n" +
                "  \"Sig\": \"D2D584A14F3F284445DF85D0E8C0697C\"\n" +
                "}",
        resBody = "{\n" +
                "    \"Ret\": 0,\n" +
                "    \"Msg\": \"\",\n" +
                "    \"Data\": \"uxeKP0ezR5yL8xSg4/ZCDh/N91/u86NXFxd2DrwZVW8zCPYcpl59Twz/yQZ3RaO4rDDrGmkvQignmNEJ+k4PGxdmIC+4fpJ8rU6osSobY+AeA0uueuQ5+eQiWBL6p6v5XMMm91brtK8yfFELYUWQzVcxABnAwK/+dyxtUhqLIxUpkwTEU/4ktN40df9IzzlLO5uvUknPGYu9yL0pp5w9vdRxmA1RiiTDNCysz6klr9bunGV3VJa2qpLcgeZMf/oG\",\n" +
                "    \"Sig\": \"58E52010C7DEE87FE183B0AFA5B2BE30\"\n" +
                "}"
)
@Java110Cmd(serviceCode = "query_token")
public class QueryTokenForTeldCmd extends Cmd {

    /** 操作密钥常量 - 用于加密通信 */
    public static String OPERATOR_SECRET = "1234567890abcdef"; 

    /** 操作ID常量 - 9位组织机构代码 */
    public static String OPERATOR_ID = "123456789"; 
    
    /** 特来电平台API地址 */
    public static String URL = "http://hlht.telaidian.com.cn:9501/evcs/v20161110/";

    /** 用户服务接口 - 用于用户信息查询和验证 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求中是否包含必要的加密数据字段
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当验证失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求JSON中是否包含Data字段（加密报文）
        Assert.hasKeyAndValue(reqJson, "Data", "未包含加密报文");
    }

    /**
     * 执行获取Token命令
     * 
     * 主要流程：
     * 1. 从配置中获取加解密密钥
     * 2. 解密请求数据
     * 3. 验证用户身份
     * 4. 生成访问令牌
     * 5. 构造并返回响应数据
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从缓存中获取特来电平台的加解密配置参数
        String signKey = MappingCache.getValue("TELD_DOMAIN","SIGN_KEY");    // 签名密钥
        String aesKey = MappingCache.getValue("TELD_DOMAIN","AES_KEY");      // AES加密密钥
        String aesIv = MappingCache.getValue("TELD_DOMAIN","AES_IV");        // AES初始化向量

        // 使用AES解密请求中的加密数据
        String data = TeldUtil.Decrypt(reqJson.getString("Data"), aesKey, aesIv);

        // 检查解密后的数据是否为空
        if (StringUtil.isEmpty(data)) {
            throw new CmdException("未包含报文");
        }

        // 将解密后的JSON字符串解析为JSON对象
        JSONObject dataObj = JSONObject.parseObject(data);

        // 创建用户DTO对象用于用户验证
        UserDto userDto = new UserDto();
        // 使用OperatorID作为电话号码进行用户查询
        userDto.setTel(dataObj.getString("OperatorID"));
        // 对OperatorSecret进行MD5加密后作为密码进行验证
        userDto.setPassword(AuthenticationFactory.md5UserPassword(dataObj.getString("OperatorSecret")));
        // 查询匹配的用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证查询结果：必须且只能有一个匹配的用户
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 准备用户信息映射，用于生成Token
        Map userMap = new HashMap();
        userMap.put(CommonConstant.LOGIN_USER_ID, userDtos.get(0).getUserId());      // 用户ID
        userMap.put(CommonConstant.LOGIN_USER_NAME, userDtos.get(0).getUserName());  // 用户名
        
        String paramOut = "";
        try {
            // 创建并保存用户Token，返回Token字符串
            String token = AuthenticationFactory.createAndSaveToken(userMap);
            
            // 构造返回给客户端的响应数据
            JSONObject param = new JSONObject();
            param.put("OperatorID", dataObj.getString("OperatorID"));    // 操作ID
            param.put("SuccStat", 0);                                    // 成功状态
            param.put("AccessToken", token);                             // 访问令牌
            param.put("TokenAvailableTime", 7200);                       // 令牌有效期（2小时）
            param.put("FailReason", 0);                                  // 失败原因
            
            // 生成加密的返回参数
            paramOut = TeldUtil.generateReturnParam(param.toJSONString(), aesKey, aesIv, signKey, dataObj.getString("OperatorID"));
            
            // 设置HTTP响应实体
            context.setResponseEntity(new ResponseEntity(paramOut, HttpStatus.OK));
        } catch (Exception e) {
            // 处理异常情况
            throw new RuntimeException(e);
        }
    }
}