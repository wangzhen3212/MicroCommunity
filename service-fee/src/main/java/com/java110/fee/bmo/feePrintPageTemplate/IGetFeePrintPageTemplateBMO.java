package com.java110.fee.bmo.feePrintPageTemplate;

import com.java110.dto.fee.FeePrintPageTemplateDto;
import org.springframework.http.ResponseEntity;

public interface IGetFeePrintPageTemplateBMO {

    /**
     * 查询打印模板
     * add by wuxw
     *
     * @param feePrintPageTemplateDto
     * @return
     */
    ResponseEntity<String> get(FeePrintPageTemplateDto feePrintPageTemplateDto);

}
