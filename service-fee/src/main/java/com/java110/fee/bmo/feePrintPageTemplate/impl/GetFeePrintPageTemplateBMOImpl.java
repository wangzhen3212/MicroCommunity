package com.java110.fee.bmo.feePrintPageTemplate.impl;

import com.java110.dto.fee.FeePrintPageTemplateDto;
import com.java110.fee.bmo.feePrintPageTemplate.IGetFeePrintPageTemplateBMO;
import com.java110.intf.fee.IFeePrintPageTemplateV1InnerServiceSMO;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("getFeePrintPageTemplateBMOImpl")
public class GetFeePrintPageTemplateBMOImpl implements IGetFeePrintPageTemplateBMO {

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

        List<FeePrintPageTemplateDto> feePrintPageTemplateList = null;
        if (count > 0) {
            feePrintPageTemplateList = feePrintPageTemplateV1InnerServiceSMOImpl.queryFeePrintPageTemplates(feePrintPageTemplateDto);
        } else {
            feePrintPageTemplateList = new ArrayList<>();
        }

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) feePrintPageTemplateDto.getRow()), count, feePrintPageTemplateList);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        return responseEntity;
    }

}
