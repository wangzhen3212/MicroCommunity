/**
 * 查询管理端房屋折扣申请命令类
 * 
 * 该命令类用于处理管理端查询房屋折扣申请信息的请求，包括分页查询申请列表、
 * 关联查询申请相关的文件信息，并返回格式化后的结果数据。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.room.ApplyRoomDiscountDto;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.fee.IApplyRoomDiscountInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "fee.queryAdminApplyRoomDiscount")
public class QueryAdminApplyRoomDiscountCmd extends Cmd {

    /**
     * 房屋折扣申请内部服务接口
     */
    @Autowired
    private IApplyRoomDiscountInnerServiceSMO applyRoomDiscountInnerServiceSMOImpl;

    /**
     * 文件关联内部服务接口
     */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证管理端权限和分页参数的有效性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行查询房屋折扣申请命令
     * 
     * 主要功能：
     * 1. 查询房屋折扣申请记录总数和分页数据
     * 2. 为每个申请记录关联查询相关的文件信息
     * 3. 构建并返回格式化后的响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为ApplyRoomDiscountDto对象
        ApplyRoomDiscountDto applyRoomDiscountDto = BeanConvertUtil.covertBean(reqJson, ApplyRoomDiscountDto.class);
        
        // 查询符合条件的房屋折扣申请记录总数
        int count = applyRoomDiscountInnerServiceSMOImpl.queryApplyRoomDiscountsCount(applyRoomDiscountDto);

        List<ApplyRoomDiscountDto> applyRoomDiscountDtos = new ArrayList<>();
        
        // 如果存在记录，则查询详细数据
        if (count > 0) {
            // 分页查询房屋折扣申请记录
            List<ApplyRoomDiscountDto> applyRoomDiscounts = applyRoomDiscountInnerServiceSMOImpl.queryApplyRoomDiscounts(applyRoomDiscountDto);
            
            // 遍历每个申请记录，关联查询文件信息
            for (ApplyRoomDiscountDto applyRoomDiscount : applyRoomDiscounts) {
                // 构建文件查询条件
                FileRelDto fileRelDto = new FileRelDto();
                fileRelDto.setObjId(applyRoomDiscount.getArdId()); // 设置关联对象ID为申请记录ID
                
                // 查询与该申请记录关联的所有文件
                List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
                
                List<String> urls = new ArrayList<>();
                // 遍历文件关联记录，筛选出有效的文件URL
                for (FileRelDto fileRel : fileRelDtos) {
                    // 检查文件真实名称不为空
                    if (!StringUtil.isEmpty(fileRel.getFileRealName())) {
                        // 关联类型为19000的文件（可能是申请相关文件）
                        if (fileRel.getRelTypeCd().equals("19000")) {
                            urls.add(fileRel.getFileRealName());
                        } 
                        // 关联类型为21000的文件（可能是审批相关文件）
                        else if (fileRel.getRelTypeCd().equals("21000")) {
                            urls.add(fileRel.getFileRealName());
                        }
                    }
                }
                
                // 将文件URL列表设置到申请记录中
                applyRoomDiscount.setUrls(urls);
                // 将处理完的申请记录添加到结果列表
                applyRoomDiscountDtos.add(applyRoomDiscount);
            }
        } else {
            // 如果没有记录，返回空列表
            applyRoomDiscountDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) applyRoomDiscountDto.getRow());
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, applyRoomDiscountDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文
        context.setResponseEntity(responseEntity);
    }
}