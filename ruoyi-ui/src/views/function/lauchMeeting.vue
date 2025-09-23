<template>
    <div>
        <div class="search-bar">
            <div>
                <label>会议主题</label>
                <el-input type="text" v-model="searchParams.topic" size="small" />
            </div>
            <div>
                <label>主持人</label>
                <el-input type="text" v-model="searchParams.host"  size="small"/>
            </div>
            <div>
                <label>会议地址</label>
                <el-input type="text" v-model="searchParams.place"  size="small" />
            </div>
            <div>
                <label>参会人员</label>
                <el-input type="text" v-model="searchParams.peoplelist" size="small" />
            </div>
            <div>
                <el-button type="primary" @click="search" size="mini" icon="el-icon-search">搜索</el-button>
                <el-button type="default" @click="reset" size="mini" icon="el-icon-refresh">重置</el-button>
            </div>
        </div>
        <table-template
            :data="tableData"
            :total="total"
            selection
            @selection-change="handleSelectionChange"
            @page-change="handlePageChange"
        >
            <template #toolbar>
                <el-button type="primary" @click="dialogVisible = true" plain icon="el-icon-plus" size="mini">添加</el-button>
                <el-button type="danger" :disabled="currentSelection.length === 0" @click="handleDelelteMultiple" plain icon="el-icon-delete" size="mini">删除</el-button>
            </template>
            <template #columns>
                <el-table-column
                    prop="topic"
                    label="会议主题">
                </el-table-column>
                <el-table-column
                    prop="host"
                    label="主持人">
                </el-table-column>
                <el-table-column
                    prop="place"
                    label="会议地址">

                </el-table-column>
                <el-table-column
                    prop="peoplelist"
                    label="参会人员">
                </el-table-column>
                <el-table-column
                    prop="startTime"
                    label="开始时间">
                </el-table-column>
                <el-table-column
                    prop="endTime"
                    label="结束时间">
                </el-table-column>
                <el-table-column
                    prop="operation"
                    label="操作">
                    <template slot-scope="scope">
                        <el-button
                        size="mini"
                        type="text"
                        icon="el-icon-delete"
                        @click="handleDelete(scope.$index, scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </template>
        </table-template>
        <el-dialog :visible.sync="dialogVisible" width="600px">
            <el-form ref="form" :model="form" label-width="110px">
                <el-form-item label="会议主题">
                    <el-input v-model="form.topic"></el-input>
                </el-form-item>
                <el-form-item label="主持人">
                    <el-input v-model="form.host" disabled></el-input>
                </el-form-item>
                <el-form-item label="会议地址">
                    <el-input v-model="form.place"></el-input>
                </el-form-item>
                <el-form-item label="参会人员">
                    <el-select v-model="form.peoplelist" multiple>
                        <el-option
                            v-for="(user, i) in userList" 
                            :key="i"
                            :label="user.userName"
                            :value="user.userName"
                        ></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="开始时间">
                    <el-date-picker type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择日期" v-model="form.startTime" style="width: 100%;"></el-date-picker>
                </el-form-item>
                <el-form-item label="结束时间">
                    <el-date-picker type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择日期" v-model="form.endTime" style="width: 100%;"></el-date-picker>
                </el-form-item>
            </el-form>
            <span slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false">取 消</el-button>
                <el-button type="primary" @click="handleAdd">确 定</el-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import TableTemplate from "@/components/TableTemplate";
import {getMeetingList, addMeeting, deleteMeeting, exportMeeting} from "./api/meeting";
import {listUser} from "@/api/system/user.js"
export default {
    name: "leaveApply",
    components: {
        TableTemplate
    },
    data() {
        return {
            responseData: {},
            dialogVisible: false,
            form: {
                topic: "",
                host: this.$store.state.user.name,
                place: "",
                peoplelist: [],
                startTime: "",
                endTime: ""
            },
            searchParams: {
                pageNum: 1,
                pageSize: 10,
                topic: "",
                host: "",
                place: "",
                peoplelist: "",
            },
            currentSelection: [],
            userList: []
        };
    },
    computed: {
        tableData() {
            return this.responseData.rows || []
        },
        total() {
            return this.responseData.total || 0
        },
        selectionIds() {
            return this.currentSelection.map(item => item.id)
        }
    },
    mounted() {
        this.getMeetingListAndRender(this.searchParams);
        listUser().then(res => {
            console.log("获取用户", res);
            this.userList = res.rows;
        });
    },
    methods: {
        getMeetingListAndRender(params) {
            const {pageNum = 1, pageSize = 10, topic, host, place, peoplelist} = params;
            getMeetingList({
                pageNum,
                pageSize,
                topic,
                host,
                place,
                peoplelist,
                isAsc: "asc"
            }).then(res => {
                this.responseData = res;
            });
        },
        handleAdd() {
            console.log("填写的form值是", this.form);
            const params = Object.assign({}, this.form, {
                peoplelist: this.form.peoplelist.join()
            });
            addMeeting(params).then(res => {
                this.dialogVisible = false;
                this.$message.success("添加成功");
                this.getMeetingListAndRender(this.searchParams);
            });
        },
        handleDelete(index, row) {
            this.$confirm('确定删除吗？', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                const {id} = row;
                this.deleteByIdsAndRender(id)
            });
        },
        handleDelelteMultiple() {
            this.$confirm(`确定删除选中的${this.currentSelection.length}条数据吗？`, {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                const ids = this.selectionIds.join(",");
                this.deleteByIdsAndRender(ids)
            });
        },
        handleExport() {
            this.$confirm('确定导出所有数据吗？', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                exportMeeting(this.searchParams)
            });
        },
        search() {
            this.getMeetingListAndRender(this.searchParams);
        },
        reset() {
            this.searchParams.topic = "";
            this.searchParams.host = "";
            this.searchParams.place = "";
            this.searchParams.peoplelist = "";
            this.getMeetingListAndRender(this.searchParams);
        },
        deleteByIdsAndRender(ids) {
            deleteMeeting({
                ids
            }).then(() => {
                this.$message.error("删除成功!")
                this.getMeetingListAndRender(this.searchParams);
            })
        },
        handleSelectionChange(selection) {
            this.currentSelection = selection;
        },
        handlePageChange({pageNum, pageSize}) {
            this.searchParams.pageNum = pageNum;
            this.searchParams.pageSize = pageSize;
            this.getMeetingListAndRender(this.searchParams);
        }
    }
};
</script>

<style scoped>
.search-bar label {
    font-size: 14px;
    color: #606266;
    margin-right: 8px;
}
.search-bar {
    display: flex;
    margin-top: 8px;
    margin-left: 8px;
}
.search-bar .el-input {
    display: inline-block;
    width: 200px;
    margin-right: 10px;
}
</style>