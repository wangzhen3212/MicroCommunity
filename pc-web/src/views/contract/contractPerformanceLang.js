export const messages = {
  en: {
    contractPerformance: {
      title: 'Contract Performance',
      subtitle: 'Purchase / maintenance contracts tracked by payment nodes; house lease rent is billed in Fee Management',
      stat: {
        performing: 'Performing Contracts',
        performingExtra: 'Contracts with payment plans',
        paid: 'Paid Amount',
        paidExtra: 'Total of paid periods',
        unpaid: 'Unpaid Amount',
        unpaidExtra: 'Total of unpaid periods',
        overdue: 'Overdue Periods',
        overdueExtra: 'Nodes passed the plan date'
      },
      filter: {
        keyword: 'Keyword',
        keywordPlaceholder: 'Contract name / code / party B',
        category: 'Contract Type',
        categoryPlaceholder: 'Purchase / Maintenance',
        payState: 'Performance State',
        payStatePlaceholder: 'All states',
        export: 'Export'
      },
      payState: {
        doing: 'Unpaid',
        paid: 'Paid off',
        overdue: 'Overdue'
      },
      legend: {
        paid: 'Paid',
        due: 'Due soon',
        overdue: 'Overdue',
        future: 'Not due'
      },
      table: {
        code: 'Contract Code',
        name: 'Contract Name',
        payMode: 'Payment Mode',
        progress: 'Paid Progress',
        plan: 'Payment Nodes',
        next: 'Next Node',
        state: 'Performance State',
        operation: 'Operation'
      },
      op: {
        detail: 'Detail',
        pay: 'Register Payment'
      },
      dialog: {
        title: 'Register Payment',
        planName: 'Period',
        planAmount: 'Plan Amount',
        payAmount: 'Paid Amount',
        payTime: 'Paid Date',
        voucher: 'Voucher',
        remark: 'Remark',
        submit: 'Confirm',
        cancelPay: 'Cancel Payment',
        required: 'Please fill the paid amount',
        success: 'Payment registered',
        fail: 'Failed to register payment',
        cancelSuccess: 'Payment cancelled'
      },
      empty: 'No performance data',
      msg: {
        fetchError: 'Failed to load performance data',
        exportSuccess: 'Export file is ready',
        exportError: 'Export failed'
      }
    }
  },
  zh: {
    contractPerformance: {
      title: '合同履约',
      subtitle: '采购 / 维保合同按付款节点跟踪；房屋出租租金仍由「收费管理」出账',
      stat: {
        performing: '履约中合同数',
        performingExtra: '已配置收款计划的合同',
        paid: '已付金额',
        paidExtra: '已登记付款期次合计',
        unpaid: '待付金额',
        unpaidExtra: '未付期次合计',
        overdue: '逾期节点数',
        overdueExtra: '已过计划日期仍未付款'
      },
      filter: {
        keyword: '关键字',
        keywordPlaceholder: '合同名称 / 编号 / 签约方',
        category: '合同类型',
        categoryPlaceholder: '采购 / 维保',
        payState: '履约状态',
        payStatePlaceholder: '全部状态',
        export: '导出'
      },
      payState: {
        doing: '未付清',
        paid: '已付清',
        overdue: '存在逾期'
      },
      legend: {
        paid: '已付',
        due: '即将到期',
        overdue: '逾期',
        future: '未到期'
      },
      table: {
        code: '合同编号',
        name: '合同名称',
        payMode: '付款方式',
        progress: '已付进度',
        plan: '付款节点',
        next: '下一节点',
        state: '履约状态',
        operation: '操作'
      },
      op: {
        detail: '详情',
        pay: '登记付款'
      },
      dialog: {
        title: '登记付款',
        planName: '期次',
        planAmount: '计划金额',
        payAmount: '实付金额',
        payTime: '实付日期',
        voucher: '凭证附件',
        remark: '备注',
        submit: '确认',
        cancelPay: '撤销付款',
        required: '请填写实付金额',
        success: '付款登记成功',
        fail: '付款登记失败',
        cancelSuccess: '付款已撤销'
      },
      empty: '暂无履约数据',
      msg: {
        fetchError: '获取履约数据失败',
        exportSuccess: '导出文件已生成',
        exportError: '导出失败'
      }
    }
  },
  'zh-TW': {
    contractPerformance: {
      title: '合同履約',
      subtitle: '採購 / 維保合同按付款節點跟蹤；房屋出租租金仍由「收費管理」出賬',
      stat: {
        performing: '履約中合同數',
        performingExtra: '已配置收款計劃的合同',
        paid: '已付金額',
        paidExtra: '已登記付款期次合計',
        unpaid: '待付金額',
        unpaidExtra: '未付期次合計',
        overdue: '逾期節點數',
        overdueExtra: '已過計劃日期仍未付款'
      },
      filter: {
        keyword: '關鍵字',
        keywordPlaceholder: '合同名稱 / 編號 / 簽約方',
        category: '合同類型',
        categoryPlaceholder: '採購 / 維保',
        payState: '履約狀態',
        payStatePlaceholder: '全部狀態',
        export: '導出'
      },
      payState: {
        doing: '未付清',
        paid: '已付清',
        overdue: '存在逾期'
      },
      legend: {
        paid: '已付',
        due: '即將到期',
        overdue: '逾期',
        future: '未到期'
      },
      table: {
        code: '合同編號',
        name: '合同名稱',
        payMode: '付款方式',
        progress: '已付進度',
        plan: '付款節點',
        next: '下一節點',
        state: '履約狀態',
        operation: '操作'
      },
      op: {
        detail: '詳情',
        pay: '登記付款'
      },
      dialog: {
        title: '登記付款',
        planName: '期次',
        planAmount: '計劃金額',
        payAmount: '實付金額',
        payTime: '實付日期',
        voucher: '憑證附件',
        remark: '備註',
        submit: '確認',
        cancelPay: '撤銷付款',
        required: '請填寫實付金額',
        success: '付款登記成功',
        fail: '付款登記失敗',
        cancelSuccess: '付款已撤銷'
      },
      empty: '暫無履約數據',
      msg: {
        fetchError: '獲取履約數據失敗',
        exportSuccess: '導出文件已生成',
        exportError: '導出失敗'
      }
    }
  }
}
