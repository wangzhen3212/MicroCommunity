package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.user.UserAttrDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.store.IOrgStaffRelV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.user.IUserAttrV1InnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.org.OrgStaffRelPo;
import com.java110.po.store.StoreUserPo;
import com.java110.po.user.UserAttrPo;
import com.java110.po.user.UserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.constant.StoreUserRelConstant;
import com.java110.utils.constant.UserLevelConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Java110CmdDoc(title = "添加员工",
        description = "外部系统通过添加员工接口 添加员工，注意需要物业管理员账号登录，因为不需要传storeId 是根据管理员登录信息获取的",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/user.staff.add",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "user.staff.add",
        seq = 3
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "orgId", length = 30, remark = "组织ID"),
        @Java110ParamDoc(name = "orgName", length = 64, remark = "组织名称"),
        @Java110ParamDoc(name = "name", length = 64, remark = "名称"),
        @Java110ParamDoc(name = "sex", length = 64, remark = "性别 0女 1男"),
        @Java110ParamDoc(name = "email", length = 64, remark = "邮箱"),
        @Java110ParamDoc(name = "tel", length = 11, remark = "手机号"),
        @Java110ParamDoc(name = "address", length = 64, remark = "地址"),
        @Java110ParamDoc(name = "relCd", length = 64, remark = "岗位,普通员工 1000 部门经理 2000 部门副经理 3000 部门组长 4000 分公司总经理 5000 分公司副总经理 6000 总经理助理 7000 总公司总经理 8000 总公司副总经理 9000"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{\"orgId\":\"102022091988250052\",\"orgName\":\"演示物业 / 件部\",\"username\":\"张三\",\"sex\":\"0\",\"email\":\"231@qq.com\",\"tel\":\"123\",\"address\":\"123\",\"relCd\":\"1000\",\"photo\":\"\",\"name\":\"张三\"}",
        resBody = "{'code':0,'msg':'成功'}"
)

/**
 * 员工添加命令类
 * 负责处理添加员工的相关业务逻辑，包括用户信息验证、员工信息保存、组织关系建立等
 * 需要物业管理员权限登录，系统会自动从登录信息中获取storeId
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "user.staff.add")
public class UserStaffAddCmd extends Cmd {

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IUserAttrV1InnerServiceSMO userAttrV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelV1InnerServiceSMO orgStaffRelV1InnerServiceSMOImpl;

    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的合法性，包括用户登录状态和员工手机号唯一性检查
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从上下文中获取店铺ID，验证用户是否已登录
        String storeId = CmdContextUtils.getStoreId(context);
        Assert.hasLength(storeId, "用户未登录");

        // 创建用户查询对象，设置查询条件
        UserDto userDto = new UserDto();
        userDto.setTel(reqJson.getString("tel")); // 设置手机号
        userDto.setUserFlag("1"); // 设置用户标志
        userDto.setLevelCd("01"); // 设置用户级别为员工
        userDto.setStoreId(storeId); // 设置店铺ID
        
        // 查询是否存在相同手机号的员工
        List<UserDto> users = userInnerServiceSMOImpl.getStaffs(userDto);
        // 验证手机号唯一性，如果已存在则抛出异常
        Assert.listIsNull(users, "员工手机号不能重复，请重新输入");
    }

    /**
     * 执行命令方法
     * 处理添加员工的核心业务逻辑，包括用户信息保存、员工信息保存、组织关系建立和头像保存
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String userId = "";

        // 获取员工岗位信息
        String relCd = reqJson.getString("relCd");

        // 生成用户ID
        userId = GenerateCodeFactory.getUserId();
        reqJson.put("userId", userId);
        
        // 添加用户基本信息
        addUser(reqJson);
        reqJson.put("userId", userId);
        
        // 设置默认岗位关系码
        if (!reqJson.containsKey("relCd") || StringUtil.isEmpty(reqJson.getString("relCd"))) {
            reqJson.put("relCd", StoreUserRelConstant.REL_COMMON);
        }
        
        // 添加员工信息
        addStaff(reqJson);
        
        // 重新设置员工岗位，添加组织关系
        reqJson.put("relCd", relCd);
        addStaffOrg(reqJson);
        
        // 处理员工头像保存
        int flag = 0;
        if (reqJson.containsKey("photo") && !StringUtils.isEmpty(reqJson.getString("photo"))) {
            JSONObject businessUnit = new JSONObject();
            businessUnit.put("fileRelId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_fileRelId));
            businessUnit.put("relTypeCd", "12000"); // 设置关系类型码
            businessUnit.put("saveWay", "table"); // 设置保存方式
            businessUnit.put("objId", userId); // 设置关联对象ID
            businessUnit.put("fileRealName", reqJson.getString("photo")); // 设置文件真实名称
            businessUnit.put("fileSaveName", reqJson.getString("photo")); // 设置文件保存名称
            
            // 转换并保存文件关系信息
            FileRelPo fileRelPo = BeanConvertUtil.covertBean(businessUnit, FileRelPo.class);
            flag = fileRelInnerServiceSMOImpl.saveFileRel(fileRelPo);
            if (flag < 1) {
                throw new CmdException("保存图片异常");
            }
        }
    }

    /**
     * 添加用户基本信息
     * 验证并保存用户的基本信息，包括姓名、电话、地址、性别等
     *
     * @param paramObj 包含用户信息的JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    public void addUser(JSONObject paramObj) {
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(paramObj, "name", "请求参数中未包含name 节点，请确认");
        Assert.jsonObjectHaveKey(paramObj, "tel", "请求参数中未包含tel 节点，请确认");
        Assert.jsonObjectHaveKey(paramObj, "address", "请求报文格式错误或未包含地址信息");
        Assert.jsonObjectHaveKey(paramObj, "sex", "请求报文格式错误或未包含性别信息");

        // 验证邮箱格式（如果提供了邮箱）
        if (paramObj.containsKey("email") && !StringUtil.isEmpty(paramObj.getString("email"))) {
            Assert.isEmail(paramObj, "email", "不是有效的邮箱格式");
        }

        // 刷新参数并转换为用户PO对象
        UserPo userPo = BeanConvertUtil.covertBean(refreshParamIn(paramObj), UserPo.class);
        // 保存用户信息
        int flag = userV1InnerServiceSMOImpl.saveUser(userPo);

        if (flag < 1) {
            throw new CmdException("保存用户异常");
        }
        
        // 保存用户身份证信息（如果提供了身份证号）
        saveUserIdCard(userPo, paramObj);
    }

    /**
     * 保存用户身份证信息
     * 如果请求中包含身份证号，则保存到用户属性表中
     *
     * @param userPo 用户持久化对象
     * @param paramObj 包含身份证号的请求参数JSON对象
     */
    private void saveUserIdCard(UserPo userPo, JSONObject paramObj) {
        // 检查是否包含身份证号参数
        if (!paramObj.containsKey("idCard")) {
            return;
        }

        String idCard = paramObj.getString("idCard");
        // 检查身份证号是否为空
        if (StringUtil.isEmpty(idCard)) {
            return;
        }

        // 创建用户属性对象并设置属性值
        UserAttrPo userAttrPo = new UserAttrPo();
        userAttrPo.setAttrId(GenerateCodeFactory.getAttrId()); // 生成属性ID
        userAttrPo.setSpecCd(UserAttrDto.SPEC_ID_CARD); // 设置特殊代码为身份证
        userAttrPo.setUserId(userPo.getUserId()); // 设置用户ID
        userAttrPo.setValue(idCard); // 设置身份证号

        // 保存用户属性信息
        userAttrV1InnerServiceSMOImpl.saveUserAttr(userAttrPo);
    }

    /**
     * 刷新输入参数
     * 对请求参数进行预处理，设置默认的用户级别和密码
     *
     * @param paramObj 原始请求参数JSON对象
     * @return 处理后的参数JSON对象
     */
    private JSONObject refreshParamIn(JSONObject paramObj) {
        // 设置用户级别为员工
        paramObj.put("levelCd", UserLevelConstant.USER_LEVEL_STAFF);
        
        // 从缓存中获取员工默认密码
        String staffDefaultPassword = MappingCache.getValue(MappingConstant.KEY_STAFF_DEFAULT_PASSWORD);
        Assert.hasLength(staffDefaultPassword, "映射表中未设置员工默认密码，请检查" + MappingConstant.KEY_STAFF_DEFAULT_PASSWORD);
        
        // 对密码进行MD5加密
        staffDefaultPassword = AuthenticationFactory.passwdMd5(staffDefaultPassword);
        paramObj.put("password", staffDefaultPassword);
        
        return paramObj;
    }

    /**
     * 添加员工信息
     * 保存员工与店铺的关系信息
     *
     * @param paramInJson 包含员工信息的JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    public void addStaff(JSONObject paramInJson) {
        // 构建员工店铺关系数据
        JSONObject businessStoreUser = new JSONObject();
        businessStoreUser.put("storeId", paramInJson.getString("storeId")); // 店铺ID
        businessStoreUser.put("storeUserId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_storeUserId)); // 生成店铺用户ID
        businessStoreUser.put("userId", paramInJson.getString("userId")); // 用户ID
        businessStoreUser.put("relCd", paramInJson.getString("relCd")); // 关系代码

        // 转换并保存员工店铺关系
        StoreUserPo storeUserPo = BeanConvertUtil.covertBean(businessStoreUser, StoreUserPo.class);
        int flag = storeUserV1InnerServiceSMOImpl.saveStoreUser(storeUserPo);

        if (flag < 1) {
            throw new CmdException("保存员工 失败");
        }
    }

    /**
     * 添加员工组织关系
     * 保存员工与组织部门的关联关系
     *
     * @param paramInJson 包含组织信息的JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    public void addStaffOrg(JSONObject paramInJson) {
        // 检查是否包含组织ID
        if (!paramInJson.containsKey("orgId") || StringUtil.isEmpty(paramInJson.getString("orgId"))) {
            return;
        }

        // 构建员工组织关系数据
        JSONObject businessOrgStaffRel = new JSONObject();
        businessOrgStaffRel.put("relId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId)); // 生成关系ID
        businessOrgStaffRel.put("storeId", paramInJson.getString("storeId")); // 店铺ID
        businessOrgStaffRel.put("staffId", paramInJson.getString("userId")); // 员工ID
        businessOrgStaffRel.put("orgId", paramInJson.getString("orgId")); // 组织ID
        businessOrgStaffRel.put("relCd", paramInJson.getString("relCd")); // 关系代码

        // 转换并保存员工组织关系
        OrgStaffRelPo orgStaffRelPo = BeanConvertUtil.covertBean(businessOrgStaffRel, OrgStaffRelPo.class);
        int flag = orgStaffRelV1InnerServiceSMOImpl.saveOrgStaffRel(orgStaffRelPo);
        if (flag < 1) {
            throw new CmdException("保存员工 失败");
        }
    }
}