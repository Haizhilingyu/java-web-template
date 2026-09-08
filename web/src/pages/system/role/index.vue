<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button @click="openForm()"> 新增角色 </t-button>
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
            <t-link theme="primary" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑角色' : '新增角色'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="520px"
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
        <t-form-item label="菜单" name="menuIds">
          <t-select
            v-model="form.menuIds"
            multiple
            clearable
            :options="menuOptions"
            placeholder="选择可访问的菜单"
            :max-collapsed-span="1"
          />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除角色「${deleteTarget?.name}」？`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, PageInfo, TableProps } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { MenuDto, RoleDto } from '@/api/__generated/model/dto';
  import type { RoleInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type RoleRow = RoleDto['RoleService/DEFAULT_FETCHER'];
  type MenuNode = MenuDto['MenuService/TREE_FETCHER'];

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'code', title: '编码' },
    { colKey: 'name', title: '名称' },
    { colKey: 'description', title: '描述' },
    { colKey: 'menus', title: '可访问菜单' },
    { colKey: 'op', title: '操作', width: 120 },
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

  const menuOptions = ref<Array<{ label: string; value: number }>>([]);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    code: '',
    name: '',
    description: '',
    menuIds: [] as number[],
  });

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

  async function loadMenus() {
    const menus = await api.menuService.findMenus();
    const options: Array<{ label: string; value: number }> = [];
    const walk = (nodes: ReadonlyArray<MenuNode>) => {
      nodes.forEach((node) => {
        options.push({ label: node.name, value: node.id });
        if (node.children?.length) {
          walk(node.children);
        }
      });
    };
    walk(menus);
    menuOptions.value = options;
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
        menuIds: form.menuIds,
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

  onMounted(() => {
    load();
    loadMenus();
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
</style>
