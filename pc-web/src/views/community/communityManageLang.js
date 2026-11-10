export const messages = {
  en: {
    communityManage: {
      search: {
        title: 'Search Conditions',
        communityId: 'Please enter community ID',
        name: 'Please enter community name'
      },
      list: {
        title: 'Community Information',
        addCommunity: 'Add Community'
      },
      table: {
        communityId: 'Community ID',
        name: 'Community Name',
        storeName: 'Property Company',
        nearbyLandmarks: 'Nearby Landmarks',
        cityCode: 'City Code',
        createTime: 'Create Time',
        state: 'Status',
        syncIot: 'Sync IOT'
      },
      card: {
        normal: 'Normal',
        incomplete: 'Incomplete',
        store: 'Property',
        address: 'Address',
        tel: 'Service',
        attr: 'Attributes',
        attrUnit: 'items',
        code: 'No.',
        unlinked: 'No property company',
        unfilled: 'Not maintained',
        unnamed: 'Unnamed community',
        locationEmpty: 'Area not maintained',
        empty: 'No matched community, try adjusting filters',
        clearFilter: 'Clear Filters'
      },
      filterBar: {
        namePlaceholder: 'Please enter community name',
        storePlaceholder: 'All property companies',
        page: 'This page',
        total: 'Total',
        hint: '{page} of {total} communities',
        totalCount: '{total} in total'
      },
      drawer: {
        title: 'Community Detail',
        tabCompany: 'Property Company',
        tabAttr: 'Community Attributes',
        company: 'Property Company',
        companyEmpty: 'No property company linked to this community',
        companyNotFound: 'Property company not found',
        companyFetchError: 'Failed to query property company',
        service: 'In service',
        contact: 'Contact',
        attrEmpty: 'No attributes maintained for this community',
        attrTotal: '{total} attributes maintained',
        specName: 'Attribute',
        specCd: 'Spec Code',
        value: 'Value',
        listShow: 'List Show',
        show: 'Show',
        notShow: 'Hide',
        updatedAt: 'Updated at',
        code: 'Community No.',
        communityId: 'Community No.',
        store: 'Property Company',
        state: 'Audit State',
        city: 'City',
        address: 'Address',
        landmarks: 'Nearby Landmarks',
        tel: 'Service Tel',
        cityCode: 'City Code',
        companyId: 'Company No.',
        admin: 'Admin Account',
        corporation: 'Corporation',
        foundingTime: 'Founding Date',
        loginState: 'Login State',
        allowLogin: 'Allowed',
        limitLogin: 'Restricted',
        companyAddress: 'Company Address',
        companyName: 'Company Name'
      },
      fetchError: 'Failed to query community list'
    },
    addCommunity: {
      title: 'Add Community',
      name: 'Community Name',
      namePlaceholder: 'Required, please enter community name',
      area: 'Community Area',
      address: 'Community Address',
      addressPlaceholder: 'Required, please enter community address',
      nearbyLandmarks: 'Nearby Landmarks',
      nearbyLandmarksPlaceholder: 'Required, please enter nearby landmarks',
      tel: 'Customer Service Phone',
      telPlaceholder: 'Required, please enter customer service phone',
      payFeeMonth: 'Payment Cycle',
      payFeeMonthPlaceholder: 'Required, please enter payment cycle (months)',
      feePrice: 'Monthly Price',
      feePricePlaceholder: 'Required, please enter monthly price (yuan)',
      saveSuccess: 'Community added successfully',
      validate: {
        name: 'Community name cannot be empty',
        cityCode: 'City code cannot be empty',
        address: 'Community address cannot be empty',
        nearbyLandmarks: 'Nearby landmarks cannot be empty',
        tel: 'Customer service phone cannot be empty'
      }
    },
    editCommunity: {
      title: 'Edit Community',
      name: 'Community Name',
      namePlaceholder: 'Required, please enter community name',
      address: 'Community Address',
      addressPlaceholder: 'Required, please enter community address',
      nearbyLandmarks: 'Nearby Landmarks',
      nearbyLandmarksPlaceholder: 'Required, please enter nearby landmarks',
      payFeeMonth: 'Payment Cycle',
      payFeeMonthPlaceholder: 'Required, please enter payment cycle (months)',
      feePrice: 'Monthly Price',
      feePricePlaceholder: 'Required, please enter monthly price (yuan)',
      tel: 'Customer Service Phone',
      telPlaceholder: 'Required, please enter customer service phone',
      saveSuccess: 'Community updated successfully',
      validate: {
        name: 'Community name cannot be empty',
        address: 'Community address cannot be empty',
        nearbyLandmarks: 'Nearby landmarks cannot be empty',
        communityId: 'Community ID cannot be empty'
      }
    },
    deleteCommunity: {
      title: 'Confirm Operation',
      confirmText: 'Confirm to delete the community?',
      cancel: 'Cancel',
      confirm: 'Confirm Delete',
      deleteSuccess: 'Community deleted successfully'
    },
    communityDataToIot: {
      title: 'Confirm Operation',
      confirmText: 'Sync owner information (house information associated with owners), vehicle information and staff information in the community',
      cancel: 'Cancel',
      confirm: 'Confirm Sync',
      syncSuccess: 'Sync submitted successfully'
    },
    recallAuditFinishCommunity: {
      title: 'Confirm Operation',
      confirmText: 'Confirm to recall the audit?',
      cancel: 'Cancel',
      confirm: 'Confirm Recall'
    },
    areaSelect: {
      province: 'Select Province',
      city: 'Select City',
      area: 'Select Area',
      fetchError: 'Failed to query area'
    }
  },
  zh: {
    communityManage: {
      search: {
        title: '查询条件',
        communityId: '请输入小区ID',
        name: '请输入小区名称'
      },
      list: {
        title: '小区信息',
        addCommunity: '添加小区'
      },
      table: {
        communityId: '小区ID',
        name: '小区名称',
        storeName: '物业公司',
        nearbyLandmarks: '附近地标',
        cityCode: '城市编码',
        createTime: '创建时间',
        state: '状态',
        syncIot: '同步IOT'
      },
      card: {
        normal: '正常',
        incomplete: '待完善',
        store: '物业',
        address: '地址',
        tel: '客服',
        attr: '属性',
        attrUnit: '项',
        code: '编号',
        unlinked: '未关联物业公司',
        unfilled: '未维护',
        unnamed: '未命名小区',
        locationEmpty: '地区未维护',
        empty: '没有匹配的小区，试试调整筛选条件',
        clearFilter: '清除筛选'
      },
      filterBar: {
        namePlaceholder: '请输入小区名称',
        storePlaceholder: '全部物业公司',
        page: '本页',
        total: '合计',
        hint: '共 {page} / {total} 个小区',
        totalCount: '共 {total} 条'
      },
      drawer: {
        title: '小区详情',
        tabCompany: '物业公司',
        tabAttr: '小区属性',
        company: '所属物业公司',
        companyEmpty: '该小区暂未关联物业公司',
        companyNotFound: '未查询到物业公司信息',
        companyFetchError: '物业公司信息查询失败',
        service: '正常服务',
        contact: '联系方式',
        attrEmpty: '该小区暂未维护属性信息',
        attrTotal: '共 {total} 项小区属性',
        specName: '属性名称',
        specCd: '规格编码',
        value: '属性值',
        listShow: '列表展示',
        show: '展示',
        notShow: '不展示',
        updatedAt: '数据更新于',
        code: '小区编号',
        communityId: '小区编号',
        store: '物业公司',
        state: '审核状态',
        city: '所在城市',
        address: '详细地址',
        landmarks: '周边地标',
        tel: '客服电话',
        cityCode: '城市编码',
        companyId: '物业编号',
        admin: '管理员账号',
        corporation: '公司法人',
        foundingTime: '成立日期',
        loginState: '登录状态',
        allowLogin: '允许登录',
        limitLogin: '限制登录',
        companyAddress: '公司地址',
        companyName: '公司名称'
      },
      fetchError: '获取小区列表失败'
    },
    addCommunity: {
      title: '添加小区',
      name: '小区名称',
      namePlaceholder: '必填，请填写小区名称',
      area: '小区地区',
      address: '小区地址',
      addressPlaceholder: '必填，请填写小区地址',
      nearbyLandmarks: '附近地标',
      nearbyLandmarksPlaceholder: '必填，请填写附近地标',
      tel: '客服电话',
      telPlaceholder: '必填，请填写客服电话',
      payFeeMonth: '缴费周期',
      payFeeMonthPlaceholder: '必填，请填写缴费周期（月）',
      feePrice: '每月单价',
      feePricePlaceholder: '必填，请填写每月单价（元）',
      saveSuccess: '小区添加成功',
      validate: {
        name: '小区名称不能为空',
        cityCode: '城市编码不能为空',
        address: '小区地址不能为空',
        nearbyLandmarks: '附近地标不能为空',
        tel: '客服电话不能为空'
      }
    },
    editCommunity: {
      title: '修改小区',
      name: '小区名称',
      namePlaceholder: '必填，请填写小区名称',
      address: '小区地址',
      addressPlaceholder: '必填，请填写小区地址',
      nearbyLandmarks: '附近地标',
      nearbyLandmarksPlaceholder: '必填，请填写附近地标',
      payFeeMonth: '缴费周期',
      payFeeMonthPlaceholder: '必填，请填写缴费周期（月）',
      feePrice: '每月单价',
      feePricePlaceholder: '必填，请填写每月单价（元）',
      tel: '客服电话',
      telPlaceholder: '必填，请填写客服电话',
      saveSuccess: '小区修改成功',
      validate: {
        name: '小区名称不能为空',
        address: '小区地址不能为空',
        nearbyLandmarks: '附近地标不能为空',
        communityId: '小区ID不能为空'
      }
    },
    deleteCommunity: {
      title: '请确认您的操作',
      confirmText: '确定删除小区',
      cancel: '点错了',
      confirm: '确认删除',
      deleteSuccess: '小区删除成功'
    },
    communityDataToIot: {
      title: '请确认您的操作',
      confirmText: '同步小区中的业主信息(业主关联的房屋信息)，车辆信息和员工信息',
      cancel: '点错了',
      confirm: '确认同步',
      syncSuccess: '同步提交成功'
    },
    recallAuditFinishCommunity: {
      title: '请确认您的操作',
      confirmText: '确定撤回审核',
      cancel: '点错了',
      confirm: '确认撤回'
    },
    areaSelect: {
      province: '请选择省',
      city: '请选择城市',
      area: '请选择区县',
fetchError: '查询地区失败'
    }
  },
  'zh-TW': {
    communityManage: {
      search: {
        title: '查詢條件',
        communityId: '請輸入小區ID',
        name: '請輸入小區名稱'
      },
      list: {
        title: '小區信息',
        addCommunity: '添加小區'
      },
      table: {
        communityId: '小區ID',
        name: '小區名稱',
        storeName: '物业公司',
        nearbyLandmarks: '附近地標',
        cityCode: '城市編碼',
        createTime: '創建時間',
        state: '狀態',
        syncIot: '同步IOT'
      },
      card: {
        normal: '正常',
        incomplete: '待完善',
        store: '物业',
        address: '地址',
        tel: '客服',
        attr: '屬性',
        attrUnit: '項',
        code: '編號',
        unlinked: '未關聯物業公司',
        unfilled: '未維護',
        unnamed: '未命名小區',
        locationEmpty: '地區未維護',
        empty: '沒有匹配的小區，試試調整篩選條件',
        clearFilter: '清除篩選'
      },
      filterBar: {
        namePlaceholder: '請輸入小區名稱',
        storePlaceholder: '全部物業公司',
        page: '本頁',
        total: '合計',
        hint: '共 {page} / {total} 個小區',
        totalCount: '共 {total} 條'
      },
      drawer: {
        title: '小區詳情',
        tabCompany: '物業公司',
        tabAttr: '小區屬性',
        company: '所屬物業公司',
        companyEmpty: '該小區暫未關聯物業公司',
        companyNotFound: '未查詢到物業公司信息',
        companyFetchError: '物業公司信息查詢失敗',
        service: '正常服務',
        contact: '聯繫方式',
        attrEmpty: '該小區暫未維護屬性信息',
        attrTotal: '共 {total} 項小區屬性',
        specName: '屬性名稱',
        specCd: '規格編碼',
        value: '屬性值',
        listShow: '列表展示',
        show: '展示',
        notShow: '不展示',
        updatedAt: '數據更新於',
        code: '小區編號',
        communityId: '小區編號',
        store: '物業公司',
        state: '審核狀態',
        city: '所在城市',
        address: '詳細地址',
        landmarks: '周邊地標',
        tel: '客服電話',
        cityCode: '城市編碼',
        companyId: '物業編號',
        admin: '管理員賬號',
        corporation: '公司法人',
        foundingTime: '成立日期',
        loginState: '登錄狀態',
        allowLogin: '允許登錄',
        limitLogin: '限制登錄',
        companyAddress: '公司地址',
        companyName: '公司名稱'
      },
      fetchError: '獲取小區列表失敗'
    },
    addCommunity: {
      title: '添加小區',
      name: '小區名稱',
      namePlaceholder: '必填，請填寫小區名稱',
      area: '小區地區',
      address: '小區地址',
      addressPlaceholder: '必填，請填寫小區地址',
      nearbyLandmarks: '附近地標',
      nearbyLandmarksPlaceholder: '必填，請填寫附近地標',
      tel: '客服電話',
      telPlaceholder: '必填，請填寫客服電話',
      payFeeMonth: '繳費週期',
      payFeeMonthPlaceholder: '必填，請填寫繳費週期（月）',
      feePrice: '每月單價',
      feePricePlaceholder: '必填，請填寫每月單價（元）',
      saveSuccess: '小區添加成功',
      validate: {
        name: '小區名稱不能為空',
        cityCode: '城市編碼不能為空',
        address: '小區地址不能為空',
        nearbyLandmarks: '附近地標不能為空',
        tel: '客服電話不能為空'
      }
    },
    editCommunity: {
      title: '修改小區',
      name: '小區名稱',
      namePlaceholder: '必填，請填寫小區名稱',
      address: '小區地址',
      addressPlaceholder: '必填，請填寫小區地址',
      nearbyLandmarks: '附近地標',
      nearbyLandmarksPlaceholder: '必填，請填寫附近地標',
      payFeeMonth: '繳費週期',
      payFeeMonthPlaceholder: '必填，請填寫繳費週期（月）',
      feePrice: '每月單價',
      feePricePlaceholder: '必填，請填寫每月單價（元）',
      tel: '客服電話',
      telPlaceholder: '必填，請填寫客服電話',
      saveSuccess: '小區修改成功',
      validate: {
        name: '小區名稱不能為空',
        address: '小區地址不能為空',
        nearbyLandmarks: '附近地標不能為空',
        communityId: '小區ID不能為空'
      }
    },
    deleteCommunity: {
      title: '請確認您的操作',
      confirmText: '確定刪除小區',
      cancel: '點錯了',
      confirm: '確認刪除',
      deleteSuccess: '小區刪除成功'
    },
    communityDataToIot: {
      title: '請確認您的操作',
      confirmText: '同步小區中的業主信息(業主關聯的房屋信息)，車輛信息和員工信息',
      cancel: '點錯了',
      confirm: '確認同步',
      syncSuccess: '同步提交成功'
    },
    recallAuditFinishCommunity: {
      title: '請確認您的操作',
      confirmText: '確定撤回審核',
      cancel: '點錯了',
      confirm: '確認撤回'
    },
    areaSelect: {
      province: '請選擇省',
      city: '請選擇城市',
      area: '請選擇區縣',
      fetchError: '查詢地區失敗'
    }
  }
}