export const messages = {
  en: {
    contractAuditCenter: {
      title: 'Contract Audit Center',
      subtitle: 'Fixed two-level approval: Legal Review then Leader Approval',
      stat: {
        pending: 'Pending My Approval',
        monthPass: 'Passed This Month',
        monthReject: 'Rejected This Month'
      },
      tab: {
        pending: 'Pending My Approval',
        pass: 'Passed',
        reject: 'Rejected',
        mine: 'Submitted by Me'
      },
      filter: {
        keyword: 'Keyword',
        keywordPlaceholder: 'Approval code / contract code / name',
        search: 'Search',
        reset: 'Reset',
        auditType: 'Document Type',
        auditTypeAll: 'All Types'
      },
      table: {
        auditCode: 'Approval Code',
        contract: 'Contract',
        contractType: 'Contract Template',
        type: 'Type',
        amount: 'Amount',
        partyB: 'Party B',
        submitter: 'Submitter',
        submitTime: 'Submit Time',
        currentStep: 'Current Step',
        state: 'Status',
        operation: 'Operation'
      },
      op: {
        audit: 'Approve',
        cancel: 'Withdraw',
        view: 'View',
        flow: 'Approval Chain'
      },
      typeContract: 'Contract',
      typeChange: 'Change',
      stepLegal: 'Legal Review',
      stepLeader: 'Leader Approval',
      stateDoing: 'Approving',
      statePass: 'Passed',
      stateReject: 'Rejected',
      stateCancel: 'Withdrawn',
      resultDoing: 'Pending',
      resultPass: 'Passed',
      resultReject: 'Rejected',
      auditorConfig: 'Auditor Settings',
      ruleSetting: 'Approval Rule Settings',
      nodeTip: 'Approval nodes: Applicant submits -> Legal Review -> Leader Approval',
      dialog: {
        title: 'Approval',
        pass: 'Pass',
        reject: 'Reject',
        opinion: 'Opinion',
        opinionPlaceholder: 'Please enter approval opinion',
        opinionRequired: 'Opinion is required when rejecting',
        opinionMax: 'Opinion cannot exceed 200 characters',
        submit: 'Confirm',
        success: 'Approval finished',
        error: 'Approval failed',
        cancelConfirm: 'Withdraw this approval?',
        cancelSuccess: 'Approval withdrawn'
      },
      config: {
        title: 'Auditor Settings',
        tip: 'Legal reviewer and leader reviewer are required, multiple alternatives supported',
        legal: 'Legal Reviewer',
        leader: 'Leader Reviewer',
        add: 'Add Auditor',
        staff: 'Staff',
        staffPlaceholder: 'Select staff',
        save: 'Save',
        success: 'Auditor settings saved',
        error: 'Failed to save auditor settings',
        empty: 'Not configured',
        remove: 'Remove'
      },
      empty: 'No approval record',
      msg: {
        noPrivilege: 'You have no permission to approve this document',
        fetchError: 'Failed to load approval list',
        statError: 'Failed to load audit statistics',
        flowError: 'Failed to load approval chain'
      }
    },
    contractAuditFlow: {
      title: 'Approval Flow',
      submitter: 'Submitter',
      legal: 'Legal Review',
      leader: 'Leader Approval',
      effective: 'Effective',
      current: 'Current',
      waiting: 'Waiting',
      noStart: 'Not submitted',
      auditor: 'Auditor',
      time: 'Time',
      opinion: 'Opinion'
    }
  },
  zh: {
    contractAuditCenter: {
      title: '合同审核中心',
      subtitle: '固定两级审批：法务校对 → 领导审核',
      stat: {
        pending: '待我审批',
        monthPass: '本月已通过',
        monthReject: '本月驳回'
      },
      tab: {
        pending: '待我审批',
        pass: '已通过',
        reject: '已驳回',
        mine: '我发起的'
      },
      filter: {
        keyword: '关键字',
        keywordPlaceholder: '审批单号 / 合同编号 / 名称',
        search: '查询',
        reset: '重置',
        auditType: '单据类型',
        auditTypeAll: '全部类型'
      },
      table: {
        auditCode: '审批单号',
        contract: '合同',
        contractType: '合同模板',
        type: '类型',
        amount: '金额',
        partyB: '签约对象',
        submitter: '提交人',
        submitTime: '提交时间',
        currentStep: '当前节点',
        state: '状态',
        operation: '操作'
      },
      op: {
        audit: '审批',
        cancel: '撤回',
        view: '查看',
        flow: '审批链'
      },
      typeContract: '合同',
      typeChange: '变更',
      stepLegal: '法务校对',
      stepLeader: '领导审核',
      stateDoing: '审批中',
      statePass: '已通过',
      stateReject: '已驳回',
      stateCancel: '已撤回',
      resultDoing: '待处理',
      resultPass: '已通过',
      resultReject: '已驳回',
      auditorConfig: '审核人设置',
      ruleSetting: '审批规则设置',
      nodeTip: '审核节点：经办人提交 → 法务校对 → 领导审核',
      dialog: {
        title: '审批操作',
        pass: '通过',
        reject: '驳回',
        opinion: '审批意见',
        opinionPlaceholder: '请输入审批意见',
        opinionRequired: '驳回时必须填写审批意见',
        opinionMax: '审批意见不能超过 200 字',
        submit: '确定',
        success: '审批完成',
        error: '审批失败',
        cancelConfirm: '确定撤回该审批？',
        cancelSuccess: '审批已撤回'
      },
      config: {
        title: '审核人设置',
        tip: '法务校对与领导审核各至少配置 1 人，支持配置多备选',
        legal: '法务审核人',
        leader: '领导审核人',
        add: '添加审核人',
        staff: '审核人',
        staffPlaceholder: '请选择员工',
        save: '保存',
        success: '审核人设置已保存',
        error: '审核人设置保存失败',
        empty: '未配置',
        remove: '移除'
      },
      empty: '暂无审批记录',
      msg: {
        noPrivilege: '您没有该单据的审批权限',
        fetchError: '获取审批列表失败',
        statError: '获取审核统计失败',
        flowError: '获取审批链失败'
      }
    },
    contractAuditFlow: {
      title: '审批流程',
      submitter: '经办人提交',
      legal: '法务校对',
      leader: '领导审核',
      effective: '生效',
      current: '当前',
      waiting: '待处理',
      noStart: '未提交',
      auditor: '审核人',
      time: '时间',
      opinion: '意见'
    }
  },
  'zh-TW': {
    contractAuditCenter: {
      title: '合同審核中心',
      subtitle: '固定兩級審批：法務校對 → 領導審核',
      stat: {
        pending: '待我審批',
        monthPass: '本月已通過',
        monthReject: '本月駁回'
      },
      tab: {
        pending: '待我審批',
        pass: '已通過',
        reject: '已駁回',
        mine: '我發起的'
      },
      filter: {
        keyword: '關鍵字',
        keywordPlaceholder: '審批單號 / 合同編號 / 名稱',
        search: '查詢',
        reset: '重置',
        auditType: '單據類型',
        auditTypeAll: '全部類型'
      },
      table: {
        auditCode: '審批單號',
        contract: '合同',
        contractType: '合同模板',
        type: '類型',
        amount: '金額',
        partyB: '簽約對象',
        submitter: '提交人',
        submitTime: '提交時間',
        currentStep: '當前節點',
        state: '狀態',
        operation: '操作'
      },
      op: {
        audit: '審批',
        cancel: '撤回',
        view: '查看',
        flow: '審批鏈'
      },
      typeContract: '合同',
      typeChange: '變更',
      stepLegal: '法務校對',
      stepLeader: '領導審核',
      stateDoing: '審批中',
      statePass: '已通過',
      stateReject: '已駁回',
      stateCancel: '已撤回',
      resultDoing: '待處理',
      resultPass: '已通過',
      resultReject: '已駁回',
      auditorConfig: '審核人設置',
      ruleSetting: '審批規則設置',
      nodeTip: '審核節點：經辦人提交 → 法務校對 → 領導審核',
      dialog: {
        title: '審批操作',
        pass: '通過',
        reject: '駁回',
        opinion: '審批意見',
        opinionPlaceholder: '請輸入審批意見',
        opinionRequired: '駁回時必須填寫審批意見',
        opinionMax: '審批意見不能超過 200 字',
        submit: '確定',
        success: '審批完成',
        error: '審批失敗',
        cancelConfirm: '確定撤回該審批？',
        cancelSuccess: '審批已撤回'
      },
      config: {
        title: '審核人設置',
        tip: '法務校對與領導審核各至少配置 1 人，支持配置多備選',
        legal: '法務審核人',
        leader: '領導審核人',
        add: '添加審核人',
        staff: '審核人',
        staffPlaceholder: '請選擇員工',
        save: '保存',
        success: '審核人設置已保存',
        error: '審核人設置保存失敗',
        empty: '未配置',
        remove: '移除'
      },
      empty: '暫無審批記錄',
      msg: {
        noPrivilege: '您沒有該單據的審批權限',
        fetchError: '獲取審批列表失敗',
        statError: '獲取審核統計失敗',
        flowError: '獲取審批鏈失敗'
      }
    },
    contractAuditFlow: {
      title: '審批流程',
      submitter: '經辦人提交',
      legal: '法務校對',
      leader: '領導審核',
      effective: '生效',
      current: '當前',
      waiting: '待處理',
      noStart: '未提交',
      auditor: '審核人',
      time: '時間',
      opinion: '意見'
    }
  }
}
