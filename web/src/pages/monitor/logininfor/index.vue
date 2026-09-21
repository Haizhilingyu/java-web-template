<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:log:clear'" theme="danger" variant="outline" @click="clearVisible = true">
            清空日志
          </t-button>
          <t-button v-permission="'system:log:export'" variant="outline" :loading="exporting" @click="onExport">
            导出
          </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="账号/IP/消息" clearable class="search-item" />
          <t-button theme="primary" variant="base" @click="search"> 查询 </t-button>
          <t-button variant="outline" @click="reset"> 重置 </t-button>
        </t-space>
      </t-row>

      <t-table
        row-key="id"
        :data="data"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @page-change="onPageChange"
      />
    </t-card>

    <t-dialog
      v-model:visible="clearVisible"
      header="清空确认"
      body="确认清空全部登录日志？该操作不可恢复。"
      @confirm="doClear"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { LogininforDto } from '@/api/__generated/model/dto';
  import { api } from '@/api/jimmer';
  import { downloadFile } from '@/api/download';

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'username', title: '账号' },
    { colKey: 'ip', title: 'IP', width: 160 },
    { colKey: 'message', title: '消息' },
    { colKey: 'createdTime', title: '时间', width: 180 },
  ];

  const query = reactive({ keyword: '' });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  type LogininforRow = LogininforDto['LogininforService/DEFAULT_FETCHER'];

  const data = ref<LogininforRow[]>([]);
  const loading = ref(false);
  const clearVisible = ref(false);
  const exporting = ref(false);

  async function onExport() {
    exporting.value = true;
    try {
      await downloadFile('/api/v1/logininfor/export', { keyword: query.keyword || undefined });
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      exporting.value = false;
    }
  }

  async function load() {
    loading.value = true;
    try {
      const page = await api.logininforService.findLogininforsBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'id desc',
        keyword: query.keyword || undefined,
      });
      data.value = [...page.content];
      pagination.total = page.totalElements;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      loading.value = false;
    }
  }

  function search() {
    pagination.current = 1;
    load();
  }

  function reset() {
    query.keyword = '';
    search();
  }

  function onPageChange(pageInfo: PageInfo) {
    pagination.current = pageInfo.current;
    pagination.pageSize = pageInfo.pageSize;
    load();
  }

  async function doClear() {
    try {
      await api.logininforService.clear();
      MessagePlugin.success('已清空');
      clearVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  onMounted(load);
</script>

<style scoped lang="less">
  .left-operation-container {
    display: flex;
    align-items: center;
  }

  .search-item {
    width: 160px;
  }
</style>
