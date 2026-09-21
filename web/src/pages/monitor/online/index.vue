<template>
  <div>
    <t-card class="list-card-container" :bordered="false">
      <t-row justify="space-between">
        <div class="left-operation-container">
          <span class="title">在线用户（实时）</span>
        </div>
        <t-button variant="outline" @click="load"> 刷新 </t-button>
      </t-row>

      <t-table row-key="jti" :data="data" :columns="columns" :loading="loading">
        <template #loginTime="{ row }">
          {{ formatTime(row.loginTime) }}
        </template>
        <template #op="{ row }">
          <t-link theme="danger" v-permission="'system:online:forceLogout'" @click="confirmForceLogout(row)">
            强退
          </t-link>
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="forceVisible"
      header="强退确认"
      :body="`确认强退用户「${forceTarget?.username}」（${forceTarget?.ip}）？其令牌将立即失效。`"
      @confirm="doForceLogout"
    />
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { TableProps } from 'tdesign-vue-next';
  import { onMounted, ref } from 'vue';

  import type { OnlineService_OnlineUser as OnlineUser } from '@/api/__generated/model/static';
  import { api } from '@/api/jimmer';

  const columns: TableProps['columns'] = [
    { colKey: 'username', title: '账号' },
    { colKey: 'nickname', title: '昵称' },
    { colKey: 'ip', title: '登录 IP' },
    { colKey: 'loginTime', title: '登录时间', cell: 'loginTime' },
    { colKey: 'op', title: '操作', width: 100 },
  ];

  const data = ref<OnlineUser[]>([]);
  const loading = ref(false);

  const forceVisible = ref(false);
  const forceTarget = ref<OnlineUser>();

  async function load() {
    loading.value = true;
    try {
      data.value = [...(await api.onlineService.list())];
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      loading.value = false;
    }
  }

  function formatTime(iso: string): string {
    return iso ? iso.slice(0, 19).replace('T', ' ') : '-';
  }

  function confirmForceLogout(row: OnlineUser) {
    forceTarget.value = row;
    forceVisible.value = true;
  }

  async function doForceLogout() {
    if (!forceTarget.value) {
      return;
    }
    try {
      await api.onlineService.forceLogout({ jti: forceTarget.value.jti });
      MessagePlugin.success('已强退');
      forceVisible.value = false;
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

  .title {
    font-weight: 600;
  }
</style>
