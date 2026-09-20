<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:notice:add'" @click="openForm()"> 新增公告 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="标题" clearable class="search-item" />
          <t-select v-model="query.noticeType" placeholder="类型" clearable class="search-item select">
            <t-option v-for="item in noticeTypeDict.items.value" :key="item.value" :label="item.label" :value="item.value" />
          </t-select>
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
        <template #noticeType="{ row }">
          <t-tag variant="outline">{{ noticeTypeDict.label(row.noticeType) }}</t-tag>
        </template>
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'success' : 'default'" variant="light">
            {{ row.enabled ? '启用' : '停用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:notice:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="primary" v-permission="'system:notice:list'" @click="openView(row)"> 查看 </t-link>
            <t-link theme="danger" v-permission="'system:notice:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑公告' : '新增公告'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="560px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="标题" name="noticeTitle">
          <t-input v-model="form.noticeTitle" />
        </t-form-item>
        <t-form-item label="类型" name="noticeType">
          <t-select v-model="form.noticeType" clearable>
            <t-option v-for="item in noticeTypeDict.items.value" :key="item.value" :label="item.label" :value="item.value" />
          </t-select>
        </t-form-item>
        <t-form-item label="内容" name="content">
          <t-textarea v-model="form.content" :autosize="{ minRows: 5 }" placeholder="纯文本内容" />
        </t-form-item>
        <t-form-item label="是否启用" name="enabled">
          <t-switch v-model="form.enabled" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog v-model:visible="viewVisible" :header="viewTarget?.noticeTitle" width="560px" :footer="false">
      <div class="view-body">
        <p>
          <t-tag variant="outline">{{ noticeTypeDict.label(viewTarget?.noticeType) }}</t-tag>
        </p>
        <p class="view-content">{{ viewTarget?.content || '（无内容）' }}</p>
      </div>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除公告「${deleteTarget?.noticeTitle}」？`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { NoticeDto } from '@/api/__generated/model/dto';
  import type { NoticeInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';
  import { useDict } from '@/hooks/useDict';

  type NoticeRow = NoticeDto['NoticeService/DEFAULT_FETCHER'];

  // 公告类型消费字典 sys_notice_type(工单03 种子)
  const noticeTypeDict = useDict('sys_notice_type');

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 70 },
    { colKey: 'noticeTitle', title: '标题' },
    { colKey: 'noticeType', title: '类型', width: 100, cell: 'noticeType' },
    { colKey: 'enabled', title: '状态', width: 90, cell: 'enabled' },
    { colKey: 'createdTime', title: '创建时间', width: 180 },
    { colKey: 'op', title: '操作', width: 150 },
  ];

  const query = reactive({
    keyword: '',
    noticeType: undefined as string | undefined,
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<NoticeRow[]>([]);
  const loading = ref(false);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    noticeTitle: '',
    noticeType: '',
    content: '',
    enabled: true,
  });

  const rules: FormRules = {
    noticeTitle: [{ required: true, message: '标题不能为空', type: 'error' }],
    noticeType: [{ required: true, message: '类型不能为空', type: 'error' }],
  };

  const viewVisible = ref(false);
  const viewTarget = ref<NoticeRow>();

  const deleteVisible = ref(false);
  const deleteTarget = ref<NoticeRow>();

  async function load() {
    loading.value = true;
    try {
      const page = await api.noticeService.findNoticesBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'id asc',
        specification: {
          keyword: query.keyword || undefined,
          noticeType: query.noticeType,
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
    query.noticeType = undefined;
    search();
  }

  function onPageChange(pageInfo: PageInfo) {
    pagination.current = pageInfo.current;
    pagination.pageSize = pageInfo.pageSize;
    load();
  }

  function openForm(row?: NoticeRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.noticeTitle = row?.noticeTitle ?? '';
    form.noticeType = row?.noticeType ?? '';
    form.content = row?.content ?? '';
    form.enabled = row?.enabled ?? true;
    formVisible.value = true;
  }

  function openView(row: NoticeRow) {
    viewTarget.value = row;
    viewVisible.value = true;
  }

  async function save() {
    const valid = await formInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: NoticeInput = {
        id: form.id,
        noticeTitle: form.noticeTitle,
        noticeType: form.noticeType,
        content: form.content ? form.content : undefined,
        enabled: form.enabled,
      };
      await api.noticeService.saveNotice({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: NoticeRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.noticeService.deleteNotice({ id: deleteTarget.value.id });
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

    &.select {
      width: 110px;
    }
  }

  .view-content {
    white-space: pre-wrap;
    line-height: 1.7;
  }
</style>
