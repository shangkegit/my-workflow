<template>
  <el-dialog title="插件详情" :visible.sync="visible" width="500px" append-to-body>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="插件ID">{{ plugin.pluginId }}</el-descriptions-item>
      <el-descriptions-item label="插件名称">{{ plugin.pluginName }}</el-descriptions-item>
      <el-descriptions-item label="流程标识">{{ plugin.processKey }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ plugin.version }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="plugin.status === 'ENABLED' ? 'success' : 'info'">
          {{ plugin.status === 'ENABLED' ? '启用' : '禁用' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="安装时间">{{ plugin.installTime }}</el-descriptions-item>
      <el-descriptions-item label="作者">{{ plugin.author }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ plugin.description }}</el-descriptions-item>
    </el-descriptions>

    <div slot="footer">
      <el-button @click="visible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'DetailDialog',
  data() {
    return {
      visible: false,
      plugin: {}
    }
  },
  methods: {
    show(plugin) {
      this.plugin = plugin
      this.visible = true
    }
  }
}
</script>
