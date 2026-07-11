package com.java110.fee.bmo.feePrintPageTemplate.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.PageDto;
import com.java110.dto.fee.FeePrintPageTemplateDto;
import com.java110.fee.bmo.feePrintPageTemplate.IGetFeePrintPageTemplateBMO;
import com.java110.intf.fee.IFeePrintPageTemplateV1InnerServiceSMO;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("getFeePrintPageTemplateBMOImpl")
public class GetFeePrintPageTemplateBMOImpl extends BaseServiceSMO implements IGetFeePrintPageTemplateBMO {

    @Autowired
    private IFeePrintPageTemplateV1InnerServiceSMO feePrintPageTemplateV1InnerServiceSMOImpl;

    /**
     * 查询打印模板
     *
     * @param feePrintPageTemplateDto
     * @return 订单服务能够接受的报文
     */
    public ResponseEntity<String> get(FeePrintPageTemplateDto feePrintPageTemplateDto) {

        int count = feePrintPageTemplateV1InnerServiceSMOImpl.queryFeePrintPageTemplatesCount(feePrintPageTemplateDto);

        if (count < 1) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "未找到记录", new JSONObject());
        }

        List<FeePrintPageTemplateDto> feePrintPageTemplateList = feePrintPageTemplateV1InnerServiceSMOImpl.queryFeePrintPageTemplates(feePrintPageTemplateDto);

        //总量校验
        validateResult(count, feePrintPageTemplateList);

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) feePrintPageTemplateDto.getRow()), count, BeanConvertUtil.beanCovertMap(feePrintPageTemplateList));
        return ResultVo.createResponseEntity(resultVo);
    }

}
