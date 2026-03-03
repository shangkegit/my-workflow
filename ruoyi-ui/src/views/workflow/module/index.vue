<!-- 流程模块管理页面 -->
<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧：模块列表 -->
      <el-col :span="16">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>流程模块列表</span>
            <el-button-group style="float: right;">
              <el-button size="mini" type="primary" icon="el-icon-upload2" @click="handleImport">导入模块</el-button>
              <el-button size="mini" type="success" icon="el-icon-plus" @click="handleCreate">新建模块</el-button>
            </el-button-group>
          </div>
          
          <el-table :data="moduleList" style="width: 100%">
            <el-table-column prop="name" label="模块名称" width="180"></el-table-column>
            <el-table-column prop="processKey" label="流程键" width="120"></el-table-column>
            <el-table-column prop="version" label="版本" width="80"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template slot-scope="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deployTime" label="部署时间" width="160"></el-table-column>
            <el-table-column label="操作" width="240">
              <template slot-scope="scope">
                <el-button size="mini" type="text" icon="el-icon-view" @click="handleView(scope.row)">查看</el-button>
                <el-button size="mini" type="text" icon="el-icon-download" @click="handleExport(scope.row)">导出</el-button>
                <el-button 
                  v-if="scope.row.status === 'active'" 
                  size="mini" 
                  type="text" 
                  icon="el-icon-video-pause" 
                  @click="handleSuspend(scope.row)">暂停</el-button>
                <el-button 
                  v-if="scope.row.status === 'suspended'" 
                  size="mini" 
                  type="text" 
                  icon="el-icon-video-play" 
                  @click="handleActivate(scope.row)">激活</el-button>
                <el-button size="mini" type="text" icon="el-icon-delete" style="color: #F56C6C" @click="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <!-- 右侧：快速操作 -->
      <el-col :span="8">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>快速操作</span>
          </div>
          
          <div class="quick-actions">
            <el-upload
              class="upload-demo"
              drag
              action="/workflow/module/manage/upload"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              accept=".json">
              <i class="el-icon-upload"></i>
              <div class="el-upload__text">拖拽模块文件到此处上传<br><em>或点击选择文件</em></div>
            </el-upload>
            
            <el-divider></el-divider>
            
            <div class="stats">
              <el-row :gutter="10">
                <el-col :span="8">
                  <div class="stat-item">
                    <div class="stat-value">{{ stats.total }}</div>
                    <div class="stat-label">总模块</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-item active">
                    <div class="stat-value">{{ stats.active }}</div>
                    <div class="stat-label">运行中</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-item suspended">
                    <div class="stat-value">{{ stats.suspended }}</div>
                    <div class="stat-label">已暂停</div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </el-card>
        
        <el-card class="box-card" style="margin-top: 20px;">
          <div slot="header">
            <span>最近操作</span>
          </div>
          <el-timeline>
            <el-timeline-item
              v-for="(activity, index) in recentActivities"
              :key="index"
              :timestamp="activity.time">
              {{ activity.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 导入对话框 -->
    <el-dialog title="导入模块" :visible.sync="importDialogVisible" width="60%">
      <el-alert type="info" :closable="false" style="margin-bottom: 20px;">
        <template slot="title">
          <strong>模块配置格式说明</strong>
        </template>
        <div style="margin-top: 10px; font-size: 12px;">
          <p>• meta: 模块元信息（名称、版本、作者等）</p>
          <p>• tasks: 任务处理器配置</p>
          <p>• scripts: Groovy 脚本代码</p>
          <p>• forms: 表单字段定义</p>
        </div>
      </el-alert>
      <el-input
        type="textarea"
        :rows="20"
        placeholder="请粘贴模块 JSON 配置"
        v-model="importJson">
      </el-input>
      <div slot="footer">
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport" :loading="importing">确定导入</el-button>
      </div>
    </el-dialog>
    
    <!-- 模块详情对话框 -->
    <el-dialog title="模块详情" :visible.sync="detailDialogVisible" width="70%">
      <el-descriptions :column="2" border v-if="currentModule">
        <el-descriptions-item label="模块ID">{{ currentModule.id }}</el-descriptions-item>
        <el-descriptions-item label="模块名称">{{ currentModule.name }}</el-descriptions-item>
        <el-descriptions-item label="流程键">{{ currentModule.processKey }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ currentModule.version }}</el-descriptions-item>
        <el-descriptions-item label="作者">{{ currentModule.author }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentModule.status)">{{ getStatusLabel(currentModule.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="部署时间">{{ currentModule.deployTime }}</el-descriptions-item>
        <el-descriptions-item label="部署来源">{{ currentModule.deploySource }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentModule.description }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider content-position="left">任务处理器</el-divider>
      
      <el-table :data="taskHandlers" style="width: 100%">
        <el-table-column prop="taskKey" label="任务键" width="150"></el-table-column>
        <el-table-column prop="taskName" label="任务名称" width="150"></el-table-column>
        <el-table-column prop="formKey" label="表单键" width="200"></el-table-column>
        <el-table-column prop="handlerType" label="处理器类型" width="120"></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listModules, getModuleDetail, importModule, exportModule, suspendModule, activateModule, removeModule } from "@/api/workflow/module";

