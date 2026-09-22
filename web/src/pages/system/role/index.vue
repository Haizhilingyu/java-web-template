<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:role:add'" @click="openForm()"> 新增角色 </t-button>
        </div>
        <t-space break-line>
          <t-input v-model="query.keyword" placeholder="编码/名称" clearable class="search-item" />
          <t-input v-model="query.menuName" placeholder="菜单名" clearable class="search-item" />
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
        <template #menus="{ row }">
          <t-space size="small">
            <t-tag v-for="menu in row.menus" :key="menu.id" variant="outline">{{ menu.name }}</t-tag>
          </t-space>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:role:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="primary" v-permission="'system:role:edit'" @click="openAssign(row)"> 分配用户 </t-link>
            <t-link theme="danger" v-permission="'system:role:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑角色' : '新增角色'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="560px"
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
        <t-form-item label="描述" name="description">
          <t-textarea v-model="form.description" />
        </t-form-item>
        <t-form-item label="数据范围" name="dataScope">
          <t-radio-group v-model="form.dataScope">
            <t-radio-button v-for="opt in DATA_SCOPES" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </t-radio-button>
          </t-radio-group>
        </t-form-item>
        <t-form-item v-if="form.dataScope === 2" label="可见部门" name="customDeptIds">
          <!-- 精确等于勾选集合：checkStrictly 关闭父子联动，不自动补子孙 -->
          <div class="menu-tree-box">
            <t-tree
              v-model="form.customDeptIds"
              :data="deptTreeOptions"
              checkable
              hover
              check-strictly
              :expand-level="2"
            />
          </div>
        </t-form-item>
        <t-form-item label="菜单授权" name="menuIds">
          <div class="menu-tree-box">
            <!-- TDesign Tree 的受控勾选是 v-model(modelValue)，写成 v-model:checked 不会同步值 -->
            <t-tree
              v-model="form.menuIds"
              :data="menuTreeOptions"
              checkable
              hover
              value-mode="all"
              :expand-level="2"
            />
          </div>
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除角色「${deleteTarget?.name}」？`"
      @confirm="doDelete"
    />

    <!-- 分配用户抽屉(工单06)：上半已绑用户分页列表，支持批量授权/批量取消授权 -->
    <t-drawer v-model:visible="assignVisible" :header="`分配用户：${assignTarget?.name ?? ''}`" size="720px" :footer="false">
      <t-space direction="vertical" style="width: 100%">
        <t-row justify="space-between">
          <t-space>
            <t-input v-model="boundQuery.keyword" placeholder="用户名/昵称" clearable style="width: 180px" @enter="searchBound" />
            <t-button theme="primary" variant="base" @click="searchBound"> 查询 </t-button>
          </t-space>
          <t-space>
            <t-button theme="primary" @click="openPickUsers"> 批量授权 </t-button>
            <t-button theme="danger" variant="outline" :disabled="!boundSelection.length" @click="doUnassign">
              批量取消授权
            </t-button>
          </t-space>
        </t-row>
        <t-table
          row-key="id"
          :data="boundUsers"
          :columns="boundColumns"
          :loading="boundLoading"
          :pagination="boundPagination"
          :selected-row-keys="boundSelection"
          @page-change="onBoundPageChange"
          @select-change="(keys: Array<string | number>) => (boundSelection = keys)"
        >
          <template #dept="{ row }">{{ row.dept?.name ?? '-' }}</template>
        </t-table>
      </t-space>
    </t-drawer>

    <!-- 挑选用户弹窗：勾选后加入该角色 -->
    <t-dialog v-model:visible="pickVisible" header="选择要授权的用户" width="480px" @confirm="doAssign">
      <t-select
        v-model="pickSelection"
        multiple
        clearable
        filterable
        :loading="pickLoading"
        placeholder="输入用户名/昵称搜索"
        style="width: 100%"
      >
        <t-option v-for="user in pickableUsers" :key="user.id" :label="`${user.username}（${user.nickname ?? '-'}）`" :value="user.id" />
      </t-select>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { DeptDto, MenuDto, RoleDto, UserDto } from '@/api/__generated/model/dto';
  import type { RoleInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type RoleRow = RoleDto['RoleService/DEFAULT_FETCHER'];
  type MenuNode = MenuDto['MenuService/TREE_FETCHER'];
  type DeptNode = DeptDto['DeptService/TREE_FETCHER'];
  interface DeptOption {
    label: string;
    value: number;
    children?: DeptOption[];
  }

  const DATA_SCOPES = [
    { value: 1, label: '全部' },
    { value: 2, label: '自定义' },
    { value: 3, label: '本部门' },
    { value: 4, label: '本部门及以下' },
    { value: 5, label: '仅本人' },
  ] as const;

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'code', title: '编码' },
    { colKey: 'name', title: '名称' },
    { colKey: 'description', title: '描述' },
    { colKey: 'menus', title: '可访问菜单' },
    { colKey: 'op', title: '操作', width: 170 },
  ];

  const query = reactive({
    keyword: '',
    menuName: '',
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });

  const data = ref<RoleRow[]>([]);
  const loading = ref(false);

  const menuTreeOptions = ref<MenuOption[]>([]);
  // 授权树原始数据：保存时用于补全勾选节点的父级，防止子菜单脱离目录形成断链
  const menuTreeData = ref<MenuNode[]>([]);

  interface MenuOption {
    label: string;
    value: number;
    children?: MenuOption[];
  }

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    code: '',
    name: '',
    description: '',
    dataScope: 1,
    customDeptIds: [] as number[],
    menuIds: [] as number[],
  });

  const deptTreeOptions = ref<DeptOption[]>([]);

  const rules: FormRules = {
    code: [
      { required: true, message: '编码不能为空', type: 'error' },
      { pattern: /^[A-Z][A-Z0-9_]*$/, message: '大写字母开头，可含大写字母/数字/下划线', type: 'error' },
    ],
    name: [{ required: true, message: '名称不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<RoleRow>();

  async function load() {
    loading.value = true;
    try {
      const page = await api.roleService.findRolesBySuperQBE({
        pageIndex: pagination.current - 1,
        pageSize: pagination.pageSize,
        sortCode: 'code asc',
        specification: {
          keyword: query.keyword || undefined,
          menuName: query.menuName || undefined,
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

  async function loadDepts() {
    const depts = await api.deptService.findDepts();
    deptTreeOptions.value = toDeptOptions(depts);
  }

  function toDeptOptions(nodes: ReadonlyArray<DeptNode>): DeptOption[] {
    return nodes.map((node) => {
      const option: DeptOption = { label: node.name, value: node.id };
      if (node.children?.length) {
        option.children = toDeptOptions(node.children);
      }
      return option;
    });
  }

  async function loadMenus() {
    const menus = await api.menuService.findMenus();
    menuTreeData.value = [...menus];
    menuTreeOptions.value = toTreeOptions(menus);
  }

  function toTreeOptions(nodes: ReadonlyArray<MenuNode>): MenuOption[] {
    return nodes.map((node) => {
      const option: MenuOption = {
        // 按钮节点标注类型，便于区分页面与按钮级权限
        label: node.type === 'F' ? `${node.name}（按钮）` : node.name,
        value: node.id,
      };
      if (node.children?.length) {
        option.children = toTreeOptions(node.children);
      }
      return option;
    });
  }

  /** 勾选集合补全其全部祖先节点 */
  function withAncestors(ids: number[]): number[] {
    const set = new Set(ids);
    const walk = (nodes: ReadonlyArray<MenuNode>, ancestors: number[]) => {
      nodes.forEach((node) => {
        if (set.has(node.id)) {
          ancestors.forEach((id) => set.add(id));
        }
        if (node.children?.length) {
          walk(node.children, [...ancestors, node.id]);
        }
      });
    };
    walk(menuTreeData.value, []);
    return [...set];
  }

  function search() {
    pagination.current = 1;
    load();
  }

  function reset() {
    query.keyword = '';
    query.menuName = '';
    search();
  }

  function onPageChange(pageInfo: PageInfo) {
    pagination.current = pageInfo.current;
    pagination.pageSize = pageInfo.pageSize;
    load();
  }

  function openForm(row?: RoleRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.code = row?.code ?? '';
    form.name = row?.name ?? '';
    form.description = row?.description ?? '';
    form.dataScope = row?.dataScope ?? 1;
    form.customDeptIds = row ? row.customDepts.map((dept) => dept.id) : [];
    form.menuIds = row ? row.menus.map((menu) => menu.id) : [];
    formVisible.value = true;
  }

  async function save() {
    const valid = await formInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: RoleInput = {
        id: form.id,
        code: form.code,
        name: form.name,
        description: form.description ? form.description : undefined,
        dataScope: form.dataScope,
        customDeptIds: form.customDeptIds,
        menuIds: withAncestors(form.menuIds),
      };
      await api.roleService.saveRole({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: RoleRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.roleService.deleteRole({ id: deleteTarget.value.id });
      MessagePlugin.success('删除成功');
      deleteVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  // ------- 分配用户(工单06) -------
  const assignVisible = ref(false);
  const assignTarget = ref<RoleRow>();
  const boundUsers = ref<Array<UserDto['RoleService/ROLE_USER_FETCHER']>>([]);
  const boundSelection = ref<Array<string | number>>([]);
  const boundLoading = ref(false);
  const boundQuery = reactive({ keyword: '' });
  const boundPagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showJumper: true,
  });
  const boundColumns: TableProps['columns'] = [
    { colKey: 'row-select', type: 'multiple', width: 46 },
    { colKey: 'username', title: '用户名', width: 120 },
    { colKey: 'nickname', title: '昵称', width: 110 },
    { colKey: 'dept', title: '部门', cell: 'dept' },
    { colKey: 'enabled', title: '状态', width: 80 },
  ];

  function openAssign(row: RoleRow) {
    assignTarget.value = row;
    boundQuery.keyword = '';
    boundSelection.value = [];
    assignVisible.value = true;
    loadBoundUsers();
  }

  async function loadBoundUsers() {
    if (!assignTarget.value) {
      return;
    }
    boundLoading.value = true;
    try {
      const page = await api.roleService.findRoleUsers({
        id: assignTarget.value.id,
        pageIndex: boundPagination.current - 1,
        pageSize: boundPagination.pageSize,
        keyword: boundQuery.keyword || undefined,
      });
      boundUsers.value = [...page.content];
      boundPagination.total = page.totalElements;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      boundLoading.value = false;
    }
  }

  function searchBound() {
    boundPagination.current = 1;
    loadBoundUsers();
  }

  function onBoundPageChange(pageInfo: PageInfo) {
    boundPagination.current = pageInfo.current;
    boundPagination.pageSize = pageInfo.pageSize;
    boundSelection.value = [];
    loadBoundUsers();
  }

  // 批量授权：弹窗里多选用户
  const pickVisible = ref(false);
  const pickLoading = ref(false);
  const pickSelection = ref<Array<number>>([]);
  const pickableUsers = ref<Array<{ id: number; username: string; nickname?: string }>>([]);

  async function openPickUsers() {
    if (!assignTarget.value) {
      return;
    }
    pickSelection.value = [];
    pickVisible.value = true;
    pickLoading.value = true;
    try {
      // 候选为全部启用用户(后端授权幂等，重复勾选已绑用户无副作用)
      const page = await api.userService.findUsersBySuperQBE({
        pageIndex: 0,
        pageSize: 500,
        sortCode: 'username asc',
        specification: {},
      });
      pickableUsers.value = page.content
        .filter((user) => user.enabled)
        .map((user) => ({ id: user.id, username: user.username, nickname: user.nickname ?? undefined }));
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      pickLoading.value = false;
    }
  }

  async function doAssign() {
    if (!assignTarget.value || !pickSelection.value.length) {
      pickVisible.value = false;
      return;
    }
    try {
      await api.roleService.assignUsers({
        id: assignTarget.value.id,
        body: { userIds: pickSelection.value },
      });
      MessagePlugin.success('授权成功，用户权限即时生效');
      pickVisible.value = false;
      searchBound();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  async function doUnassign() {
    if (!assignTarget.value || !boundSelection.value.length) {
      return;
    }
    try {
      await api.roleService.unassignUsers({
        id: assignTarget.value.id,
        body: { userIds: boundSelection.value.map((key) => Number(key)) },
      });
      MessagePlugin.success('已取消授权');
      boundSelection.value = [];
      loadBoundUsers();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  onMounted(() => {
    load();
    loadMenus();
    loadDepts();
  });
</script>

<style scoped lang="less">
  .left-operation-container {
    display: flex;
    align-items: center;
  }

  .search-item {
    width: 160px;
  }

  .menu-tree-box {
    width: 100%;
    max-height: 320px;
    overflow: auto;
    border: 1px solid var(--td-component-border);
    border-radius: var(--td-radius-medium);
    padding: 8px;
  }
</style>
