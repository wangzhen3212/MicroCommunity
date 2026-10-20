<template>
  <div class="margin-top-sm">
    <el-row :gutter="20">
      <el-col :span="3" class="padding-r-0">
        <el-card class="border-radius">
          <div class="treeview attendance-staff" style="height: 650px;">
            <ul class="list-group text-center border-radius">
              <li v-for="(item, index) in feeTypeCds" :key="index" class="list-group-item node-orgTree"
                :class="{ 'vc-node-selected': feeTypeCd === item.statusCd }" @click="_switchFeeTypeCd(item.statusCd)">
                {{ item.name }}
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col :span="21">
        <component :is="_contentComponent" :key="feeTypeCd" ref="content" />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDict } from '@/api/community/communityApi'
import DataMonthOweStatisticsRoom from '@/components/report/DataMonthOweStatisticsRoom'
import DataMonthOweStatisticsParking from '@/components/report/DataMonthOweStatisticsParking'

const PARKING_FEE_TYPE_CD = '888800010008'

export default {
  name: 'DataMonthOweStatistics',

  components: {
    DataMonthOweStatisticsRoom,
    DataMonthOweStatisticsParking
  },
  data() {
    return {
      feeTypeCds: [],
      feeTypeCd: '',
      communityId: '',
      startDate: '',
      endDate: ''
    }
  },
  computed: {
    _contentComponent() {
      return this.feeTypeCd === PARKING_FEE_TYPE_CD ? 'DataMonthOweStatisticsParking' : 'DataMonthOweStatisticsRoom'
    }
  },
  methods: {
    open(param) {
      if (param) {
        this.communityId = param.communityId || this.communityId
        this.startDate = param.startDate || this.startDate
        this.endDate = param.endDate || this.endDate
      }
      if (this.feeTypeCds.length > 0) {
        this._openContent()
      } else {
        this._initMethod()
      }
    },
    async _initMethod() {
      try {
        const data = await getDict('pay_fee_config', 'fee_type_cd_show')
        this.feeTypeCds = data || []
        if (!this.feeTypeCd && this.feeTypeCds.length > 0) {
          this.feeTypeCd = this.feeTypeCds[0].statusCd
        }
      } catch (error) {
        console.error('Failed to load dictionary:', error)
      }
      this._openContent()
    },
    _openContent() {
      this.$nextTick(() => {
        const content = this.$refs.content
        if (content && content.open) {
          content.open({
            communityId: this.communityId,
            startDate: this.startDate,
            endDate: this.endDate,
            feeTypeCd: this.feeTypeCd
          })
        }
      })
    },
    _switchFeeTypeCd(feeTypeCd) {
      if (this.feeTypeCd === feeTypeCd) {
        return
      }
      this.feeTypeCd = feeTypeCd
      this._openContent()
    }
  }
}
</script>

<style scoped>
.text-right {
  text-align: right;
}

.padding-r-0 {
  padding-right: 0;
}

.border-radius {
  border-radius: 4px;
}

.list-group {
  list-style: none;
  padding: 0;
  margin: 0;
}

.list-group-item {
  padding: 10px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
}

.list-group-item:hover {
  background-color: #f5f5f5;
}

.vc-node-selected {
  background-color: #409EFF;
  color: white;
}

.treeview {
  overflow-y: auto;
}
</style>
