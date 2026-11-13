package com.java110.user.bmo.owner.impl;

import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.payFee.PayFeeRuleDto;
import com.java110.intf.fee.IPayFeeRuleV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.user.bmo.owner.IOwnerFeeSyncBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OwnerFeeSyncBMOImpl implements IOwnerFeeSyncBMO {

    private static final Logger logger = LoggerFactory.getLogger(OwnerFeeSyncBMOImpl.class);

    @Autowired
    private IPayFeeRuleV1InnerServiceSMO payFeeRuleV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Override
    public int syncOwnerToFee(OwnerDto oldOwner, OwnerDto newOwner) {
        if (oldOwner == null || newOwner == null) {
            return 0;
        }
        String oldOwnerId = oldOwner.getOwnerId();
        if (StringUtil.isEmpty(oldOwnerId)) {
            return 0;
        }
        // 家庭成员与业主共用 owner_id，仅业主本人的记录才同步费用数据
        if (!StringUtil.isEmpty(oldOwner.getMemberId()) && !oldOwnerId.equals(oldOwner.getMemberId())) {
            return 0;
        }
        String newOwnerId = StringUtil.isEmpty(newOwner.getOwnerId()) ? oldOwnerId : newOwner.getOwnerId();
        String newOwnerName = newOwner.getName();
        String communityId = StringUtil.isEmpty(newOwner.getCommunityId()) ? oldOwner.getCommunityId() : newOwner.getCommunityId();
        if (StringUtil.isEmpty(communityId)) {
            return 0;
        }
        boolean ownerIdChanged = !oldOwnerId.equals(newOwnerId);
        boolean ownerNameChanged = !StringUtil.isEmpty(newOwnerName) && !newOwnerName.equals(oldOwner.getName());
        if (!ownerIdChanged && !ownerNameChanged) {
            return 0;
        }

        int ruleCount;
        int feeCount;
        try {
            PayFeeRuleDto payFeeRuleDto = new PayFeeRuleDto();
            payFeeRuleDto.setCommunityId(communityId);
            payFeeRuleDto.setOwnerId(newOwnerId);
            payFeeRuleDto.setOldOwnerId(oldOwnerId);
            if (ownerNameChanged) {
                payFeeRuleDto.setOwnerName(newOwnerName);
            }
            ruleCount = payFeeRuleV1InnerServiceSMOImpl.updatePayFeeRuleOwner(payFeeRuleDto);

            PayFeeDto payFeeDto = new PayFeeDto();
            payFeeDto.setCommunityId(communityId);
            payFeeDto.setOwnerId(newOwnerId);
            payFeeDto.setOldOwnerId(oldOwnerId);
            if (ownerNameChanged) {
                payFeeDto.setOwnerName(newOwnerName);
            }
            feeCount = payFeeV1InnerServiceSMOImpl.updatePayFeeOwner(payFeeDto);
        } catch (Exception e) {
            logger.error("业主费用信息同步失败，communityId：{}，oldOwnerId：{}，newOwnerId：{}", communityId, oldOwnerId, newOwnerId, e);
            throw new CmdException("业主费用信息同步失败，请重试");
        }

        logger.info("业主费用信息同步完成，communityId：{}，oldOwnerId：{}，newOwnerId：{}，ownerName：{}，规则 {} 行，账单 {} 行",
                communityId, oldOwnerId, newOwnerId, newOwnerName, ruleCount, feeCount);

        return ruleCount + feeCount;
    }
}
