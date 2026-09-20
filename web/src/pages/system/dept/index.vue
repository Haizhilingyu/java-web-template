<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <t-button v-permission="'system:dept:add'" @click="openForm()"> 新增部门 </t-button>
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
        <template #enabled="{ row }">
          <t-tag :theme="row.enabled ? 'success' : 'default'" variant="light">
            {{ row.enabled ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-link theme="primary" v-permission="'system:dept:add'" @click="openForm(undefined, row)"> 新增子部门 </t-link>
            <t-link theme="primary" v-permission="'system:dept:edit'" @click="openForm(row)"> 编辑 </t-link>
            <t-link theme="danger" v-permission="'system:dept:delete'" @click="confirmDelete(row)"> 删除 </t-link>
          </t-space>
        </template>
      </t-enhanced-table>
    </t-card>

    <t-dialog
      v-model:visible="formVisible"
      :header="form.id ? '编辑部门' : '新增部门'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="520px"
      @confirm="save"
      @closed="formInstance?.reset()"
    >
      <t-form ref="formInstance" :data="form" :rules="rules" label-width="80px" @submit.prevent>
        <t-form-item label="上级部门" name="parentId">
          <t-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            clearable
            filterable
            placeholder="不选则为根部门"
          />
        </t-form-item>
        <t-form-item label="部门名" name="name">
          <t-input v-model="form.name" placeholder="同一父节点下不允许重名" />
        </t-form-item>
        <t-form-item label="负责人" name="leader">
          <t-input v-model="form.leader" placeholder="自由文本，可留空" />
        </t-form-item>
        <t-form-item label="联系电话" name="phone">
          <t-input v-model="form.phone" />
        </t-form-item>
        <t-form-item label="邮箱" name="email">
          <t-input v-model="form.email" />
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
      :body="`确认删除部门「${deleteTarget?.name}」？存在子部门或部门下有用户时将被拒绝。`"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules, TableProps, TableRowData } from 'tdesign-vue-next';
  import { computed, onMounted, reactive, ref } from 'vue';

  import type { DeptDto } from '@/api/__generated/model/dto';
  import type { DeptInput } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  type DeptNode = DeptDto['DeptService/TREE_FETCHER'];
  type DeptRow = DeptDto['DeptService/DEFAULT_FETCHER'];
  interface DeptOption {
    label: string;
    value: number;
    children?: DeptOption[];
  }

  const columns: TableProps['columns'] = [
    { colKey: 'id', title: 'ID', width: 80 },
    { colKey: 'name', title: '部门名' },
    { colKey: 'leader', title: '负责人', width: 110 },
    { colKey: 'phone', title: '联系电话', width: 140 },
    { colKey: 'email', title: '邮箱' },
    { colKey: 'sortOrder', title: '排序号', width: 90 },
    { colKey: 'enabled', title: '状态', width: 90, cell: 'enabled' },
    { colKey: 'op', title: '操作', width: 200 },
  ];

  const data = ref<TableRowData[]>([]);
  const deptTree = ref<DeptNode[]>([]);
  const loading = ref(false);

  const formVisible = ref(false);
  const saving = ref(false);
  const formInstance = ref<FormInstanceFunctions>();

  const form = reactive({
    id: undefined as number | undefined,
    name: '',
    parentId: undefined as number | undefined,
    leader: '',
    phone: '',
    email: '',
    sortOrder: 0,
    enabled: true,
  });

  const rules: FormRules = {
    name: [{ required: true, message: '部门名不能为空', type: 'error' }],
  };

  const deleteVisible = ref(false);
  const deleteTarget = ref<DeptRow>();

  async function load() {
    loading.value = true;
    try {
      const depts = await api.deptService.findDepts();
      data.value = depts as unknown as TableRowData[];
      deptTree.value = [...depts];
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 父部门候选：编辑时把自身及子孙剔除，防止把自己挂到自己的子孙下成环
   */
  const parentOptions = computed(() => {
    const exclude = (options: DeptOption[], editingId: number | undefined): DeptOption[] =>
      options
        .filter((option) => option.value !== editingId)
        .map((option) => ({
          ...option,
          children: option.children ? exclude(option.children, editingId) : undefined,
        }));
    return exclude(toTreeOptions(deptTree.value), form.id);
  });

  function toTreeOptions(nodes: ReadonlyArray<DeptNode>): DeptOption[] {
    return nodes.map((node) => {
      const option: DeptOption = {
        label: node.name,
        value: node.id,
      };
      if (node.children?.length) {
        option.children = toTreeOptions(node.children);
      }
      return option;
    });
  }

  /** 从树中查找某节点的父节点 id */
  function findParentId(nodes: ReadonlyArray<DeptNode>, targetId: number): number | undefined {
    for (const node of nodes) {
      if (node.children?.some((child) => child.id === targetId)) {
        return node.id;
      }
      const hit = node.children ? findParentId(node.children, targetId) : undefined;
      if (hit !== undefined) {
        return hit;
      }
    }
    return undefined;
  }

  function openForm(row?: DeptRow, parent?: DeptNode) {
    formInstance.value?.reset();
    form.id = row?.id;
    form.name = row?.name ?? '';
    form.parentId = row ? findParentId(deptTree.value, row.id) : parent?.id;
    form.leader = row?.leader ?? '';
    form.phone = row?.phone ?? '';
    form.email = row?.email ?? '';
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
      const input: DeptInput = {
        id: form.id,
        name: form.name,
        parentId: form.parentId,
        leader: form.leader ? form.leader : undefined,
        phone: form.phone ? form.phone : undefined,
        email: form.email ? form.email : undefined,
        sortOrder: form.sortOrder,
        enabled: form.enabled,
      };
      await api.deptService.saveDept({ body: input });
      MessagePlugin.success('保存成功');
      formVisible.value = false;
      load();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  function confirmDelete(row: DeptRow) {
    deleteTarget.value = row;
    deleteVisible.value = true;
  }

  async function doDelete() {
    if (!deleteTarget.value) {
      return;
    }
    try {
      await api.deptService.deleteDept({ id: deleteTarget.value.id });
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
