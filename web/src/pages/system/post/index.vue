<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:post:add'" @click="openForm()"> 新增岗位 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="编码/名称" clearable class="search-item" />
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
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'success' : 'default'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:post:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:post:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑岗位' : '新增岗位'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="480px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="编码" name="code">
          <t-input v-model="form.code" placeholder="大写字母开头，可含大写字母/数字/下划线" />
        </t-form-item>
        <t-form-item label="名称" name="name">
          <t-input v-model="form.name" />
        </t-form-item>
        <t-form-item label="排序号" name="sortOrder">
          <t-input-number v-model="form.sortOrder" :min="0" theme="column" style="width: 100%" />
        </t-form-item>
        <t-form-item label="是否启用" name="enabled">
          <t-switch v-model="form.enabled" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除岗位「${deleteTarget?.name}」？已被用户绑定时将被拒绝。`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { PostDto } from '@/api/__generated/model/dto';
  import type { PostInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type PostRow = PostDto['PostService/DEFAULT_FETCHER'];

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'code', title: '编码' },
    { colKey: 'name', title: '名称' },
    { colKey: 'sortOrder', title: '排序号', width: 90 },
    { colKey: 'enabled', title: '状态', width: 90, cell: 'enabled' },
    { colKey: 'op', title: '操作', width: 120 },
  ];

  const query = reactive({
    keyword: '',
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<PostRow[]>([]);
  const loading = ref(false);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    code: '',
    name: '',
    sortOrder: 0,
    enabled: true,
  });

  const rules: FormRules = {
    code: [
      { required: true, message: '编码不能为空', type: 'error' },
      { pattern: /^[A-Z][A-Z0-9_]*$/, message: '大写字母开头，可含大写字母/数字/下划线', type: 'error' },
    ],
    name: [{ required: true, message: '名称不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<PostRow>();

  async function load() {
    loading.value = true;
    try {
      const page = await api.postService.findPostsBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'sortOrder asc',
        specification: {
          keyword: query.keyword || undefined,
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
    search();
  }

  function onPageChange(pageInfo: PageInfo) {
    pagination.current = pageInfo.current;
    pagination.pageSize = pageInfo.pageSize;
    load();
  }

  function openForm(row?: PostRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.code = row?.code ?? '';
    form.name = row?.name ?? '';
    form.sortOrder = row?.sortOrder ?? 0;
    form.enabled = row?.enabled ?? true;
    formVisible.value = true;
  }

  async function save() {
    const valid = await formInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: PostInput = {
        id: form.id,
        code: form.code,
        name: form.name,
        sortOrder: form.sortOrder,
        enabled: form.enabled,
      };
      await api.postService.savePost({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: PostRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.postService.deletePost({ id: deleteTarget.value.id });
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
