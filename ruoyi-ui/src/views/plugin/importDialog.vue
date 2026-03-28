<template>
  <el-dialog title="导入插件" :visible.sync="visible" width="500px" append-to-body>
    <el-upload
      ref="upload"
      :limit="1"
      accept=".zip"
      :auto-upload="false"
      :on-change="handleFileChange"
      :file-list="fileList"
      drag
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <div class="el-upload__tip" slot="tip">支持 .zip 格式的流程包</div>
    </el-upload>

    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="handleImport">导入</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { importPlugin } from './api/plugin'

export default {
  name: 'ImportDialog',
  data() {
    return {
      visible: false,
      uploading: false,
      fileList: [],
      file: null
    }
  },
  methods: {
    show() {
      this.visible = true
      this.fileList = []
      this.file = null
    },
    handleFileChange(file, fileList) {
      this.file = file.raw
    },
    handleImport() {
      if (!this.file) {
        this.$message.warning('请选择文件')
        return
      }

      this.uploading = true
      const formData = new FormData()
      formData.append('file', this.file)

      importPlugin(formData).then(res => {
        this.uploading = false
        if (res.code === 200) {
          this.$message.success('导入成功')
          this.visible = false
          this.$emit('success')
        } else {
          this.$message.error(res.msg || '导入失败')
        }
      }).catch(() => {
        this.uploading = false
        this.$message.error('导入失败')
      })
    }
  }
}
</script>