export default {
  name: "ProcessModule",
  data() {
    return {
      moduleList: [],
      stats: {
        total: 0,
        active: 0,
        suspended: 0
      },
      recentActivities: [],
      importDialogVisible: false,
      detailDialogVisible: false,
      importJson: "",
      importing: false,
      currentModule: null,
      taskHandlers: [],
      uploadHeaders: {
        Authorization: 'Bearer ' + this.getToken()
      }
    };
  },
  created() {
    this.loadModuleList();
  },
  methods: {
    getToken() {
      return this.$store.getters.token;
    },
    loadModuleList() {
      listModules().then(response => {
        this.moduleList = response.data;
        this.updateStats();
      });
    },
    updateStats() {
      this.stats.total = this.moduleList.length;
      this.stats.active = this.moduleList.filter(m => m.status === 'active').length;
      this.stats.suspended = this.moduleList.filter(m => m.status === 'suspended').length;
    },
    getStatusType(status) {
      const types = {
        'active': 'success',
        'suspended': 'warning',
        'disabled': 'info'
      };
      return types[status] || 'info';
    },
    getStatusLabel(status) {
      const labels = {
        'active': '运行中',
        'suspended': '已暂停',
        'disabled': '已禁用'
      };
      return labels[status] || status;
    },
    handleImport() {
      this.importDialogVisible = true;
      this.importJson = "";
    },
    submitImport() {
      if (!this.importJson.trim()) {
        this.$message.warning("请输入模块配置");
        return;
      }
      this.importing = true;
      importModule(this.importJson).then(response => {
        this.$message.success("导入成功");
        this.importDialogVisible = false;
        this.loadModuleList();
        this.addActivity("导入模块成功");
      }).catch(() => {
        this.$message.error("导入失败");
      }).finally(() => {
        this.importing = false;
      });
    },
    handleView(row) {
      getModuleDetail(row.processKey).then(response => {
        this.currentModule = response.data;
        this.taskHandlers = Object.entries(response.data.taskHandlers || {}).map(([key, value]) => ({
          taskKey: key,
          ...value
        }));
        this.detailDialogVisible = true;
      });
    },
    handleExport(row) {
      exportModule(row.processKey).then(response => {
        const blob = new Blob([JSON.stringify(response.data, null, 2)], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${row.processKey}-module.json`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.$message.success("导出成功");
        this.addActivity(`导出模块: ${row.name}`);
      });
    },
    handleSuspend(row) {
      this.$confirm(`确认暂停模块 "${row.name}" 吗？`, "提示", {
        type: "warning"
      }).then(() => {
        suspendModule(row.processKey).then(() => {
          this.$message.success("已暂停");
          this.loadModuleList();
          this.addActivity(`暂停模块: ${row.name}`);
        });
      });
    },
    handleActivate(row) {
      activateModule(row.processKey).then(() => {
        this.$message.success("已激活");
        this.loadModuleList();
        this.addActivity(`激活模块: ${row.name}`);
      });
    },
    handleDelete(row) {
      this.$confirm(`确认删除模块 "${row.name}" 吗？此操作不可恢复！`, "警告", {
        type: "warning"
      }).then(() => {
        removeModule(row.processKey).then(() => {
          this.$message.success("已删除");
          this.loadModuleList();
          this.addActivity(`删除模块: ${row.name}`);
        });
      });
    },
    handleCreate() {
      this.$message.info("请使用导入功能或上传模块文件");
    },
    handleUploadSuccess(response) {
      this.$message.success("上传成功");
      this.loadModuleList();
      this.addActivity("上传模块成功");
    },
    handleUploadError() {
      this.$message.error("上传失败");
    },
    addActivity(content) {
      this.recentActivities.unshift({
        content,
        time: new Date().toLocaleString()
      });
      if (this.recentActivities.length > 10) {
        this.recentActivities.pop();
      }
    }
  }
};
</script>

<style scoped>
.quick-actions {
  padding: 10px 0;
}

.stats {
  margin-top: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.stat-item.active {
  background: #f0f9ff;
}

.stat-item.suspended {
  background: #fef9e7;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.stat-item.active .stat-value {
  color: #67C23A;
}

.stat-item.suspended .stat-value {
  color: #E6A23C;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
