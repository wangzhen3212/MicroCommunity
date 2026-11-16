<template>
  <el-dialog
    :title="$t('contractWorkbench.target.house')"
    :visible.sync="visible"
    width="760px"
    @close="handleClose"
  >
    <div class="cc-toolbar">
      <el-input
        style="width: 240px"
        v-model="keyword"
        :placeholder="$t('contractLedger.filter.keywordPlaceholder')"
        clearable
        @keyup.enter.native="queryRoomsMethod"
      />
      <el-button type="primary" @click="queryRoomsMethod">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
    </div>

    <el-table
      :data="rooms"
      size="mini"
      border
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column :label="$t('contractWorkbench.target.house')">
        <template slot-scope="scope">
          {{ scope.row.roomName || ((scope.row.floorNum || '') + '-' + (scope.row.unitNum || '') + '-' + (scope.row.roomNum || '')) }}
        </template>
      </el-table-column>
      <el-table-column prop="ownerName" :label="$t('contractWorkbench.target.owner')" align="center" />
      <el-table-column :label="$t('contractWorkbench.target.area')" align="center" width="110">
        <template slot-scope="scope">{{ scope.row.builtUpArea || '-' }}</template>
      </el-table-column>
      <el-table-column prop="stateName" :label="$t('contractCategory.all')" align="center" width="110" />
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
import { queryRooms } from '@/api/contract/addContractApi'

export default {
  name: 'ContractHouseDialog',
  data() {
    return {
      visible: false,
      loading: false,
      keyword: '',
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
    open(exists) {
      this.visible = true
      this.selected = exists ? exists.slice() : []
      this.keyword = ''
      this.page.current = 1
      this.queryRoomsMethod()
    },
    handleClose() {
      this.visible = false
    },
    queryRoomsMethod() {
      this.loading = true
      queryRooms({
        communityId: getCommunityId(),
        page: this.page.current,
        row: this.page.size,
        roomNameLike: this.keyword,
        all: true
      }).then(res => {
        const payload = res || {}
        this.rooms = payload.rooms || payload.data || []
        this.page.total = payload.total || this.rooms.length
      }).catch(error => {
        this.rooms = []
        this.page.total = 0
        this.$message.error(error.message)
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
      this.$emit('choose', this.selected.slice())
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

  .el-button {
    margin-left: 8px;
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
