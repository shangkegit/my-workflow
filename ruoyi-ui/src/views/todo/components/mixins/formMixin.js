/**
 * 表单组件混入
 * 提供统一的 props、事件和方法
 */
import { processTask } from '../../api/myTodoList'

export default {
  props: {
    // 任务ID
    taskId: {
      type: String,
      required: true
    },
    // 业务主键（从 query.id 获取）
    businessKey: {
      type: String,
      default: ''
    },
    // 业务数据（从后端获取）
    formInfo: {
      type: Object,
      default: () => ({})
    }
  },

  data() {
    return {
      loading: false
    }
  },

  methods: {
    /**
     * 完成任务（统一接口）
     * @param {Object} data - 提交的数据
     */
    async completeTask(data = {}) {
      this.loading = true
      try {
        await processTask({ taskId: this.taskId, ...data })
        this.$emit('complete')
      } catch (error) {
        this.$emit('error', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    /**
     * 撤销操作
     */
    handleCancel() {
      this.$emit('cancel', this.taskId)
    },

    /**
     * 驳回操作
     */
    handleReject() {
      this.$emit('reject', this.taskId)
    },

    /**
     * 显示成功消息
     * @param {string} message
     */
    showSuccess(message = '操作成功') {
      this.$message.success(message)
    },

    /**
     * 显示错误消息
     * @param {string} message
     */
    showError(message = '操作失败') {
      this.$message.error(message)
    }
  }
}
