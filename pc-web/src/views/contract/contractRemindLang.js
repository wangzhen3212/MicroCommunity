export const messages = {
  en: {
    contractRemind: {
      title: 'Expiration Reminder',
      subtitle: 'Sorted by remaining days, remind 30 / 15 / 7 days before expiration',
      filter: {
        keyword: 'Keyword',
        keywordPlaceholder: 'Contract name / code / party B',
        category: 'Contract Template',
        categoryPlaceholder: 'All templates',
        includeExpired: 'Include expired',
        days: 'Days Range',
        export: 'Export'
      },
      bulk: {
        selected: '{count} selected',
        push: 'Push Reminder',
        renew: 'Batch Renew',
        clear: 'Clear Selection'
      },
      table: {
        daysLeft: 'Days Left',
        code: 'Contract Code',
        name: 'Contract Name',
        category: 'Type',
        partyB: 'Party B',
        endTime: 'Expire Date',
        amount: 'Amount',
        operator: 'Operator',
        logs: 'Remind Records',
        operation: 'Operation'
      },
      op: {
        detail: 'Detail',
        renew: 'Renew',
        push: 'Push'
      },
      daysLeft: {
        expired: 'Expired {days} days',
        remain: '{days} days'
      },
      push: {
        title: 'Push Expiration Reminder',
        channels: 'Channels',
        inner: 'Inner Message',
        sms: 'SMS',
        confirm: 'Push',
        success: 'Pushed: {success}, failed: {fail}',
        error: 'Failed to push reminder',
        noSelection: 'Please select contracts first'
      },
      log: {
        manual: 'Manual',
        channelInner: 'Inner',
        channelSms: 'SMS',
        empty: 'No remind record'
      },
      empty: 'No contract expiring in the selected days',
      msg: {
        fetchError: 'Failed to load reminder list',
        exportSuccess: 'Export file is ready',
        exportError: 'Export failed'
      }
    }
  },
  zh: {
    contractRemind: {
      title: '到期提醒',
      subtitle: '按剩余天数升序，到期前 30 / 15 / 7 天提醒',
      filter: {
        keyword: '关键字',
        keywordPlaceholder: '合同名称 / 编号 / 签约方',
        category: '合同模板',
        categoryPlaceholder: '全部模板',
        includeExpired: '包含已到期',
        days: '提醒范围',
        export: '导出'
      },
      bulk: {
        selected: '已选 {count} 份',
        push: '批量推送提醒',
        renew: '批量续签',
        clear: '清空选择'
      },
      table: {
        daysLeft: '剩余天数',
        code: '合同编号',
        name: '合同名称',
        category: '类型',
        partyB: '签约对象',
        endTime: '到期日',
        amount: '金额',
        operator: '经办人',
        logs: '提醒记录',
        operation: '操作'
      },
      op: {
        detail: '详情',
        renew: '续签',
        push: '推送提醒'
      },
      daysLeft: {
        expired: '已逾期 {days} 天',
        remain: '剩余 {days} 天'
      },
      push: {
        title: '推送到期提醒',
        channels: '推送通道',
        inner: '站内信',
        sms: '短信',
        confirm: '立即推送',
        success: '推送成功 {success} 条，失败 {fail} 条',
        error: '推送提醒失败',
        noSelection: '请先选择合同'
      },
      log: {
        manual: '手动',
        channelInner: '站内信',
        channelSms: '短信',
        empty: '暂无提醒记录'
      },
      empty: '所选范围内暂无即将到期的合同',
      msg: {
        fetchError: '获取到期提醒清单失败',
        exportSuccess: '导出文件已生成',
        exportError: '导出失败'
      }
    }
  },
  'zh-TW': {
    contractRemind: {
      title: '到期提醒',
      subtitle: '按剩餘天數升序，到期前 30 / 15 / 7 天提醒',
      filter: {
        keyword: '關鍵字',
        keywordPlaceholder: '合同名稱 / 編號 / 簽約方',
        category: '合約模板',
        categoryPlaceholder: '全部模板',
        includeExpired: '包含已到期',
        days: '提醒範圍',
        export: '導出'
      },
      bulk: {
        selected: '已選 {count} 份',
        push: '批量推送提醒',
        renew: '批量續簽',
        clear: '清空選擇'
      },
      table: {
        daysLeft: '剩餘天數',
        code: '合同編號',
        name: '合同名稱',
        category: '類型',
        partyB: '簽約對象',
        endTime: '到期日',
        amount: '金額',
        operator: '經辦人',
        logs: '提醒記錄',
        operation: '操作'
      },
      op: {
        detail: '詳情',
        renew: '續簽',
        push: '推送提醒'
      },
      daysLeft: {
        expired: '已逾期 {days} 天',
        remain: '剩餘 {days} 天'
      },
      push: {
        title: '推送到期提醒',
        channels: '推送通道',
        inner: '站內信',
        sms: '短信',
        confirm: '立即推送',
        success: '推送成功 {success} 條，失敗 {fail} 條',
        error: '推送提醒失敗',
        noSelection: '請先選擇合同'
      },
      log: {
        manual: '手動',
        channelInner: '站內信',
        channelSms: '短信',
        empty: '暫無提醒記錄'
      },
      empty: '所選範圍內暫無即將到期的合同',
      msg: {
        fetchError: '獲取到期提醒清單失敗',
        exportSuccess: '導出文件已生成',
        exportError: '導出失敗'
      }
    }
  }
}
