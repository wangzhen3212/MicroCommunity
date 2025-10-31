package com.java110.dev.cmd.menu;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.menu.MenuDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.menu.ApiMenuDataVo;
import com.java110.vo.api.menu.ApiMenuVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单列表查询命令类
 * 负责处理菜单列表查询请求，返回分页的菜单数据
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"menu.listMenus"
 */
@Java110Cmd(serviceCode = "menu.listMenus")
public class ListMenusCmd extends Cmd {

    /**
     * 菜单内部服务接口，用于查询菜单数据
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数验证
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询菜单列表数据并返回分页结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为MenuDto对象，用于查询条件
        MenuDto menuDto = BeanConvertUtil.covertBean(reqJson, MenuDto.class);

        // 查询符合条件的菜单总数
        int count = menuInnerServiceSMOImpl.queryMenusCount(menuDto);

        // 菜单数据列表
        List<ApiMenuDataVo> menus = null;

        // 如果有数据，则查询具体的菜单列表
        if (count > 0) {
            // 查询菜单数据并转换为ApiMenuDataVo对象列表
            menus = BeanConvertUtil.covertBeanList(menuInnerServiceSMOImpl.queryMenus(menuDto), ApiMenuDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            menus = new ArrayList<>();
        }

        // 创建返回VO对象
        ApiMenuVo apiMenuVo = new ApiMenuVo();

        // 设置总记录数
        apiMenuVo.setTotal(count);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiMenuVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置菜单数据列表
        apiMenuVo.setMenus(menus);

        // 创建HTTP响应实体，将结果转换为JSON字符串返回
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiMenuVo), HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}