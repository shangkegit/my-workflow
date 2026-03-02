<template>
    <div class="c-process-task">
        <div class="c-process-task__form">
            <!-- 动态组件 -->
            <component
                :is="currentForm"
                v-if="currentForm"
                ref="formRef"
                :step="step"
                :task-id="taskId"
                :business-key="businessKey"
                :form-info="formInfo"
                @complete="handleComplete"
                @cancel="handleCancel"
                @reject="handleReject"
                @error="handleError"
            />
            <!-- 未注册的流程类型提示 -->
            <el-empty v-else description="未知的流程类型">
                <el-button type="primary" @click="goBack">返回</el-button>
            </el-empty>
        </div>
        <div class="c-process-task__timeline">
            <time-line :activities="activities"></time-line>
        </div>
    </div>
</template>

<script>
import { getFormComponent } from './components/registry'
import { getProcessByTaskid, forceEnd, rejectLeave, getInfoByTaskId } from './api/myTodoList'
import TimeLine from './components/TimeLine.vue'

export default {
    name: 'ProcessTask',

    components: {
        TimeLine
    },

    data() {
        return {
            taskId: '',
            taskType: '',
            step: '',
            businessKey: '',
            formInfo: {},
            activities: []
        }
    },

    computed: {
        // 动态获取表单组件
        currentForm() {
            return getFormComponent(this.taskType)
        }
    },

    mounted() {
        this.init()
    },

    methods: {
        async init() {
            const { taskType, step, taskId } = this.$route.params
            const { id } = this.$route.query

            this.taskType = taskType
            this.step = step
            this.taskId = taskId
            this.businessKey = id || ''

            // 加载流程历史
            try {
                this.activities = await getProcessByTaskid(taskId)
            } catch (error) {
                console.error('加载流程历史失败:', error)
            }

            // 加载业务数据（约定: /{taskType}/{step}）
            try {
                const apiPath = this.getApiPath()
                const res = await getInfoByTaskId(apiPath, taskId)
                if (res.data) {
                    this.formInfo = res.data
                }
            } catch (error) {
                console.error('加载业务数据失败:', error)
            }
        },

        // 获取业务数据 API 路径（约定优于配置）
        getApiPath() {
            // 约定: /{taskType}/{step}
            // 如果有 step 参数，使用 step；否则使用 info
            const step = this.step || 'info'
            return `/${this.taskType}/${step}`
        },

        // 处理完成
        handleComplete() {
            this.$message.success('处理成功!')
            this.closeAndNavigate()
        },

        // 处理撤销
        async handleCancel(taskId) {
            try {
                await forceEnd(taskId || this.taskId)
                this.$message.success('已撤销')
                this.closeAndNavigate()
            } catch (error) {
                this.$message.error('撤销失败')
            }
        },

        // 处理驳回
        async handleReject(taskId) {
            try {
                await rejectLeave(taskId || this.taskId)
                this.$message.success('已驳回')
                this.closeAndNavigate()
            } catch (error) {
                this.$message.error('驳回失败')
            }
        },

        // 处理错误
        handleError(error) {
            console.error('表单错误:', error)
            this.$message.error(error.message || '操作失败')
        },

        // 关闭页面并导航
        closeAndNavigate() {
            this.$tab.closePage(this.$route).then(({ visitedViews }) => {
                this.toLastView(visitedViews, this.$route)
            })
        },

        // 导航到最后一个访问的页面
        toLastView(visitedViews, view) {
            const latestView = visitedViews.slice(-1)[0]
            if (latestView) {
                this.$router.push(latestView.fullPath)
            } else {
                if (view.name === 'Dashboard') {
                    this.$router.replace({ path: '/redirect' + view.fullPath })
                } else {
                    this.$router.push('/')
                }
            }
        },

        // 返回上一页
        goBack() {
            this.$router.go(-1)
        }
    }
}
</script>

<style>
.c-process-task {
    padding: 16px;
    display: flex;
    justify-content: center;
    gap: 20px;
}
.c-process-task__form {
    width: 500px;
    flex-shrink: 0;
}
.c-process-task__timeline {
    min-width: 300px;
}
</style>
