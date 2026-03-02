<template>
    <div class="c-purchase-form">
        <el-form ref="form" :model="form" label-width="100px" v-loading="loading">
            <el-form-item label="申请人">
                <el-input v-model="form.applyer" :disabled="true"></el-input>
            </el-form-item>
            <el-form-item label="申请时间">
                <el-input v-if="step !== 'updateapply'" v-model="form.applytime" :disabled="canNotEdit"></el-input>
                <el-date-picker v-else type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择日期" v-model="form.applytime" style="width: 100%;"></el-date-picker>
            </el-form-item>
            <el-form-item label="采购清单">
                <el-input v-model="form.itemlist" :disabled="canNotEdit"></el-input>
            </el-form-item>
            <el-form-item label="总价">
                <el-input v-model="form.total" :disabled="canNotEdit"></el-input>
            </el-form-item>
            <template v-if="step === 'finance' || step === 'purchasemanager' || step === 'manager'">
                <el-form-item label="审批结果">
                    <el-radio-group v-model="form.result">
                        <el-radio label="true">同意</el-radio>
                        <el-radio label="false">拒绝</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="审批意见">
                    <el-input type="textarea" v-model="form.comment"></el-input>
                </el-form-item>
            </template>
            <el-form-item label="是否重新申请" v-if="step === 'updateapply'">
                <el-radio-group v-model="form.updateapply">
                    <el-radio label="true">是</el-radio>
                    <el-radio label="false">否</el-radio>
                </el-radio-group>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" @click="onSubmit">提交</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script>
import formMixin from '../mixins/formMixin'
import { updatePurchase } from '../../api/myTodoList'

// 步骤到参数名的映射
const keyMap = {
    purchasemanager: "purchaseauditi",
    finance: "finance",
    pay: "pay",
    receiveitem: "receiveitem",
    manager: "manager",
    updateapply: "updateapply"
}

export default {
    name: 'PurchaseForm',
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
                updateapply: "true",
                result: "true",
                applyer: "",
                applytime: "",
                itemlist: "",
                total: "",
                comment: ""
            }
        }
    },

    computed: {
        canNotEdit() {
            return this.step !== "updateapply"
        }
    },

    watch: {
        formInfo: {
            handler(newFormValue) {
                if (newFormValue) {
                    this.form.applyer = newFormValue.applyer
                    this.form.applytime = newFormValue.applytime
                    this.form.itemlist = newFormValue.itemlist
                    this.form.total = newFormValue.total
                    this.form.comment = newFormValue.comment || ""
                }
            },
            immediate: true
        }
    },

    methods: {
        async onSubmit() {
            try {
                // 如果是更新申请步骤，先调用 updatePurchase
                if (this.step === 'updateapply') {
                    await updatePurchase({
                        ...this.form,
                        id: this.businessKey,
                        taskId: this.taskId
                    })
                }

                // 构建提交参数
                const params = { ...this.form }
                if (this.step !== "updateapply") {
                    params[keyMap[this.step]] = this.form.result
                }

                // 调用统一完成任务接口
                await this.completeTask(params)
            } catch (error) {
                this.showError(error.message || '提交失败')
            }
        }
    }
}
</script>

<style scoped>
.c-purchase-form {
    padding: 20px;
}
</style>
