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
          <t-input v-model="query.keyword" placeholder="模块/动作/操作人/URI" clearable class="search-item" />
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
      >
        <template #success="{ row }">
          <t-tag :theme="row.success ? 'success' : 'danger'" variant="light">
            {{ row.success ? '成功' : '失败' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-link theme="primary" v-permission="'system:log:list'" @click="openDetail(row)"> 详情 </t-link>
        </template>
      </t-table>
    </t-card>

    <t-dialog v-model:visible="detailVisible" header="操作详情" width="640px" :footer="false">
      <div class="detail-body">
        <p>
          <t-tag variant="outline">{{ detail?.module }}</t-tag>
          <t-tag variant="outline" class="detail-tag">{{ detail?.action }}</t-tag>
          <t-tag :theme="detail?.success ? 'success' : 'danger'" variant="light">
            {{ detail?.success ? '成功' : '失败' }}
          </t-tag>
        </p>
        <p class="detail-line">操作人：{{ detail?.operator }} · 耗时 {{ detail?.costMs }}ms</p>
        <p class="detail-line">URI：{{ detail?.uri || '-' }}</p>
        <t-collapse :default-value="['params']">
          <t-collapse-panel value="params" header="入参">
            <pre class="detail-pre">{{ detail?.params || '（无）' }}</pre>
          </t-collapse-panel>
          <t-collapse-panel value="result" header="结果">
            <pre class="detail-pre">{{ detail?.result || '（无）' }}</pre>
          </t-collapse-panel>
          <t-collapse-panel v-if="detail?.errorMsg" value="error" header="异常">
            <pre class="detail-pre detail-error">{{ detail.errorMsg }}</pre>
          </t-collapse-panel>
        </t-collapse>
      </div>
    </t-dialog>

    <t-dialog
      v-model:visible="clearVisible"
      header="清空确认"
      body="确认清空全部操作日志？该操作不可恢复。"
      @confirm="doClear"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { OperLogDto } from '@/api/__generated/model/dto';
  import { api } from '@/api/jimmer';
  import { downloadFile } from '@/api/download';

  type OperLogRow = OperLogDto['OperLogService/DEFAULT_FETCHER'];

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 70 },
    { colKey: 'module', title: '模块', width: 110 },
    { colKey: 'action', title: '动作', width: 110 },
    { colKey: 'operator', title: '操作人', width: 110 },
    { colKey: 'uri', title: 'URI' },
    { colKey: 'costMs', title: '耗时(ms)', width: 90 },
    { colKey: 'success', title: '状态', width: 80, cell: 'success' },
    { colKey: 'op', title: '操作', width: 80 },
  ];

  const query = reactive({ keyword: '' });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<OperLogRow[]>([]);
  const loading = ref(false);
  const clearVisible = ref(false);
  const exporting = ref(false);
  const detailVisible = ref(false);
  const detail = ref<OperLogRow>();

  async function onExport() {
    exporting.value = true;
    try {
      await downloadFile('/api/v1/operlog/export', { keyword: query.keyword || undefined });
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      exporting.value = false;
    }
  }

  async function load() {
    loading.value = true;
    try {
      const page = await api.operLogService.findOperLogsBySuperQBE({
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

  function openDetail(row: OperLogRow) {
    detail.value = row;
    detailVisible.value = true;
  }

  async function doClear() {
    try {
      await api.operLogService.clear();
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
    width: 200px;
  }

  .detail-body {
    line-height: 1.8;
  }

  .detail-tag {
    margin: 0 8px;
  }

  .detail-line {
    color: var(--td-text-color-secondary);
  }

  .detail-pre {
    white-space: pre-wrap;
    word-break: break-all;
    margin: 0;
    font-size: 12px;
  }

  .detail-error {
    color: var(--td-error-color);
  }
</style>
