<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:menu:add'" @click="openForm()"> 新增菜单 </t-button>
        </div>
        <t-button variant="outline" @click="load"> 刷新 </t-button>
      </t-row>

      <!-- 树形表格必须用 t-enhanced-table，普通 t-table 不支持 tree 配置 -->
      <t-enhanced-table
        row-key="id"
        :data="data"
        :columns="columns"
        :loading="loading"
        :tree="{ childrenKey: 'children', treeNodeColumnIndex: 1, defaultExpandAll: true, indent: 24 }"
      >
        <template #type="{ row }">
          <t-tag :theme="typeTheme(row.type)" variant="light-outline">{{ typeLabel(row.type) }}</t-tag>
        </template>
        <template #visible="{ row }">
          <t-tag v-if="row.type !== 'F'" :theme="row.visible ? 'success' : 'default'" variant="light">
            {{ row.visible ? '显示' : '隐藏' }}
          </t-tag>
          <span v-else>-</span>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:menu:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:menu:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-enhanced-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑菜单' : '新增菜单'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="520px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="菜单类型" name="type">
          <t-radio-group v-model="form.type" :disabled="!!form.id">
            <t-radio-button value="M">目录</t-radio-button>
            <t-radio-button value="C">菜单</t-radio-button>
            <t-radio-button value="F">按钮</t-radio-button>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="菜单名" name="name">
          <t-input v-model="form.name" placeholder="同一父节点下不允许重名" />
        </t-form-item>
        <t-form-item label="父菜单" name="parentId">
          <t-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            clearable
            filterable
            placeholder="不选则为根菜单"
          />
        </t-form-item>
        <t-form-item v-if="form.type !== 'F'" label="路由地址" name="path">
          <t-input v-model="form.path" placeholder="如 /system/user；目录为一级路由(如 /system)" />
        </t-form-item>
        <t-form-item v-if="form.type === 'C'" label="组件路径" name="component">
          <t-input v-model="form.component" placeholder="pages 下组件路径，如 /system/user/index" />
        </t-form-item>
        <t-form-item label="权限标识" name="perms">
          <t-input v-model="form.perms" placeholder="如 system:user:add，按钮必填" />
        </t-form-item>
        <t-form-item v-if="form.type !== 'F'" label="图标" name="icon">
          <t-input v-model="form.icon" placeholder="tdesign 图标名，可留空" />
        </t-form-item>
        <t-form-item v-if="form.type !== 'F'" label="是否显示" name="visible">
          <t-switch v-model="form.visible" />
        </t-form-item>
        <t-form-item label="排序号" name="sortOrder">
          <t-input-number v-model="form.sortOrder" :min="0" theme="column" style="width: 100%" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="deleteVisible"
      header="删除确认"
      :body="`确认删除菜单「${deleteTarget?.name}」？其子菜单将被解除关联而非删除。`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, TableProps, TableRowData } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';

  import type { MenuDto } from '@/api/__generated/model/dto';
  import type { MenuInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type MenuNode = MenuDto['MenuService/TREE_FETCHER'];
  type MenuRow = MenuDto['MenuService/DEFAULT_FETCHER'];
  interface MenuOption {
    label: string;
    value: number;
    children?: MenuOption[];
  }

  const TYPE_LABELS: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' };
  const typeLabel = (type: string) => TYPE_LABELS[type] ?? type;
  const typeTheme = (type: string) => ({ M: 'warning', C: 'primary', F: 'default' } as const)[type as 'M' | 'C' | 'F'] ?? 'default';

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'name', title: '菜单名' },
    { colKey: 'type', title: '类型', width: 90, cell: 'type' },
    { colKey: 'path', title: '路由地址' },
    { colKey: 'perms', title: '权限标识' },
    { colKey: 'icon', title: '图标', width: 100 },
    { colKey: 'sortOrder', title: '排序号', width: 90 },
    { colKey: 'visible', title: '可见', width: 90, cell: 'visible' },
    { colKey: 'op', title: '操作', width: 120 },
  ];

  const data = ref<TableRowData[]>([]);
  const menuTree = ref<MenuNode[]>([]);
  const loading = ref(false);

  const parentOptions = ref<MenuOption[]>([]);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    type: 'M',
    name: '',
    parentId: undefined as number | undefined,
    path: '',
    component: '',
    perms: '',
    icon: '',
    visible: true,
    sortOrder: 0,
  });

  const rules: FormRules = {
    name: [{ required: true, message: '菜单名不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<MenuRow>();

  async function load() {
    loading.value = true;
    try {
      const menus = await api.menuService.findMenus();
      data.value = menus as unknown as TableRowData[];
      menuTree.value = [...menus];
      parentOptions.value = toTreeOptions(menus);
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      loading.value = false;
    }
  }

  function toTreeOptions(nodes: ReadonlyArray<MenuNode>): MenuOption[] {
    return nodes.map((node) => {
      const option: MenuOption = {
        label: node.name,
        value: node.id,
      };
      if (node.children?.length) {
        option.children = toTreeOptions(node.children);
      }
      return option;
    });
  }

  function openForm(row?: MenuRow) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.type = row?.type ?? 'M';
    form.name = row?.name ?? '';
    form.parentId = undefined;
    form.path = row?.path ?? '';
    form.component = row?.component ?? '';
    form.perms = row?.perms ?? '';
    form.icon = row?.icon ?? '';
    form.visible = row?.visible ?? true;
    form.sortOrder = row?.sortOrder ?? 0;
    // 编辑时回填父节点：从树中查找
    if (row) {
      const findParent = (nodes: ReadonlyArray<MenuNode>): number | undefined => {
        for (const node of nodes) {
          if (node.children?.some((child) => child.id === row.id)) {
            return node.id;
          }
          const hit = node.children ? findParent(node.children) : undefined;
          if (hit !== undefined) {
            return hit;
          }
        }
        return undefined;
      };
      form.parentId = findParent(menuTree.value);
    }
    formVisible.value = true;
  }

  async function save() {
    const valid = await formInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      const input: MenuInput = {
        id: form.id,
        type: form.type,
        name: form.name,
        parentId: form.parentId,
        path: form.path ? form.path : undefined,
        component: form.component ? form.component : undefined,
        perms: form.perms ? form.perms : undefined,
        icon: form.icon ? form.icon : undefined,
        visible: form.visible,
        sortOrder: form.sortOrder,
      };
      await api.menuService.saveMenu({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: MenuRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.menuService.deleteMenu({ id: deleteTarget.value.id });
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
</style>
