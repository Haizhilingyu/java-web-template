<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'job:add'" @click="openForm()"> 新增任务 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="名称/处理器" clearable class="search-item" />
          <t-select v-model="query.status" placeholder="状态" clearable class="search-item select">
            <t-option label="调度中" :value="0" />
            <t-option label="已暂停" :value="1" />
          </t-select>
          <t-button theme="primary" variant="base" @click="search"> 查询 </t-button>
          <t-button variant="outline" @click="reset"> 重置 </t-button>
        </t-space>
      </t-row>

      <t-table row-key="id" :data="data" :columns="columns" :loading="loading" :pagination="pagination" @page-change="onPageChange">
        <template #status="{ row }">
          <t-tag :theme="row.status === 0 ? 'success' : 'default'" variant="light">
            {{ row.status === 0 ? '调度中' : '已暂停' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'job:run'" @click="runOnce(row)"> 执行 </t-link>
            <t-link :theme="row.status === 0 ? 'warning' : 'success'" v-permission="'job:status'" @click="toggleStatus(row)">
              {{ row.status === 0 ? '暂停' : '恢复' }}
            </t-link>
            <t-link theme="primary" v-permission="'job:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'job:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑任务' : '新增任务'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="520px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="任务名" name="name">
          <t-input v-model="form.name" placeholder="同一任务名不重复" />
        </t-form-item>
        <t-form-item label="cron" name="cron">
          <t-input v-model="form.cron" placeholder="如 0/10 * * * * ?，保存时校验" />
        </t-form-item>
        <t-form-item label="处理器" name="handler">
          <t-input v-model="form.handler" placeholder="Spring Bean 名，如 sampleLogJob" />
        </t-form-item>
        <t-form-item label="参数" name="param">
          <t-input v-model="form.param" placeholder="透传给处理器的参数，可留空" />
        </t-form-item>
        <t-form-item label="备注" name="memo">
          <t-textarea v-model="form.memo" :autosize="{ minRows: 2 }" placeholder="可留空" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除任务「${deleteTarget?.name}」？调度中的任务会先摘除计划。`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { SysJobDto } from '@/api/__generated/model/dto';
  import type { SysJobInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type JobRow = SysJobDto['JobService/DEFAULT_FETCHER'];

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 70 },
    { colKey: 'name', title: '任务名' },
    { colKey: 'cron', title: 'cron 表达式' },
    { colKey: 'handler', title: '处理器', width: 140 },
    { colKey: 'param', title: '参数' },
    { colKey: 'status', title: '状态', width: 90, cell: 'status' },
    { colKey: 'memo', title: '备注' },
    { colKey: 'op', title: '操作', width: 190 },
  ];

  const query = reactive({
    keyword: '',
    status: undefined as number | undefined,
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<JobRow[]>([]);
  const loading = ref(false);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    name: '',
    cron: '',
    handler: 'sampleLogJob',
    param: '',
    memo: '',
  });

  const rules: FormRules = {
    name: [{ required: true, message: '任务名不能为空', type: 'error' }],
    cron: [{ required: true, message: 'cron 表达式不能为空', type: 'error' }],
    handler: [{ required: true, message: '处理器不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<JobRow>();

  async function load() {
    loading.value = true;
    try {
      const page = await api.jobService.findJobsBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'id asc',
        specification: {
          keyword: query.keyword || undefined,
          status: query.status,
        },
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
    query.status = undefined;
    search();
  }

  function onPageChange(pageInfo: PageInfo) {
    pagination.current = pageInfo.current;
    pagination.pageSize = pageInfo.pageSize;
    load();
  }

  function openForm(row?: JobRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.name = row?.name ?? '';
    form.cron = row?.cron ?? '';
    form.handler = row?.handler ?? 'sampleLogJob';
    form.param = row?.param ?? '';
    form.memo = row?.memo ?? '';
    formVisible.value = true;
  }

  async function save() {
    const valid = await formInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: SysJobInput = {
        id: form.id,
        name: form.name,
        cron: form.cron,
        handler: form.handler,
        param: form.param ? form.param : undefined,
        // 编辑时不动启停状态；新增默认暂停，由用户手动恢复
        status: form.id ? (data.value.find((job) => job.id === form.id)?.status ?? 1) : 1,
        memo: form.memo ? form.memo : undefined,
      };
      await api.jobService.saveJob({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  async function toggleStatus(row: JobRow) {
    try {
      await api.jobService.changeStatus({ id: row.id, status: row.status === 0 ? 1 : 0 });
      MessagePlugin.success(row.status === 0 ? '已暂停' : '已恢复调度');
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  async function runOnce(row: JobRow) {
    try {
      await api.jobService.run({ id: row.id });
      MessagePlugin.success(`任务「${row.name}」执行完成，详情见后端日志`);
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  function confirmDelete(row: JobRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.jobService.deleteJob({ id: deleteTarget.value.id });
      MessagePlugin.success('删除成功');
      deleteVisible.value = false;
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
