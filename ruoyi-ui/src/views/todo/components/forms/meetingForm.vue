<template>
    <div class="c-meeting-form">
        <el-form ref="form" :model="form" label-width="80px" v-loading="loading">
            <el-form-item label="会议主题">
                <el-input v-model="form.topic" disabled></el-input>
            </el-form-item>
            <el-form-item label="主持人">
                <el-input v-model="form.host" disabled></el-input>
            </el-form-item>
            <el-form-item label="会议地址">
                <el-input v-model="form.place" disabled></el-input>
            </el-form-item>
            <el-form-item label="参会人员">
                <el-input v-model="form.peoplelist" disabled></el-input>
            </el-form-item>
            <el-form-item label="开始时间">
                <el-input v-model="form.startTime" disabled></el-input>
            </el-form-item>
            <el-form-item label="结束时间">
                <el-input v-model="form.endTime" disabled></el-input>
            </el-form-item>
            <el-form-item v-if="step === 'input'" label="会议纪要">
                <el-input type="textarea" v-model="form.content"></el-input>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="onSubmit">{{ step === "signate" ? "签到" : "提交" }}</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script>
import formMixin from '../mixins/formMixin'
import { fillMeetingRecord } from '../../api/myTodoList'

export default {
    name: 'MeetingForm',
    mixins: [formMixin],

    props: {
        step: {
            type: String,
            default: ""
        }
    },

    data() {
        return {
            form: {
                place: "",
                endTime: "",
                peoplelist: "",
                startTime: "",
                topic: "",
                host: "",
                content: ""
            }
        }
    },

    watch: {
        formInfo: {
            handler(newFormValue) {
                if (newFormValue) {
                    this.form.place = newFormValue.place
                    this.form.endTime = newFormValue.endTime
                    this.form.peoplelist = newFormValue.peoplelist
                    this.form.startTime = newFormValue.startTime
                    this.form.topic = newFormValue.topic
                    this.form.host = newFormValue.host
                    this.form.content = newFormValue.content || ""
                }
            },
            immediate: true
        }
    },

    methods: {
        async onSubmit() {
            try {
                // 如果是填写会议纪要步骤，先调用 fillMeetingRecord
                if (this.step === 'input') {
                    await fillMeetingRecord({
                        id: this.businessKey,
                        content: this.form.content
                    })
                }

                // 调用统一完成任务接口
                await this.completeTask({
                    content: this.form.content
                })
            } catch (error) {
                this.showError(error.message || '提交失败')
            }
        }
    }
}
</script>

<style scoped>
.c-meeting-form {
    padding: 20px;
}
</style>
