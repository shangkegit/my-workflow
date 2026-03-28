<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['plugin:import']"
        >导入</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="pluginList">
      <el-table-column label="插件名称" prop="pluginName" />
      <el-table-column label="流程标识" prop="processKey" />
      <el-table-column label="版本" prop="version" width="80" />
      <el-table-column label="状态" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="安装时间" prop="installTime" width="160" />
      <el-table-column label="作者" prop="author" width="100" />
      <el-table-column label="操作" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="handleDetail(scope.row)">详情</el-button>
          <el-button size="mini" type="text" @click="handleUninstall(scope.row)"
                     v-hasPermi="['plugin:remove']">卸载</el-button>
        </template>
      </el-table-column>
    </el-table>

    <import-dialog ref="importDialog" @success="getList" />
    <detail-dialog ref="detailDialog" />
  </div>
</template>

<script>
import { listPlugins, uninstallPlugin } from './api/plugin'
import ImportDialog from './importDialog'
import DetailDialog from './detailDialog'

export default {
  name: 'Plugin',
  components: { ImportDialog, DetailDialog },
  data() {
    return {
      loading: false,
      pluginList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPlugins().then(res => {
        this.pluginList = res.rows || res.data || []
        this.loading = false
      })
    },
    handleImport() {
      this.$refs.importDialog.show()
    },
    handleDetail(row) {
      this.$refs.detailDialog.show(row)
    },
    handleUninstall(row) {
      this.$confirm('确认卸载插件 [' + row.pluginName + ']?', '警告', {
        type: 'warning'
      }).then(() => {
        uninstallPlugin(row.pluginId).then(() => {
          this.$message.success('卸载成功')
          this.getList()
        })
      })
    }
  }
}
</script>
