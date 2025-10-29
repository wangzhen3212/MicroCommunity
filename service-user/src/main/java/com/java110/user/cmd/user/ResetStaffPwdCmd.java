/**
 * 重置员工密码命令类
 * 
 * 该命令类用于处理重置员工密码的请求，主要功能包括：
 * 1. 验证请求参数的合法性
 * 2. 校验用户权限和商户关系
 * 3. 生成随机密码并更新用户密码
 * 4. 返回重置后的密码信息
 * 
 * 注意：需要物业管理员账号登录，系统会根据管理员登录信息自动获取storeId
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.store.StoreDto;
import com.java110.dto.store.StoreUserDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * API文档注解 - 重置密码接口
 */
@Java110CmdDoc(title = "重置密码",
        description = "外部系统通过重置员工接口 重置员工密码，注意需要物业管理员账号登录，因为不需要传storeId 是根据管理员登录信息获取的",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/user.resetStaffPwd",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "user.resetStaffPwd",
        seq = 15
)

/**
 * 请求参数文档注解
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "staffId", length = 30, remark = "员工ID"),
})

/**
 * 响应参数文档注解
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "pwd", type = "String", length = 250,  remark = "密码"),
        }
)

/**
 * 请求响应示例文档注解
 */
@Java110ExampleDoc(
        reqBody="{staffId: \"302022081831710053\"}",
        resBody="{\"msg\":\"成功\",\"code\":0,\"pwd\":\"004117\"}"
)

/**
 * 命令注解 - 指定服务编码
 */
@Java110Cmd(serviceCode = "user.resetStaffPwd")
public class ResetStaffPwdCmd extends Cmd {

    // 依赖注入 - 商户用户服务
    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    // 依赖注入 - 商户服务
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    // 依赖注入 - 用户服务
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    // 依赖注入 - 用户V1服务
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证重置密码请求的合法性，包括：
     * 1. 检查必填参数staffId是否存在
     * 2. 验证当前用户的商户权限
     * 3. 校验员工与商户之间的关系，防止非法操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {

        // 环境检查 - 开发环境专用
        Environment.isDevEnv();

        // 如果是管理员商户，直接返回，跳过后续验证
        if(reqJson.containsKey("storeId") && StoreDto.STORE_ADMIN.equals(reqJson.getString("storeId"))){
            return ;
        }

        // 如果请求中没有staffId参数，使用userId作为staffId
        if (!reqJson.containsKey("staffId")) {
            reqJson.put("staffId", reqJson.getString("userId"));
        }

        // 验证staffId参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "staffId", "请求参数中未包含staffId 节点，请确认");

        // 查询当前用户的商户信息
        StoreUserDto storeUserDt = new StoreUserDto();
        storeUserDt.setUserId(reqJson.getString("userId"));
        List<StoreUserDto> storeUserDts = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDt);
        // 验证只能查询到一条商户用户记录
        Assert.listOnlyOne(storeUserDts, "查询员工商户错误！");

        // 查询商户详细信息
        StoreDto storeDto = new StoreDto();
        if (!reqJson.containsKey("storeId")) {
            // 如果请求中没有storeId，使用查询到的商户ID
            storeDto.setStoreId(storeUserDts.get(0).getStoreId());
        } else {
            // 使用请求中的storeId
            storeDto.setStoreId(reqJson.getString("storeId"));
        }
        List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);

        // 验证商户存在且唯一
        Assert.listOnlyOne(storeDtos, "商户不存在");

        // 如果是管理员或开发者商户，直接返回，跳过后续验证
        if (StoreDto.STORE_TYPE_ADMIN.equals(storeDtos.get(0).getStoreTypeCd()) || StoreDto.STORE_TYPE_DEV.equals(storeDtos.get(0).getStoreTypeCd())) {
            return;
        }

        // 校验员工与商户之间的关系，防止跨商户操作
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(reqJson.getString("userId"));
        storeUserDto.setStoreId(reqJson.getString("storeId"));
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);

        // 验证用户与商户关系存在且唯一
        Assert.listOnlyOne(storeUserDtos, "非法操作");
    }

    /**
     * 执行重置密码命令
     * 
     * 该方法处理重置密码的核心业务逻辑：
     * 1. 调用modifyStaff方法重置密码
     * 2. 构建响应结果
     * 3. 设置HTTP响应实体
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当密码重置失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 调用重置密码方法
        modifyStaff(reqJson, cmdDataFlowContext);
        
        // 构建响应参数
        JSONObject paramOut = new JSONObject();
        paramOut.put("pwd", reqJson.getString("pwd"));  // 新生成的密码
        paramOut.put("code",0);                         // 成功状态码
        paramOut.put("msg","成功");                     // 成功消息
        
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);
        // 设置响应实体到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 重置员工密码的核心方法
     * 
     * 该方法执行具体的密码重置操作：
     * 1. 查询员工信息
     * 2. 生成随机密码
     * 3. 对密码进行MD5加密
     * 4. 更新用户密码
     * 
     * @param paramObj 请求参数JSON对象，包含staffId等信息
     * @param dataFlowContext 数据流上下文
     * @throws CmdException 当更新用户信息失败时抛出异常
     */
    private void modifyStaff(JSONObject paramObj, ICmdDataFlowContext dataFlowContext) {
        
        // 构建用户查询条件
        UserDto userDto = new UserDto();
        userDto.setStatusCd("0");                           // 设置状态为有效
        userDto.setUserId(paramObj.getString("staffId"));   // 设置要重置密码的员工ID
        
        // 查询用户信息（包含密码信息）
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUserHasPwd(userDto);

        // 验证只能查询到一条用户记录
        Assert.listOnlyOne(userDtos, "数据错误查询到多条用户信息或单条");

        // 将用户信息转换为JSON对象
        JSONObject userInfo = JSONObject.parseObject(JSONObject.toJSONString(userDtos.get(0)));
        
        // 生成6位随机密码
        String pwd = GenerateCodeFactory.getRandomCode(6);
        
        // 对密码进行MD5加密并更新到用户信息中
        userInfo.put("password", AuthenticationFactory.passwdMd5(pwd));
        
        // 将明文密码保存到参数对象中，用于返回给前端
        paramObj.put("pwd", pwd);

        // 将JSON对象转换为PO对象
        UserPo userPo = BeanConvertUtil.covertBean(userInfo, UserPo.class);

        // 更新用户信息
        int flag = userV1InnerServiceSMOImpl.updateUser(userPo);
        
        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("重置失败");
        }
    }
}