package com.java110.fee.api;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.fee.FeePrintPageTemplateDto;
import com.java110.fee.bmo.feePrintPageTemplate.IDeleteFeePrintPageTemplateBMO;
import com.java110.fee.bmo.feePrintPageTemplate.IGetFeePrintPageTemplateBMO;
import com.java110.fee.bmo.feePrintPageTemplate.ISaveFeePrintPageTemplateBMO;
import com.java110.fee.bmo.feePrintPageTemplate.IUpdateFeePrintPageTemplateBMO;
import com.java110.po.fee.FeePrintPageTemplatePo;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/feePrintPageTemplate")
public class FeePrintPageTemplateApi {

    @Autowired
    private ISaveFeePrintPageTemplateBMO saveFeePrintPageTemplateBMOImpl;

    @Autowired
    private IUpdateFeePrintPageTemplateBMO updateFeePrintPageTemplateBMOImpl;

    @Autowired
    private IDeleteFeePrintPageTemplateBMO deleteFeePrintPageTemplateBMOImpl;

    @Autowired
    private IGetFeePrintPageTemplateBMO getFeePrintPageTemplateBMOImpl;

    /**
     * 保存打印模板
     *
     * @param reqJson
     * @return
     * @serviceCode /feePrintPageTemplate/saveFeePrintPageTemplate
     * @path /app/feePrintPageTemplate/saveFeePrintPageTemplate
     */
    @RequestMapping(value = "/saveFeePrintPageTemplate", method = RequestMethod.POST)
    public ResponseEntity<String> saveFeePrintPageTemplate(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        FeePrintPageTemplatePo feePrintPageTemplatePo = BeanConvertUtil.covertBean(reqJson, FeePrintPageTemplatePo.class);
        return saveFeePrintPageTemplateBMOImpl.save(feePrintPageTemplatePo);
    }

    /**
     * 修改打印模板
     *
     * @param reqJson
     * @return
     * @serviceCode /feePrintPageTemplate/updateFeePrintPageTemplate
     * @path /app/feePrintPageTemplate/updateFeePrintPageTemplate
     */
    @RequestMapping(value = "/updateFeePrintPageTemplate", method = RequestMethod.POST)
    public ResponseEntity<String> updateFeePrintPageTemplate(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "templateId", "templateId不能为空");
        FeePrintPageTemplatePo feePrintPageTemplatePo = BeanConvertUtil.covertBean(reqJson, FeePrintPageTemplatePo.class);
        return updateFeePrintPageTemplateBMOImpl.update(feePrintPageTemplatePo);
    }

    /**
     * 删除打印模板
     *
     * @param reqJson
     * @return
     * @serviceCode /feePrintPageTemplate/deleteFeePrintPageTemplate
     * @path /app/feePrintPageTemplate/deleteFeePrintPageTemplate
     */
    @RequestMapping(value = "/deleteFeePrintPageTemplate", method = RequestMethod.POST)
    public ResponseEntity<String> deleteFeePrintPageTemplate(@RequestBody JSONObject reqJson) {
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
        Assert.hasKeyAndValue(reqJson, "templateId", "templateId不能为空");
        FeePrintPageTemplatePo feePrintPageTemplatePo = BeanConvertUtil.covertBean(reqJson, FeePrintPageTemplatePo.class);
        return deleteFeePrintPageTemplateBMOImpl.delete(feePrintPageTemplatePo);
    }

    /**
     * 查询打印模板
     *
     * @param communityId 小区ID
     * @return
     * @serviceCode /feePrintPageTemplate/listFeePrintPageTemplate
     * @path /app/feePrintPageTemplate/listFeePrintPageTemplate
     */
    @RequestMapping(value = "/listFeePrintPageTemplate", method = RequestMethod.GET)
    public ResponseEntity<String> listFeePrintPageTemplate(@RequestParam(value = "communityId") String communityId,
                                                           @RequestParam(value = "page") int page,
                                                           @RequestParam(value = "row") int row) {
        FeePrintPageTemplateDto feePrintPageTemplateDto = new FeePrintPageTemplateDto();
        feePrintPageTemplateDto.setPage(page);
        feePrintPageTemplateDto.setRow(row);
        feePrintPageTemplateDto.setCommunityId(communityId);
        return getFeePrintPageTemplateBMOImpl.get(feePrintPageTemplateDto);
    }
}
