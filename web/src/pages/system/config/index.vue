<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:config:add'" @click="openForm()"> 新增参数 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="键/名称" clearable class="search-item" />
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
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:config:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:config:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑参数' : '新增参数'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="520px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="参数键" name="configKey">
          <t-input v-model="form.configKey" placeholder="小写字母开头，如 site.title" />
        </t-form-item>
        <t-form-item label="参数名称" name="configName">
          <t-input v-model="form.configName" />
        </t-form-item>
        <t-form-item label="参数值" name="configValue">
          <t-input v-model="form.configValue" />
        </t-form-item>
        <t-form-item label="备注" name="remark">
          <t-textarea v-model="form.remark" :autosize="{ minRows: 2 }" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除参数「${deleteTarget?.configKey}」？`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { ConfigDto } from '@/api/__generated/model/dto';
  import type { ConfigInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type ConfigRow = ConfigDto['ConfigService/DEFAULT_FETCHER'];

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 70 },
    { colKey: 'configKey', title: '参数键' },
    { colKey: 'configName', title: '参数名称' },
    { colKey: 'configValue', title: '参数值' },
    { colKey: 'remark', title: '备注' },
    { colKey: 'op', title: '操作', width: 120 },
  ];

  const query = reactive({ keyword: '' });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<ConfigRow[]>([]);
  const loading = ref(false);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    configKey: '',
    configName: '',
    configValue: '',
    remark: '',
  });

  const rules: FormRules = {
    configKey: [
      { required: true, message: '参数键不能为空', type: 'error' },
      { pattern: /^[a-z][a-z0-9.]*$/, message: '小写字母开头，可含小写字母/数字/点', type: 'error' },
    ],
    configName: [{ required: true, message: '参数名称不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<ConfigRow>();

  async function load() {
    loading.value = true;
    try {
      const page = await api.configService.findConfigsBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'configKey asc',
        specification: { keyword: query.keyword || undefined },
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

  function openForm(row?: ConfigRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.configKey = row?.configKey ?? '';
    form.configName = row?.configName ?? '';
    form.configValue = row?.configValue ?? '';
    form.remark = row?.remark ?? '';
    formVisible.value = true;
  }

  async function save() {
    const valid = await formInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: ConfigInput = {
        id: form.id,
        configKey: form.configKey,
        configName: form.configName,
        configValue: form.configValue ? form.configValue : undefined,
        remark: form.remark ? form.remark : undefined,
      };
      await api.configService.saveConfig({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: ConfigRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.configService.deleteConfig({ id: deleteTarget.value.id });
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
