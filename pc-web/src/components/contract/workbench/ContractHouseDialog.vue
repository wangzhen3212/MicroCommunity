<template>
  <el-dialog
    :title="$t('contractWorkbench.target.addRent')"
    :visible.sync="visible"
    width="860px"
    @close="handleClose"
  >
    <el-tabs v-model="roomType" @tab-click="handleTabChange">
      <el-tab-pane :label="$t('contractWorkbench.target.tabRoom')" name="1010301" />
      <el-tab-pane :label="$t('contractWorkbench.target.tabShop')" name="2020602" />
    </el-tabs>

    <div class="cc-toolbar">
      <el-input
        style="width: 130px"
        v-model="floorNum"
        :placeholder="$t('contractWorkbench.target.floorNum')"
        clearable
        @keyup.enter.native="handleSearch"
      />
      <el-input
        style="width: 150px"
        v-model="roomNumLike"
        :placeholder="$t('contractWorkbench.target.roomNum')"
        clearable
        @keyup.enter.native="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
      <span class="cc-toolbar-tip">{{ $t('contractWorkbench.target.noOwnerTip') }}</span>
    </div>

    <el-table
      :data="rooms"
      size="mini"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column :label="$t('contractWorkbench.target.roomName')">
        <template slot-scope="scope">
          {{ scope.row.roomName || ((scope.row.floorNum || '') + '-' + (scope.row.unitNum || '') + '-' + (scope.row.roomNum || '')) }}
        </template>
      </el-table-column>
      <el-table-column prop="stateName" :label="$t('contractWorkbench.target.state')" align="center" width="100" />
      <el-table-column :label="$t('contractWorkbench.target.area')" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.builtUpArea || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.roomArea')" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.roomArea || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.rent')" align="center" width="110">
        <template slot-scope="scope">{{ formatAmount(scope.row.roomRent) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      :current-page="page.current"
      :page-size="page.size"
      :total="page.total"
      layout="total, prev, pager, next"
      style="margin-top: 12px; text-align: right"
      @current-change="handleCurrentChange"
    />

    <div class="cc-selected">
      <div class="cc-selected-title">{{ $t('contractWorkbench.target.house') }}（{{ selected.length }}）</div>
      <div v-if="selected.length" class="cc-selected-list">
        <el-tag
          v-for="(room, index) in selected"
          :key="'room' + index"
          size="small"
          closable
          @close="removeRoom(index)"
        >
          {{ room.roomName || ((room.floorNum || '') + '-' + (room.unitNum || '') + '-' + (room.roomNum || '')) }}
        </el-tag>
      </div>
      <div v-else class="cc-empty">{{ $t('contractWorkbench.target.empty') }}</div>
    </div>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="handleConfirm">{{ $t('common.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { getCommunityId } from '@/api/community/communityApi'
import { queryLeasableRooms } from '@/api/contract/addContractApi'
import { formatAmount } from '@/utils/contractUtil'

export default {
  name: 'ContractHouseDialog',
  data() {
    return {
      visible: false,
      loading: false,
      roomType: '1010301',
      floorNum: '',
      roomNumLike: '',
      rooms: [],
      selected: [],
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  methods: {
    formatAmount,
    open(exists) {
      this.visible = true
      this.selected = exists ? exists.slice() : []
      this.floorNum = ''
      this.roomNumLike = ''
      this.page.current = 1
      this.queryRoomsMethod()
    },
    handleClose() {
      this.visible = false
    },
    handleTabChange() {
      this.page.current = 1
      this.queryRoomsMethod()
    },
    handleSearch() {
      this.page.current = 1
      this.queryRoomsMethod()
    },
    queryRoomsMethod() {
      this.loading = true
      queryLeasableRooms({
        communityId: getCommunityId(),
        roomType: this.roomType,
        floorNum: this.floorNum,
        roomNumLike: this.roomNumLike,
        page: this.page.current,
        row: this.page.size
      }).then(res => {
        const payload = res || {}
        this.rooms = payload.data || []
        this.page.total = payload.total || this.rooms.length
      }).catch(error => {
        this.rooms = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractWorkbench.msg.loadError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSelectionChange(rows) {
      this.rooms.forEach(room => {
        const index = this.indexOfRoom(room.roomId)
        if (index > -1 && rows.indexOf(room) < 0) {
          this.selected.splice(index, 1)
        }
      })
      rows.forEach(room => {
        if (this.indexOfRoom(room.roomId) < 0) {
          this.selected.push(room)
        }
      })
    },
    indexOfRoom(roomId) {
      for (let index = 0; index < this.selected.length; index++) {
        if (this.selected[index].roomId === roomId) {
          return index
        }
      }
      return -1
    },
    removeRoom(index) {
      this.selected.splice(index, 1)
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.queryRoomsMethod()
    },
    handleConfirm() {
      const rooms = this.selected.map(room => {
        return {
          ...room,
          rentUnitPrice: room.rentUnitPrice || room.roomRent
        }
      })
      this.$emit('choose', rooms)
      this.handleClose()
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;

  .el-input {
    margin-right: 8px;
  }

  .el-button {
    margin-right: 8px;
  }

  .cc-toolbar-tip {
    font-size: 12px;
    color: #909399;
  }
}

.cc-selected {
  margin-top: 14px;

  .cc-selected-title {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
  }

  .cc-selected-list {
    .el-tag {
      margin: 0 8px 8px 0;
    }
  }
}
</style>
