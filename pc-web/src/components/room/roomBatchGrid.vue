<template>
  <div class="room-batch-grid">
    <div class="grid-toolbar">
      <div class="active-building">
        <span class="active-name">{{ buildingName }}</span>
        <span class="active-meta">{{ buildingMeta }}</span>
      </div>
      <el-input
        v-model="keywordModel"
        class="search-input"
        size="small"
        clearable
        prefix-icon="el-icon-search"
        :placeholder="$t('roomCreatePayFee.searchRoomPlaceholder')">
      </el-input>
      <span class="grid-count">
        <span>{{ $t('roomCreatePayFee.buildingTotalRooms', { total: visibleCount }) }}</span>
        <span class="dot">·</span>
        <span class="selected">{{ $t('roomCreatePayFee.selectedRooms', { selected: visibleSelectedCount }) }}</span>
      </span>
    </div>

    <div v-if="otherMatches && otherMatches.length > 0" class="other-hint">
      <span class="hint-label">{{ $t('roomCreatePayFee.otherMatch') }}</span>
      <el-button
        v-for="item in otherMatches"
        :key="item.floorId"
        class="jump-btn"
        size="mini"
        @click="$emit('jump-building', item.floorId)">
        {{ item.floorNum }}{{ $t('roomCreatePayFee.buildingSuffix') }} {{ item.count }} {{ $t('roomCreatePayFee.householdsUnit') }}
      </el-button>
    </div>

    <div v-loading="loading" class="grid-body">
      <div v-for="group in groups" :key="group.key" class="room-group">
        <div class="group-head">
          <el-checkbox
            class="group-check"
            :value="group.checked"
            :indeterminate="group.indeterminate"
            @change="$emit('toggle-group', group.key)">
            {{ group.unitNum }}{{ $t('roomCreatePayFee.unitSuffix') }}
          </el-checkbox>
          <span class="group-count">{{ group.selCount }}/{{ group.total }} {{ $t('roomCreatePayFee.householdsUnit') }}</span>
          <span class="group-line"></span>
        </div>
        <div class="room-grid">
          <div
            v-for="room in group.rooms"
            :key="room.roomId"
            class="room-card"
            :class="{ 'is-on': isSelected(room.roomId) }"
            :title="cardTitle(room)"
            @click="$emit('toggle-room', room.roomId)">
            <el-checkbox
              class="room-check"
              :value="isSelected(room.roomId)"
              @click.native.stop
              @change="$emit('toggle-room', room.roomId)">
            </el-checkbox>
            <span class="room-text">
              <span class="room-name">{{ roomLabel(room) }}</span>
              <span class="room-sub">{{ room.stateName || '-' }}</span>
            </span>
          </div>
        </div>
      </div>

      <div v-if="!loading && groups.length === 0" class="grid-empty">
        <i class="el-icon-office-building"></i>
        <p class="empty-title">{{ $t('roomCreatePayFee.noMatchRoom') }}</p>
        <p class="empty-tip">{{ $t('roomCreatePayFee.noMatchRoomTip') }}</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getRoomLabel } from '@/utils/roomBatchUtil'

export default {
  name: 'RoomBatchGrid',
  props: {
    groups: {
      type: Array,
      default: () => []
    },
    selectedMap: {
      type: Object,
      default: () => ({})
    },
    keyword: {
      type: String,
      default: ''
    },
    loading: {
      type: Boolean,
      default: false
    },
    buildingName: {
      type: String,
      default: ''
    },
    buildingMeta: {
      type: String,
      default: ''
    },
    otherMatches: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    keywordModel: {
      get() {
        return this.keyword
      },
      set(val) {
        this.$emit('update:keyword', val)
      }
    },
    visibleCount() {
      return this.groups.reduce((total, group) => total + group.total, 0)
    },
    visibleSelectedCount() {
      return this.groups.reduce((total, group) => total + group.selCount, 0)
    }
  },
  methods: {
    isSelected(roomId) {
      return !!this.selectedMap[roomId]
    },
    roomLabel(room) {
      return getRoomLabel(room)
    },
    cardTitle(room) {
      const parts = [getRoomLabel(room), room.stateName, room.ownerName]
      return parts.filter(item => !!item).join(' · ')
    }
  }
}
</script>

<style lang="scss" scoped>
.room-batch-grid {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  text-align: left;

  .grid-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border-bottom: 1px solid #f1f5f9;

    .active-building {
      display: flex;
      align-items: baseline;
      margin-right: 4px;

      .active-name {
        font-size: 13.5px;
        font-weight: 600;
        color: #1e293b;
      }

      .active-meta {
        margin-left: 8px;
        font-size: 11.5px;
        color: #94a3b8;
      }
    }

    .search-input {
      flex: 1;
      min-width: 160px;
    }

    .grid-count {
      font-size: 12px;
      color: #94a3b8;

      .dot {
        margin: 0 4px;
      }

      .selected {
        color: #2563eb;
      }
    }
  }

  .other-hint {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 8px 16px 0;
    font-size: 11.5px;
    color: #64748b;

    .jump-btn {
      height: 22px;
      padding: 0 8px;
      font-size: 11.5px;
      color: #2563eb;
      background: #eff6ff;
      border-color: #eff6ff;

      &:hover {
        background: #dbeafe;
        border-color: #dbeafe;
      }
    }
  }

  .grid-body {
    flex: 1;
    overflow-y: auto;
    max-height: 560px;
    padding: 12px 16px;
  }

  .room-group {
    margin-bottom: 12px;
  }

  .group-head {
    display: flex;
    align-items: center;
    margin-bottom: 6px;

    .group-check {
      ::v-deep .el-checkbox__label {
        font-size: 12.5px;
        font-weight: 600;
        color: #334155;
      }
    }

    .group-count {
      margin-left: 8px;
      font-size: 11px;
      color: #94a3b8;
    }

    .group-line {
      flex: 1;
      height: 1px;
      margin-left: 8px;
      background: #f1f5f9;
    }
  }

  .room-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
  }

  @media (min-width: 640px) {
    .room-grid {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  @media (min-width: 900px) {
    .room-grid {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
  }

  @media (min-width: 1280px) {
    .room-grid {
      grid-template-columns: repeat(8, minmax(0, 1fr));
    }
  }

  .room-card {
    display: flex;
    align-items: center;
    min-width: 0;
    padding: 5px 8px;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.14s, background 0.14s, box-shadow 0.14s;

    &:hover {
      border-color: #93c5fd;
      background: #f8fbff;
    }

    &.is-on {
      border-color: #3b82f6;
      background: #eff6ff;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
    }

    .room-check {
      margin-right: 4px;

      ::v-deep .el-checkbox__inner {
        width: 13px;
        height: 13px;
      }
    }

    .room-text {
      min-width: 0;
      line-height: 1.3;

      .room-name {
        display: block;
        overflow: hidden;
        font-size: 11.5px;
        font-weight: 500;
        color: #334155;
        white-space: nowrap;
        text-overflow: ellipsis;
      }

      .room-sub {
        display: block;
        overflow: hidden;
        font-size: 10px;
        color: #94a3b8;
        white-space: nowrap;
        text-overflow: ellipsis;
      }
    }
  }

  .grid-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 0;
    color: #94a3b8;
    text-align: center;

    i {
      margin-bottom: 8px;
      font-size: 34px;
    }

    .empty-title {
      font-size: 13px;
    }

    .empty-tip {
      margin-top: 4px;
      font-size: 11.5px;
    }
  }
}
</style>
