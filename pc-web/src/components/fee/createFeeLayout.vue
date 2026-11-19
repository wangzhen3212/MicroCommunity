<template>
  <div class="create-fee-layout">
    <div class="resizable-layout" :class="{ resizing: isResizing }">
      <el-card v-if="empty" class="empty-page">
        <i class="el-icon-document"></i>
        <span>{{ emptyText }}</span>
      </el-card>
      <template v-else>
        <div class="tree-panel" :style="{ width: treeWidth + 'px' }">
          <slot name="tree"></slot>
        </div>

        <div class="resize-handle" @mousedown="startResize" @mouseover="showResizeCursor"
          @mouseleave="hideResizeCursor">
          <div class="resize-line"></div>
        </div>

        <div class="content-panel" :style="{ width: 'calc(100% - ' + (treeWidth + 10) + 'px)' }">
          <el-card class="content-card">
            <div slot="header" class="flex justify-between card-header">
              <span class="card-title">{{ title }}</span>
              <div class="header-tools">
                <slot name="tools"></slot>
              </div>
            </div>
            <div class="content-body">
              <slot name="content"></slot>
            </div>
          </el-card>
        </div>
      </template>
    </div>
    <slot></slot>
  </div>
</template>

<script>
export default {
  name: 'CreateFeeLayout',
  props: {
    title: {
      type: String,
      default: ''
    },
    empty: {
      type: Boolean,
      default: false
    },
    emptyText: {
      type: String,
      default: ''
    },
    storageKey: {
      type: String,
      required: true
    },
    defaultTreeWidth: {
      type: Number,
      default: 250
    },
    minTreeWidth: {
      type: Number,
      default: 200
    },
    maxTreeWidth: {
      type: Number,
      default: 600
    }
  },
  data() {
    return {
      treeWidth: this.defaultTreeWidth,
      isResizing: false,
      startX: 0,
      startWidth: 0
    }
  },
  created() {
    this.restoreTreeWidth()
  },
  mounted() {
    document.addEventListener('mousemove', this.onMouseMove)
    document.addEventListener('mouseup', this.onMouseUp)
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onMouseMove)
    document.removeEventListener('mouseup', this.onMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  },
  methods: {
    restoreTreeWidth() {
      const savedWidth = localStorage.getItem(this.storageKey)
      if (savedWidth) {
        const width = parseInt(savedWidth)
        if (width >= this.minTreeWidth && width <= this.maxTreeWidth) {
          this.treeWidth = width
        }
      }
    },
    saveTreeWidth() {
      localStorage.setItem(this.storageKey, this.treeWidth.toString())
    },
    startResize(event) {
      event.preventDefault()
      this.isResizing = true
      this.startX = event.clientX
      this.startWidth = this.treeWidth

      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
    },
    onMouseMove(event) {
      if (!this.isResizing) return

      const deltaX = event.clientX - this.startX
      const newWidth = this.startWidth + deltaX

      if (newWidth >= this.minTreeWidth && newWidth <= this.maxTreeWidth) {
        this.treeWidth = newWidth
      }
    },
    onMouseUp() {
      if (this.isResizing) {
        this.isResizing = false
        this.saveTreeWidth()

        document.body.style.cursor = ''
        document.body.style.userSelect = ''
      }
    },
    showResizeCursor() {
      if (!this.isResizing) {
        document.body.style.cursor = 'col-resize'
      }
    },
    hideResizeCursor() {
      if (!this.isResizing) {
        document.body.style.cursor = ''
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.create-fee-layout {
  padding: 20px;
  height: calc(100vh - 120px);
  overflow: hidden;
}

.resizable-layout {
  display: flex;
  flex-direction: row;
  height: 100%;
}

.empty-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 16px;

  i {
    font-size: 64px;
    margin-bottom: 20px;
  }
}

.tree-panel {
  background: #fff;
  border-radius: 4px;
  padding: 10px;
  border: 1px solid #e4e7ed;
}

.tree-panel * {
  user-select: none;
}

.content-panel {
  overflow-y: auto;
}

.resize-handle {
  width: 10px;
  cursor: col-resize;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resize-handle:hover .resize-line {
  background-color: #409eff;
}

.resize-line {
  width: 2px;
  height: 30px;
  background-color: #dcdfe6;
  border-radius: 1px;
  transition: background-color 0.2s;
}

.resizable-layout.resizing {
  cursor: col-resize;
}

.resizable-layout.resizing .tree-panel,
.resizable-layout.resizing .content-panel {
  pointer-events: none;
}

.resizable-layout.resizing .resize-handle {
  pointer-events: auto;
}

.resizable-layout.resizing * {
  user-select: none !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-body {
  ::v-deep .filter-container {
    margin-bottom: 20px;
  }

  ::v-deep .tab-section {
    margin-top: 20px;
  }
}
</style>
