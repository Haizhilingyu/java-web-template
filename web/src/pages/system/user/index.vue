<template>
  <div>
    <t-row :gutter="[16, 16]">
      <t-col :span="2">
        <t-card :bordered="false" class="dept-panel">
          <template #header> 部门 </template>
          <!-- 点选部门按"该部门及其全部子孙"筛选；再次点选取消 -->
          <t-tree
            :data="deptTreeOptions"
            hover
            activable
            :expand-level="2"
            @active="onDeptActive"
          />
        </t-card>
      </t-col>
      <t-col :span="10">
        <t-card class="list-card-container" :bordered="false">
          <t-row justify="space-between">
            <div class="left-operation-container">
              <t-button v-permission="'system:user:add'" @click="openForm()"> 新增用户 </t-button>
              <t-button v-permission="'system:user:export'" variant="outline" :loading="exporting" @click="onExport">
                导出
              </t-button>
              <t-button v-permission="'system:user:import'" variant="outline" @click="importVisible = true">
                导入
              </t-button>
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
            <template #dept="{ row }">
              <span>{{ row.dept?.name ?? '-' }}</span>
            </template>
            <template #roles="{ row }">
              <t-space size="small">
                <t-tag v-for="role in row.roles" :key="role.id" variant="outline">{{ role.name }}</t-tag>
              </t-space>
            </template>
            <template #posts="{ row }">
              <t-space size="small">
                <t-tag v-for="post in row.posts" :key="post.id" theme="warning" variant="outline">
                  {{ post.name }}
                </t-tag>
              </t-space>
            </template>
            <template #op="{ row }">
              <t-space>
                <t-link theme="primary" v-permission="'system:user:edit'" @click="openForm(row)"> 编辑 </t-link>
                <t-link theme="warning" v-permission="'system:user:resetPwd'" @click="openReset(row)"> 重置密码 </t-link>
                <t-link theme="danger" v-permission="'system:user:delete'" @click="confirmDelete(row)"> 删除 </t-link>
              </t-space>
            </template>
          </t-table>
        </t-card>
      </t-col>
    </t-row>

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
        <t-form-item label="部门" name="deptId">
          <!-- 选择器过滤禁用部门；清空选择不表示取消部门(后端未提交即不改) -->
          <t-tree-select
            v-model="form.deptId"
            :data="enabledDeptOptions"
            clearable
            filterable
            placeholder="选择部门"
          />
        </t-form-item>
        <t-form-item label="岗位" name="postIds">
          <t-select v-model="form.postIds" multiple clearable :options="enabledPostOptions" placeholder="选择岗位" />
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

    <!-- 重置密码(工单05)：管理员输入新密码，重置后目标用户全部会话作废 -->
    <t-dialog
      v-model:visible="resetVisible"
      :header="`重置密码：${resetTarget?.username ?? ''}`"
      :confirm-btn="{ content: '确认重置', loading: resetting }"
      width="420px"
      @confirm="doReset"
      @closed="resetFormInstance?.reset()"
    >
      <t-alert theme="warning" style="margin-bottom: 12px">
        <template #message>重置后该用户所有登录会话将立即失效，需用新密码重新登录</template>
      </t-alert>
      <t-form ref="resetFormInstance" :data="resetForm" :rules="resetRules" label-width="80px" @submit.prevent>
        <t-form-item label="新密码" name="newPassword">
          <t-input v-model="resetForm.newPassword" type="password" clearable placeholder="6~100 位" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <!-- 导入弹窗：下载模板 + 选择文件 + 结果回显(工单03) -->
    <t-dialog
      v-model:visible="importVisible"
      header="导入用户"
      :footer="false"
      width="560px"
      @closed="resetImport"
    >
      <t-space direction="vertical" style="width: 100%">
        <t-space>
          <t-button variant="outline" @click="downloadTemplate"> 下载模板 </t-button>
          <t-button theme="primary" :loading="importing" :disabled="!importFile" @click="doImport">
            开始导入
          </t-button>
        </t-space>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" @change="onFileChange" />
        <t-alert v-if="importResult" :theme="importResult.failures.length ? 'warning' : 'success'">
          <template #message>
            共 {{ importResult.total }} 行，成功 {{ importResult.successCount }} 行，失败
            {{ importResult.failures.length }} 行
          </template>
        </t-alert>
        <t-table
          v-if="importResult && importResult.failures.length"
          row-key="rowNum"
          :data="importResult.failures"
          :columns="failureColumns"
          max-height="200"
          size="small"
        />
      </t-space>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps, TreeNodeValue } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { DeptDto, UserDto } from '@/api/__generated/model/dto';
  import type { UserInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';
  import { downloadFile, type ImportResult, uploadForJson } from '@/api/download';

  type UserRow = UserDto['UserService/DEFAULT_FETCHER'];
  type DeptNode = DeptDto['DeptService/TREE_FETCHER'];
  interface DeptOption {
    label: string;
    value: number;
    children?: DeptOption[];
  }

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 70 },
    { colKey: 'username', title: '用户名' },
    { colKey: 'nickname', title: '昵称' },
    { colKey: 'dept', title: '部门', cell: 'dept' },
    { colKey: 'enabled', title: '状态', width: 90 },
    { colKey: 'roles', title: '角色' },
    { colKey: 'posts', title: '岗位' },
    { colKey: 'createdTime', title: '创建时间', width: 170 },
    { colKey: 'op', title: '操作', width: 180 },
  ];

  const query = reactive({
    keyword: '',
    enabled: undefined as boolean | undefined,
    roleName: '',
    deptId: undefined as number | undefined,
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
  const enabledPostOptions = ref<Array<{ label: string; value: number }>>([]);

  const deptTree = ref<DeptNode[]>([]);
  const deptTreeOptions = ref<DeptOption[]>([]);
  // 表单选择器只展示启用部门
  const enabledDeptOptions = ref<DeptOption[]>([]);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    username: '',
    password: '',
    nickname: '',
    deptId: undefined as number | undefined,
    postIds: [] as number[],
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
        deptId: query.deptId,
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

  async function loadDeptsAndPosts() {
    const [depts, postPage] = await Promise.all([
      api.deptService.findDepts(),
      api.postService.findPostsBySuperQBE({
        pageIndex: 0,
        pageSize: 100,
        sortCode: 'sortOrder asc',
        specification: {},
      }),
    ]);
    deptTree.value = [...depts];
    deptTreeOptions.value = toTreeOptions(depts);
    enabledDeptOptions.value = dropDisabled(depts);
    enabledPostOptions.value = postPage.content
      .filter((post) => post.enabled)
      .map((post) => ({ label: post.name, value: post.id }));
  }

  function toTreeOptions(nodes: ReadonlyArray<DeptNode>): DeptOption[] {
    return nodes.map((node) => {
      const option: DeptOption = { label: node.name, value: node.id };
      if (node.children?.length) {
        option.children = toTreeOptions(node.children);
      }
      return option;
    });
  }

  /** 选择器过滤禁用部门：仅过滤禁用节点本身，保留其子孙 */
  function dropDisabled(nodes: ReadonlyArray<DeptNode>): DeptOption[] {
    return nodes
      .filter((node) => node.enabled)
      .map((node) => ({
        label: node.name,
        value: node.id,
        children: node.children?.length ? dropDisabled(node.children) : undefined,
      }));
  }

  function onDeptActive(value: TreeNodeValue[], context: { node?: { label?: string } }) {
    const activated = Array.isArray(value) ? value[0] : value;
    query.deptId = typeof activated === 'number' ? activated : undefined;
    pagination.current = 1;
    load();
  }

  function search() {
    pagination.current = 1;
    load();
  }

  function reset() {
    query.keyword = '';
    query.enabled = undefined;
    query.roleName = '';
    query.deptId = undefined;
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
    form.deptId = row?.dept?.id;
    form.postIds = row ? row.posts.map((post) => post.id) : [];
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
        deptId: form.deptId,
        postIds: form.postIds,
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

  // ------- 重置密码(工单05) -------
  const resetVisible = ref(false);
  const resetting = ref(false);
  const resetTarget = ref<UserRow>();
  const resetFormInstance = ref<FormInstanceFunctions>();
  const resetForm = reactive({ newPassword: '' });
  const resetRules: FormRules = {
    newPassword: [
      { required: true, message: '新密码不能为空', type: 'error' },
      { min: 6, max: 100, message: '新密码长度必须在6~100之间', type: 'error' },
    ],
  };

  function openReset(row: UserRow) {
    resetTarget.value = row;
    resetForm.newPassword = '';
    resetVisible.value = true;
  }

  async function doReset() {
    if (!resetTarget.value) {
      return;
    }
    const valid = await resetFormInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    resetting.value = true;
    try {
      await api.userService.resetPassword({
        id: resetTarget.value.id,
        body: { newPassword: resetForm.newPassword },
      });
      MessagePlugin.success('密码已重置，该用户的全部会话已作废');
      resetVisible.value = false;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      resetting.value = false;
    }
  }

  // ------- 导入导出(工单03) -------
  const exporting = ref(false);
  const importVisible = ref(false);
  const importing = ref(false);
  const importFile = ref<File>();
  const fileInput = ref<HTMLInputElement>();
  const importResult = ref<ImportResult>();
  const failureColumns = [
    { colKey: 'rowNum', title: '行号', width: 80 },
    { colKey: 'reason', title: '失败原因' },
  ];

  async function onExport() {
    exporting.value = true;
    try {
      await downloadFile('/api/v1/user/export', {
        keyword: query.keyword || undefined,
        enabled: query.enabled,
        roleName: query.roleName || undefined,
        deptId: query.deptId,
      });
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      exporting.value = false;
    }
  }

  function downloadTemplate() {
    downloadFile('/api/v1/user/import-template').catch((error) => {
      MessagePlugin.error((error as Error).message);
    });
  }

  function onFileChange(event: Event) {
    const files = (event.target as HTMLInputElement).files;
    importFile.value = files && files.length ? files[0] : undefined;
  }

  async function doImport() {
    if (!importFile.value) {
      return;
    }
    importing.value = true;
    try {
      importResult.value = await uploadForJson<ImportResult>('/api/v1/user/import', importFile.value);
      MessagePlugin.success(`导入完成：成功 ${importResult.value.successCount} 行`);
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      importing.value = false;
    }
  }

  function resetImport() {
    importFile.value = undefined;
    importResult.value = undefined;
    if (fileInput.value) {
      fileInput.value.value = '';
    }
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
    loadDeptsAndPosts();
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

  .dept-panel {
    :deep(.t-card__body) {
      padding-top: 8px;
    }
  }

  .search-item {
    width: 160px;

    &.select {
      width: 100px;
    }
  }
</style>
