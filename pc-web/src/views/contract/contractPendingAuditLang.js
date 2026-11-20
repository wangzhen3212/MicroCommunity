export const messages = {
  en: {
    contractPendingAudit: {
      title: 'Contracts Pending Review',
      subtitle: 'Legal review then leader approval, only staff with the button privilege can approve',
      filter: {
        keywordPlaceholder: 'Contract code / name / party B',
        auditType: 'Document Type',
        auditTypeAll: 'All Types',
        typeContract: 'Contract',
        typeChange: 'Change Order'
      },
      table: {
        code: 'Contract',
        partya: 'Party A',
        partyB: 'Party B',
        amount: 'Amount',
        state: 'Status',
        lastAudit: 'Latest Audit',
        operation: 'Operation'
      },
      op: {
        legalAudit: 'Legal Review',
        leaderAudit: 'Leader Approval',
        view: 'View',
        done: 'Completed',
        noPrivilege: 'No Permission'
      },
      record: {
        none: 'No audit record yet'
      },
      msg: {
        fetchError: 'Failed to load contracts pending review',
        noPrivilege: 'You have no permission to review this contract',
        success: 'Reviewed successfully',
        error: 'Review failed'
      },
      dialog: {
        title: 'Contract Review',
        contract: 'Contract',
        step: 'Current Step',
        history: 'Audit History',
        historyEmpty: 'No audit record yet',
        historyCollapse: 'Show / hide audit history',
        opinion: 'Opinion',
        opinionPlaceholder: 'Required when rejecting, up to 200 characters',
        opinionRequired: 'Opinion is required when rejecting',
        opinionMax: 'Opinion cannot exceed 200 characters',
        pass: 'Pass',
        reject: 'Reject',
        success: 'Reviewed successfully',
        error: 'Review failed'
      }
    },
    contractAuditFlow: {
      title: 'Approval Flow',
      legal: 'Legal Review',
      leader: 'Leader Approval',
      effective: 'Completed',
      current: 'Current',
      waiting: 'Waiting',
      auditor: 'Auditor',
      time: 'Time',
      opinion: 'Opinion',
      resultPass: 'Passed',
      resultReject: 'Rejected',
      history: 'Audit Records',
      historyEmpty: 'No audit record yet'
    }
  },
  zh: {
    contractPendingAudit: {
      title: '合同待审核',
      subtitle: '法务校对 → 领导审批，仅有对应按钮权限的员工可审核',
      filter: {
        keywordPlaceholder: '合同编号 / 名称 / 乙方',
        auditType: '单据类型',
        auditTypeAll: '全部类型',
        typeContract: '合同',
        typeChange: '变更单'
      },
      table: {
        code: '合同编号 / 名称',
        partya: '甲方',
        partyB: '乙方',
        amount: '金额',
        state: '状态',
        lastAudit: '最近审批',
        operation: '操作'
      },
      op: {
        legalAudit: '法务校对',
        leaderAudit: '领导审批',
        view: '查看',
        done: '已完成',
        noPrivilege: '无审核权限'
      },
      record: {
        none: '暂无审批记录'
      },
      msg: {
        fetchError: '查询待审核合同失败',
        noPrivilege: '您没有该合同的审核权限',
        success: '审核成功',
        error: '审核失败'
      },
      dialog: {
        title: '合同审核',
        contract: '合同',
        step: '当前环节',
        history: '审批记录',
        historyEmpty: '暂无审批记录',
        historyCollapse: '展开 / 收起审批记录',
        opinion: '审核意见',
        opinionPlaceholder: '驳回时必填，最多 200 字',
        opinionRequired: '驳回时必须填写审核意见',
        opinionMax: '审核意见不能超过 200 字',
        pass: '通过',
        reject: '驳回',
        success: '审核成功',
        error: '审核失败'
      }
    },
    contractAuditFlow: {
      title: '审批流程',
      legal: '法务校对',
      leader: '领导审批',
      effective: '完成',
      current: '当前',
      waiting: '待审批',
      auditor: '审核人',
      time: '时间',
      opinion: '意见',
      resultPass: '通过',
      resultReject: '驳回',
      history: '审批记录',
      historyEmpty: '暂无审批记录'
    }
  },
  'zh-TW': {
    contractPendingAudit: {
      title: '合同待審核',
      subtitle: '法務校對 → 領導審批，僅有對應按鈕權限的員工可審核',
      filter: {
        keywordPlaceholder: '合同編號 / 名稱 / 乙方',
        auditType: '單據類型',
        auditTypeAll: '全部類型',
        typeContract: '合同',
        typeChange: '變更單'
      },
      table: {
        code: '合同編號 / 名稱',
        partya: '甲方',
        partyB: '乙方',
        amount: '金額',
        state: '狀態',
        lastAudit: '最近審批',
        operation: '操作'
      },
      op: {
        legalAudit: '法務校對',
        leaderAudit: '領導審批',
        view: '查看',
        done: '已完成',
        noPrivilege: '無審核權限'
      },
      record: {
        none: '暫無審批記錄'
      },
      msg: {
        fetchError: '查詢待審核合同失敗',
        noPrivilege: '您沒有該合同的審核權限',
        success: '審核成功',
        error: '審核失敗'
      },
      dialog: {
        title: '合同審核',
        contract: '合同',
        step: '當前環節',
        history: '審批記錄',
        historyEmpty: '暫無審批記錄',
        historyCollapse: '展開 / 收起審批記錄',
        opinion: '審核意見',
        opinionPlaceholder: '駁回時必填，最多 200 字',
        opinionRequired: '駁回時必須填寫審核意見',
        opinionMax: '審核意見不能超過 200 字',
        pass: '通過',
        reject: '駁回',
        success: '審核成功',
        error: '審核失敗'
      }
    },
    contractAuditFlow: {
      title: '審批流程',
      legal: '法務校對',
      leader: '領導審批',
      effective: '完成',
      current: '當前',
      waiting: '待審批',
      auditor: '審核人',
      time: '時間',
      opinion: '意見',
      resultPass: '通過',
      resultReject: '駁回',
      history: '審批記錄',
      historyEmpty: '暫無審批記錄'
    }
  }
}
