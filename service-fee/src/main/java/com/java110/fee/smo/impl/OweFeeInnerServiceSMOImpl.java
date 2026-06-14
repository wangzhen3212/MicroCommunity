package com.java110.fee.smo.impl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.PageDto;
import com.java110.dto.fee.*;
import com.java110.fee.bmo.impl.QueryOweFeeImpl;
import com.java110.fee.dao.IFeeAttrServiceDao;
import com.java110.fee.dao.IFeeServiceDao;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IOweFeeInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.fee.PayFeePo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName FloorInnerServiceSMOImpl
 * @Description 费用内部服务实现类
 * @Author wuxw
 * @Date 2019/4/24 9:20
 * @Version 1.0
 * add by wuxw 2019/4/24
 **/
@RestController
public class OweFeeInnerServiceSMOImpl extends BaseServiceSMO implements IOweFeeInnerServiceSMO {
    private final static Logger logger = LoggerFactory.getLogger(OweFeeInnerServiceSMOImpl.class);

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    //域
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    //键
    public static final String TOTAL_FEE_PRICE = "TOTAL_FEE_PRICE";

    //键
    public static final String RECEIVED_AMOUNT_SWITCH = "RECEIVED_AMOUNT_SWITCH";

    //禁用电脑端提交收费按钮
    public static final String OFFLINE_PAY_FEE_SWITCH = "OFFLINE_PAY_FEE_SWITCH";


    @Override
    public List<FeeDto> queryOwnerOweFee(@RequestBody FeeDto feeDto) {
        feeDto.setState(FeeDto.STATE_DOING);
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        if (ListUtil.isNull(feeDtos)) {
            feeDtos = new ArrayList<>();
            return feeDtos;
        }
        String val = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), TOTAL_FEE_PRICE);
        if (StringUtil.isEmpty(val)) {
            val = MappingCache.getValue(DOMAIN_COMMON, TOTAL_FEE_PRICE);
        }
        List<FeeDto> tmpFeeDtos = new ArrayList<>();
        for (FeeDto tmpFeeDto : feeDtos) {
            try {
                if (!StringUtil.isEmpty(tmpFeeDto.getAdditionalAmount())) {
                    tmpFeeDto.setAdditionalAmount(Double.parseDouble(tmpFeeDto.getAdditionalAmount()) + "");
                }
                if (!StringUtil.isEmpty(tmpFeeDto.getSquarePrice())) {
                    tmpFeeDto.setSquarePrice(Double.parseDouble(tmpFeeDto.getSquarePrice()) + "");
                }

                //todo 有目标结束时间,并且不是一次性费用
                if (!StringUtil.isEmpty(feeDto.getTargetEndTime())
                        && !FeeDto.FEE_FLAG_ONCE.equals(tmpFeeDto.getFeeFlag())
                ) {
                    computeFeeSMOImpl.computeEveryOweFeeByTargetEndTime(tmpFeeDto, feeDto.getTargetEndTime());//计算欠费金额
                } else {
                    computeFeeSMOImpl.computeEveryOweFee(tmpFeeDto);//计算欠费金额
                }
                //如果金额为0 就排除
                tmpFeeDto.setFeeTotalPrice(
                        MoneyUtil.computePriceScale(
                                tmpFeeDto.getFeeTotalPrice(),
                                tmpFeeDto.getScale(),
                                Integer.parseInt(tmpFeeDto.getDecimalPlace())
                        )
                );

                tmpFeeDto.setVal(val);
                if (tmpFeeDto.getFeeTotalPrice() != 0) {
                    tmpFeeDtos.add(tmpFeeDto);
                }
            } catch (Exception e) {
                logger.error("可能费用资料有问题导致算费失败", e);
            }
        }

        return tmpFeeDtos;
    }
}
