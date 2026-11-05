<template>
  <div class="car-structure-container animated fadeInRight ecommerce">
    <el-row :gutter="20">
      <el-col :span="4">
        <el-card class="parking-area-card">
          <parking-area-tree ref="parkingAreaTree" @switchParkingAreaSection="switchParkingAreaSection" />
        </el-card>
      </el-col>
      <el-col :span="20">
        <el-card class="box-card">
          <div slot="header" class="clearfix flex justify-between">
            <div class="flex align-center">
              <span>{{ $t('carStructure.structureTitle') }}</span>
              <span class="text-muted margin-left-sm">
                {{ $t('carStructure.total') }}：{{ total }}
              </span>
            </div>
            <div class="flex align-center">
              <span class="legend-item">
                <span class="legend-color legend-free"></span>{{ $t('carStructure.legendFree') }}
              </span>
              <span class="legend-item">
                <span class="legend-color legend-normal"></span>{{ $t('carStructure.legendNormal') }}
              </span>
              <span class="legend-item">
                <span class="legend-color legend-owe"></span>{{ $t('carStructure.legendOwe') }}
              </span>
              <el-button type="primary" size="small" class="margin-left-sm" @click="handleRefresh">
                <i class="el-icon-refresh"></i>{{ $t('carStructure.refresh') }}
              </el-button>
            </div>
          </div>
          <div v-loading="loading" class="car-card-container">
            <el-row :gutter="10">
              <el-col v-for="(item, index) in parkingSpaces" :key="index" :span="4">
                <div class="car-card margin-bottom" :style="{ 'background-color': getCardColor(item) }"
                  @dblclick="doubleClickCard">
                  <div class="car-card-header">
                    <span>{{ getParkingSpaceNum(item) }}</span>
                    <span v-if="isOwe(item)" class="car-card-badge">{{ $t('carStructure.owe') }}</span>
                  </div>
                  <div>{{ item.ownerName || $t('carStructure.free') }}</div>
                  <div>{{ item.carNum || $t('carStructure.noCarNum') }}</div>
                  <div>{{ getStateText(item) }}</div>
                </div>
              </el-col>
            </el-row>
            <div v-if="!loading && parkingSpaces.length < 1" class="no-data">
              {{ $t('carStructure.noData') }}
            </div>
          </div>
          <el-pagination v-if="total > 0" :current-page="page" :page-sizes="[20, 50, 100, 200]" :page-size="row"
            :total="total" layout="total, prev, pager, next, sizes" @size-change="handleSizeChange"
            @current-change="handlePageChange" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import ParkingAreaTree from '@/components/car/parkingAreaTree'
import { listParkingSpaceStructures } from '@/api/car/carStructureApi'

export default {
  name: 'CarStructureList',
  components: {
    ParkingAreaTree
  },
  data() {
    return {
      loading: false,
      total: 0,
      page: 1,
      row: 20,
      conditions: {
        paId: '',
        sectionId: ''
      },
      parkingSpaces: []
    }
  },
  created() {
    this.loadParkingSpaceStructures()
  },
  methods: {
    switchParkingAreaSection(data) {
      this.conditions.paId = data.paId
      this.conditions.sectionId = data.sectionId
      this.page = 1
      this.loadParkingSpaceStructures()
    },
    handleRefresh() {
      this.page = 1
      this.loadParkingSpaceStructures()
      if (this.$refs.parkingAreaTree) {
        this.$refs.parkingAreaTree.refreshTree()
      }
    },
    handleSizeChange(val) {
      this.row = val
      this.page = 1
      this.loadParkingSpaceStructures()
    },
    handlePageChange(val) {
      this.page = val
      this.loadParkingSpaceStructures()
    },
    async loadParkingSpaceStructures() {
      this.loading = true
      try {
        const params = {
          page: this.page,
          row: this.row,
          paId: this.conditions.paId,
          sectionId: this.conditions.sectionId
        }
        const res = await listParkingSpaceStructures(params)
        this.parkingSpaces = res.data || []
        this.total = res.total || 0
      } catch (error) {
        this.$message.error(this.$t('carStructure.fetchError'))
      } finally {
        this.loading = false
      }
    },
    getParkingSpaceNum(item) {
      const nums = []
      if (item.areaNum) {
        nums.push(item.areaNum)
      }
      if (item.sectionName || item.sectionNum) {
        nums.push(item.sectionName || item.sectionNum)
      }
      if (item.num) {
        nums.push(item.num)
      }
      return nums.join('-')
    },
    getOweAmount(item) {
      const amount = Number(item.oweAmount)
      if (isNaN(amount)) {
        return 0
      }
      return amount
    },
    isOwe(item) {
      return this.getOweAmount(item) > 0
    },
    getCardColor(item) {
      if (item.state === 'F' || !item.ownerName) {
        return '#1AB394'
      }
      if (this.isOwe(item)) {
        return '#DC3545'
      }
      return '#1296db'
    },
    getStateText(item) {
      if (item.state === 'F' || !item.ownerName) {
        return this.$t('carStructure.free')
      }
      if (this.isOwe(item)) {
        return this.$t('carStructure.oweAmount') + ' ¥' + this.getOweAmount(item).toFixed(2)
      }
      const stateName = item.state === 'S' ? this.$t('carStructure.selled') : this.$t('carStructure.hired')
      return stateName + ' · ' + this.$t('carStructure.noOwe')
    },
    doubleClickCard() {
      this.$router.push('/pages/property/parkingSpaceCreateFee')
    }
  }
}
</script>

<style lang="scss" scoped>
.car-structure-container {
  padding: 20px;

  .parking-area-card {
    min-height: calc(100vh - 120px);
  }

  .align-center {
    align-items: center;
  }

  .text-muted {
    color: #909399;
    font-size: 13px;
  }

  .legend-item {
    display: inline-flex;
    align-items: center;
    margin-right: 15px;
    font-size: 13px;
    color: #606266;
  }

  .legend-color {
    display: inline-block;
    width: 12px;
    height: 12px;
    border-radius: 2px;
    margin-right: 5px;
  }

  .legend-free {
    background-color: #1AB394;
  }

  .legend-normal {
    background-color: #1296db;
  }

  .legend-owe {
    background-color: #DC3545;
  }

  .car-card-container {
    min-height: calc(100vh - 260px);
  }

  .car-card {
    color: #fff;
    border-radius: 5px;
    cursor: pointer;
    min-height: 110px;
    padding: 10px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    transition: all 0.3s;

    &:hover {
      transform: scale(1.05);
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    }

    .car-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 5px;
      font-weight: 600;
    }

    .car-card-badge {
      background-color: rgba(255, 255, 255, 0.25);
      border-radius: 10px;
      padding: 0 6px;
      font-size: 12px;
    }
  }

  .no-data {
    text-align: center;
    padding: 40px 0;
    color: #909399;
  }
}
</style>