<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:user:add'" @click="openForm()"> 新增用户 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="用户名/昵称" clearable class="search-item" />
          <t-select v-model="query.enabled" placeholder="状态" clearable class="search-item select">
            <t-option label="启用" :value="true" />
            <t-option label="禁用" :value="false" />
          </t-select>
          <t-input v-model="query.roleName" placeholder="角色名" clearable class="search-item" />
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
          <t-tag :theme="row.enabled ? 'success' : 'danger'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #roles="{ row }">
          <t-space size="small">
            <t-tag v-for="role in row.roles" :key="role.id" variant="outline">{{ role.name }}</t-tag>
          </t-space>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:user:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:user:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑用户' : '新增用户'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="520px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="用户名" name="username">
          <t-input v-model="form.username" placeholder="字母、数字或下划线，3~50 位" />
        </t-form-item>
        <t-form-item label="密码" name="password">
          <t-input v-model="form.password" type="password" :placeholder="form.id ? '留空则不修改' : ''" />
        </t-form-item>
        <t-form-item label="昵称" name="nickname">
          <t-input v-model="form.nickname" />
        </t-form-item>
        <t-form-item label="角色" name="roleIds">
          <t-select v-model="form.roleIds" multiple clearable :options="roleOptions" placeholder="选择角色" />
        </t-form-item>
        <t-form-item label="启用" name="enabled">
          <t-switch v-model="form.enabled" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除用户「${deleteTarget?.username}」？`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { UserDto } from '@/api/__generated/model/dto';
  import type { UserInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type UserRow = UserDto['UserService/DEFAULT_FETCHER'];

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'username', title: '用户名' },
    { colKey: 'nickname', title: '昵称' },
    { colKey: 'enabled', title: '状态', width: 100 },
    { colKey: 'roles', title: '角色' },
    { colKey: 'createdTime', title: '创建时间', width: 180 },
    { colKey: 'op', title: '操作', width: 120 },
  ];

  const query = reactive({
    keyword: '',
    enabled: undefined as boolean | undefined,
    roleName: '',
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<UserRow[]>([]);
  const loading = ref(false);

  const roleOptions = ref<Array<{ label: string; value: number }>>([]);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    username: '',
    password: '',
    nickname: '',
    roleIds: [] as number[],
    enabled: true,
  });

  const rules: FormRules = {
    username: [
      { required: true, message: '用户名不能为空', type: 'error' },
      { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: '字母、数字或下划线，3~50 位', type: 'error' },
    ],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<UserRow>();

  async function load() {
    loading.value = true;
    try {
      const page = await api.userService.findUsersBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'username asc',
        specification: {
          keyword: query.keyword || undefined,
          enabled: query.enabled,
          roleName: query.roleName || undefined,
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

  async function loadRoles() {
    const page = await api.roleService.findRolesBySuperQBE({
      pageIndex: 0,
      pageSize: 100,
      sortCode: 'code asc',
      specification: {},
    });
    roleOptions.value = page.content.map((role) => ({ label: role.name, value: role.id }));
  }

  function search() {
    pagination.current = 1;
    load();
  }

  function reset() {
    query.keyword = '';
    query.enabled = undefined;
    query.roleName = '';
    search();
  }

  function onPageChange(pageInfo: PageInfo) {
    pagination.current = pageInfo.current;
    pagination.pageSize = pageInfo.pageSize;
    load();
  }

  function openForm(row?: UserRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.username = row?.username ?? '';
    form.password = '';
    form.nickname = row?.nickname ?? '';
    form.roleIds = row ? row.roles.map((role) => role.id) : [];
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
      const input: UserInput = {
        id: form.id,
        username: form.username,
        password: form.password ? form.password : undefined,
        nickname: form.nickname ? form.nickname : undefined,
        enabled: form.enabled,
        roleIds: form.roleIds,
      };
      await api.userService.saveUser({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: UserRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.userService.deleteUser({ id: deleteTarget.value.id });
      MessagePlugin.success('删除成功');
      deleteVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  onMounted(() => {
    load();
    loadRoles();
  });
</script>

<style scoped lang="less">
  .left-operation-container {
    display: flex;
    align-items: center;

    :deep(.t-button + .t-button) {
      margin-left: 16px;
    }
  }

  .search-item {
    width: 160px;

    &.select {
      width: 100px;
    }
  }
</style>
