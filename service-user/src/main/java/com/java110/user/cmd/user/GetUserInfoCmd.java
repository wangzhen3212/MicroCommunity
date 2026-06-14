/**
 * 用户信息查询命令类
 * 
 * 该类是一个Java110命令处理器，专门用于获取用户详细信息。
 * 通过服务码"user.getUserInfo"标识，处理用户信息查询请求，
 * 返回包含用户基本信息和相关业务数据的结果。
 * 
 * 主要功能：
 * 1. 查询用户基本信息（姓名、地址、性别等）
 * 2. 根据店铺ID查询店铺类型信息
 * 3. 添加水印配置信息
 * 4. 返回完整的用户信息数据
 * 
 * @author Java110
 * @version 1.0
 * @since 2022
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.store.StoreDto;
import com.java110.intf.store.IStoreV1InnerServiceSMO;
import com.java110.service.context.DataQuery;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "user.getUserInfo")
public class GetUserInfoCmd extends Cmd {

    /** 查询服务接口，用于执行通用的数据查询操作 */
    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /** 店铺服务接口，用于查询店铺相关信息 */
    @Autowired
    private IStoreV1InnerServiceSMO storeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，当前实现中未进行具体验证。
     * 可根据业务需求在此方法中添加参数校验逻辑。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 当前未实现参数验证逻辑，可根据业务需求添加
    }

    /**
     * 命令执行方法
     * 
     * 主要的业务逻辑处理方法，执行以下操作：
     * 1. 从请求头中获取用户ID和店铺ID
     * 2. 调用查询服务获取用户基本信息
     * 3. 根据店铺ID查询店铺类型信息
     * 4. 构建完整的用户信息响应数据
     * 5. 设置水印配置信息
     * 6. 返回处理结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求头中获取用户ID和店铺ID
        String userId = context.getReqHeaders().get("user-id");
        String storeId = context.getReqHeaders().get("store-id");
        
        // 创建数据查询对象，设置查询参数
        DataQuery dataQuery = new DataQuery();
        dataQuery.setServiceCode("query.user.userInfo"); // 设置查询服务码
        JSONObject param = new JSONObject();
        param.put("userId", userId); // 设置用户ID查询参数
        dataQuery.setRequestParams(param);
        
        // 执行用户信息查询
        queryServiceSMOImpl.commonQueryService(dataQuery);
        ResponseEntity<String> responseEntity = dataQuery.getResponseEntity();

        // 检查查询响应状态，如果非200则直接返回错误响应
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 解析查询结果中的用户信息
        JSONObject tmpUserInfo = JSONObject.parseObject(responseEntity.getBody().toString());
        JSONObject resultUserInfo = new JSONObject(); // 创建最终返回的用户信息对象

        // 如果存在店铺ID，则查询店铺类型信息
        if (!StringUtil.isEmpty(storeId)) {
            StoreDto storeDto = new StoreDto();
            storeDto.setStoreId(storeId);
            storeDto.setPage(1); // 设置分页参数
            storeDto.setRow(1);  // 只查询一条记录
            List<StoreDto> storeDtos = storeV1InnerServiceSMOImpl.queryStores(storeDto);
            
            // 如果查询到店铺信息，将店铺类型码添加到结果中
            if (storeDtos != null && storeDtos.size() > 0) {
                resultUserInfo.put("storeTypeCd", storeDtos.get(0).getStoreTypeCd());
            }
        }

        // 将用户基本信息添加到结果对象中
        resultUserInfo.put("name", tmpUserInfo.getString("name"));        // 用户姓名
        resultUserInfo.put("address", tmpUserInfo.getString("address"));  // 用户地址
        resultUserInfo.put("sex", tmpUserInfo.getString("sex"));          // 用户性别
        resultUserInfo.put("localtionCd", tmpUserInfo.getString("localtionCd")); // 位置编码
        resultUserInfo.put("levelCd", tmpUserInfo.getString("levelCd"));  // 用户等级编码
        
        // 用户手机号处理：原计划加密，但因前台业务需求改为不加密
        // 注释说明：2022-07-04由wuxw修改，因前台多处直接使用手机号，加密后无法处理
        resultUserInfo.put("tel", tmpUserInfo.getString("tel"));
        
        resultUserInfo.put("email", tmpUserInfo.getString("email"));      // 用户邮箱
        resultUserInfo.put("userId", tmpUserInfo.getString("userId"));    // 用户ID
        
        // 从配置缓存中获取水印设置
        String watermark = MappingCache.getValue(MappingConstant.DOMAIN_SYSTEM_SWITCH,"watermark");
        resultUserInfo.put("watermark", watermark); // 添加水印配置信息

        // 创建成功的HTTP响应
        responseEntity = new ResponseEntity<String>(resultUserInfo.toJSONString(), HttpStatus.OK);

        // 设置最终的响应结果
        context.setResponseEntity(responseEntity);
    }
}