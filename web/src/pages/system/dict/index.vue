<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:dict:add'" @click="openTypeForm()"> 新增字典 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="typeQuery.keyword" placeholder="编码/名称" clearable class="search-item" />
          <t-button theme="primary" variant="base" @click="searchTypes"> 查询 </t-button>
          <t-button variant="outline" @click="resetTypes"> 重置 </t-button>
        </t-space>
      </t-row>

      <t-table
        row-key="id"
        :data="typeData"
        :columns="typeColumns"
        :loading="typeLoading"
        :pagination="typePagination"
        :active-row-type="'single'"
        :on-row-click="onTypeRowClick"
        @page-change="onTypePageChange"
      >
        <template #type="{ row }">
          <t-tag
            :variant="selectedTypeId === row.id ? 'light' : 'outline'"
            :theme="selectedTypeId === row.id ? 'primary' : 'default'"
          >
            {{ row.type }}
          </t-tag>
        </template>
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'success' : 'default'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:dict:add'" @click.stop="selectType(row)"> 条目 </t-link>
            <t-link theme="primary" v-permission="'system:dict:edit'" @click.stop="openTypeForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:dict:delete'" @click.stop="confirmDeleteType(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-card v-if="selectedType" class="list-card-container detail-card" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <span class="detail-title">「{{ selectedType.name }}」条目</span>
        </div>
        <t-space break-line>
          <t-input v-model="dataQuery.keyword" placeholder="标签/值" clearable class="search-item" />
          <t-button theme="primary" variant="base" @click="searchData"> 查询 </t-button>
          <t-button v-permission="'system:dict:add'" variant="outline" @click="openDataForm()"> 新增条目 </t-button>
        </t-space>
      </t-row>

      <t-table
        row-key="id"
        :data="dataData"
        :columns="dataColumns"
        :loading="dataLoading"
        :pagination="dataPagination"
        @page-change="onDataPageChange"
      >
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'success' : 'default'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:dict:edit'" @click="openDataForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:dict:delete'" @click="confirmDeleteData(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="typeFormVisible"
      :header="typeForm.id ? '编辑字典' : '新增字典'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="480px"
      @confirm="saveType"
      @closed="typeFormInstance?.reset()"
    >
      <t-form ref="typeFormInstance" :data="typeForm" :rules="typeRules" label-width="80px" @submit.prevent>
        <t-form-item label="编码" name="type">
          <t-input v-model="typeForm.type" placeholder="小写字母开头，如 sys_yes_no" />
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="typeForm.name" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="typeForm.description" :autosize="{ minRows: 2 }" />
        </t-form-item>
        <t-form-item label="是否启用" name="enabled">
          <t-switch v-model="typeForm.enabled" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="dataFormVisible"
      :header="dataForm.id ? '编辑条目' : '新增条目'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="480px"
      @confirm="saveData"
      @closed="dataFormInstance?.reset()"
    >
      <t-form ref="dataFormInstance" :data="dataForm" :rules="dataRules" label-width="80px" @submit.prevent>
        <t-form-item label="标签" name="label">
          <t-input v-model="dataForm.label" placeholder="显示文案，如 是" />
        </t-form-item>
        <t-form-item label="值" name="value">
          <t-input v-model="dataForm.value" placeholder="存储值，如 Y" />
        </t-form-item>
        <t-form-item label="排序号" name="sortOrder">
          <t-input-number v-model="dataForm.sortOrder" :min="0" theme="column" style="width: 100%" />
        </t-form-item>
        <t-form-item label="是否启用" name="enabled">
          <t-switch v-model="dataForm.enabled" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="deleteBody"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableRowData, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { DictDataDto, DictTypeDto } from '@/api/__generated/model/dto';
  import type { DictDataInput, DictTypeInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';
  import { clearDictCache } from '@/hooks/useDict';

  type DictTypeRow = DictTypeDto['DictTypeService/DEFAULT_FETCHER'];
  type DictDataRow = DictDataDto['DictDataService/DEFAULT_FETCHER'];

  const typeColumns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 70 },
    { colKey: 'type', title: '编码', cell: 'type' },
    { colKey: 'name', title: '名称' },
    { colKey: 'description', title: '描述' },
    { colKey: 'enabled', title: '状态', width: 90, cell: 'enabled' },
    { colKey: 'op', title: '操作', width: 170 },
  ];

  const dataColumns: TableProps['columns'] = [
    { colKey: 'label', title: '标签' },
    { colKey: 'value', title: '值' },
    { colKey: 'sortOrder', title: '排序号', width: 90 },
    { colKey: 'enabled', title: '状态', width: 90, cell: 'enabled' },
    { colKey: 'op', title: '操作', width: 120 },
  ];

  const typeQuery = reactive({ keyword: '' });
  const typePagination = reactive({ current: 1, pageSize: 10, total: 0, showJumper: true });
  const typeData = ref<DictTypeRow[]>([]);
  const typeLoading = ref(false);
  const selectedTypeId = ref<number>();

  const dataQuery = reactive({ keyword: '' });
  const dataPagination = reactive({ current: 1, pageSize: 10, total: 0, showJumper: true });
  const dataData = ref<DictDataRow[]>([]);
  const dataLoading = ref(false);

  const selectedType = ref<DictTypeRow>();

  const saving = ref(false);
  const typeFormVisible = ref(false);
  const typeFormInstance = ref<FormInstanceFunctions>();
  const typeForm = reactive({
    id: undefined as number | undefined,
    type: '',
    name: '',
    description: '',
    enabled: true,
  });
  const typeRules: FormRules = {
    type: [
      { required: true, message: '编码不能为空', type: 'error' },
      { pattern: /^[a-z][a-z0-9_]*$/, message: '小写字母开头，可含小写字母/数字/下划线', type: 'error' },
    ],
    name: [{ required: true, message: '名称不能为空', type: 'error' }],
  };

  const dataFormVisible = ref(false);
  const dataFormInstance = ref<FormInstanceFunctions>();
  const dataForm = reactive({
    id: undefined as number | undefined,
    label: '',
    value: '',
    sortOrder: 0,
    enabled: true,
  });
  const dataRules: FormRules = {
    label: [{ required: true, message: '标签不能为空', type: 'error' }],
    value: [{ required: true, message: '值不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteBody = ref('');
  let deleteAction: () => Promise<void>;

  async function loadTypes() {
    typeLoading.value = true;
    try {
      const page = await api.dictTypeService.findDictTypesBySuperQBE({
        pageIndex: typePagination.current - 1,
        pageSize: typePagination.pageSize,
        sortCode: 'type asc',
        specification: { keyword: typeQuery.keyword || undefined },
      });
      typeData.value = [...page.content];
      typePagination.total = page.totalElements;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      typeLoading.value = false;
    }
  }

  function searchTypes() {
    typePagination.current = 1;
    loadTypes();
  }

  function resetTypes() {
    typeQuery.keyword = '';
    searchTypes();
  }

  function onTypePageChange(pageInfo: PageInfo) {
    typePagination.current = pageInfo.current;
    typePagination.pageSize = pageInfo.pageSize;
    loadTypes();
  }

  function selectType(row: DictTypeRow) {
    selectedType.value = row;
    selectedTypeId.value = row.id;
    dataQuery.keyword = '';
    dataPagination.current = 1;
    loadData();
  }

  function onTypeRowClick(context: { row: TableRowData }) {
    selectType(context.row as unknown as DictTypeRow);
  }

  async function loadData() {
    if (!selectedTypeId.value) {
      return;
    }
    dataLoading.value = true;
    try {
      const page = await api.dictDataService.findDictDataBySuperQBE({
        pageIndex: dataPagination.current - 1,
        pageSize: dataPagination.pageSize,
        sortCode: 'sortOrder asc',
        specification: { keyword: dataQuery.keyword || undefined },
        dictTypeId: selectedTypeId.value,
      });
      dataData.value = [...page.content];
      dataPagination.total = page.totalElements;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      dataLoading.value = false;
    }
  }

  function searchData() {
    dataPagination.current = 1;
    loadData();
  }

  function onDataPageChange(pageInfo: PageInfo) {
    dataPagination.current = pageInfo.current;
    dataPagination.pageSize = pageInfo.pageSize;
    loadData();
  }

  function openTypeForm(row?: DictTypeRow) {
    typeFormInstance.value?.reset();
    typeForm.id = row?.id;
    typeForm.type = row?.type ?? '';
    typeForm.name = row?.name ?? '';
    typeForm.description = row?.description ?? '';
    typeForm.enabled = row?.enabled ?? true;
    typeFormVisible.value = true;
  }

  async function saveType() {
    const valid = await typeFormInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: DictTypeInput = {
        id: typeForm.id,
        type: typeForm.type,
        name: typeForm.name,
        description: typeForm.description ? typeForm.description : undefined,
        enabled: typeForm.enabled,
      };
      const saved = await api.dictTypeService.saveDictType({ body: input });
      clearDictCache(saved.type);
      MessagePlugin.success('保存成功');
      typeFormVisible.value = false;
      loadTypes();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function openDataForm(row?: DictDataRow) {
    dataFormInstance.value?.reset();
    dataForm.id = row?.id;
    dataForm.label = row?.label ?? '';
    dataForm.value = row?.value ?? '';
    dataForm.sortOrder = row?.sortOrder ?? 0;
    dataForm.enabled = row?.enabled ?? true;
    dataFormVisible.value = true;
  }

  async function saveData() {
    const valid = await dataFormInstance.value?.validate();
    if (valid !== true || !selectedType.value) {
      return;
    }
    saving.value = true;
    try {
      const input: DictDataInput = {
        id: dataForm.id,
        dictTypeId: selectedType.value.id,
        label: dataForm.label,
        value: dataForm.value,
        sortOrder: dataForm.sortOrder,
        enabled: dataForm.enabled,
      };
      await api.dictDataService.saveDictData({ body: input });
      clearDictCache(selectedType.value.type);
      MessagePlugin.success('保存成功');
      dataFormVisible.value = false;
      loadData();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDeleteType(row: DictTypeRow) {
    deleteBody.value = `确认删除字典「${row.name}」？存在条目时将被拒绝。`;
    deleteAction = async () => {
      await api.dictTypeService.deleteDictType({ id: row.id });
      if (selectedTypeId.value === row.id) {
        selectedType.value = undefined;
        selectedTypeId.value = undefined;
      }
      MessagePlugin.success('删除成功');
      deleteVisible.value = false;
      clearDictCache(row.type);
      loadTypes();
    };
    deleteVisible.value = true;
  }

  function confirmDeleteData(row: DictDataRow) {
    deleteBody.value = `确认删除条目「${row.label}」？`;
    deleteAction = async () => {
      await api.dictDataService.deleteDictData({ id: row.id });
      MessagePlugin.success('删除成功');
      deleteVisible.value = false;
      if (selectedType.value) {
        clearDictCache(selectedType.value.type);
      }
      loadData();
    };
    deleteVisible.value = true;
  }

  async function doDelete() {
    try {
      await deleteAction();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  onMounted(loadTypes);
</script>

<style scoped lang="less">
  .left-operation-container {
    display: flex;
    align-items: center;
  }

  .search-item {
    width: 160px;
  }

  .detail-card {
    margin-top: 16px;
  }

  .detail-title {
    font-weight: 600;
  }
</style>
