<template>
  <div class="home-page">
    <t-card :bordered="false" class="welcome-card">
      <span class="welcome-text">你好，{{ userStore.userInfo.name || '管理员' }}，欢迎回来！</span>
    </t-card>

    <t-row :gutter="[16, 16]" class="stat-row">
      <t-col :span="3">
        <t-card :bordered="false">
          <div class="stat-body">
            <div class="stat-icon user-icon">
              <t-icon name="user" size="28px" />
            </div>
            <div>
              <div class="stat-value">{{ summary?.userCount ?? '-' }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :span="3">
        <t-card :bordered="false">
          <div class="stat-body">
            <div class="stat-icon role-icon">
              <t-icon name="usergroup" size="28px" />
            </div>
            <div>
              <div class="stat-value">{{ summary?.roleCount ?? '-' }}</div>
              <div class="stat-label">角色总数</div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :span="3">
        <t-card :bordered="false">
          <div class="stat-body">
            <div class="stat-icon login-icon">
              <t-icon name="chart" size="28px" />
            </div>
            <div>
              <div class="stat-value">{{ summary?.todayLoginSuccess ?? '-' }}</div>
              <div class="stat-label">今日登录成功</div>
            </div>
          </div>
        </t-card>
      </t-col>
      <t-col :span="3">
        <t-card :bordered="false">
          <div class="stat-body">
            <div class="stat-icon log-icon">
              <t-icon name="history" size="28px" />
            </div>
            <div>
              <div class="stat-value">{{ summary?.operLogTotal ?? '-' }}</div>
              <div class="stat-label">操作日志总数</div>
            </div>
          </div>
        </t-card>
      </t-col>
    </t-row>

    <t-row :gutter="[16, 16]">
      <t-col :span="8">
        <t-card :bordered="false" title="通知公告">
          <t-list v-if="notices.length" :split="true">
            <t-list-item v-for="notice in notices" :key="notice.id" @click="openNotice(notice.id)">
              <t-space>
                <t-tag variant="outline" size="small">{{ noticeTypeDict.label(notice.noticeType) }}</t-tag>
                <span class="notice-title">{{ notice.noticeTitle }}</span>
              </t-space>
              <template #action>
                <span class="notice-time">{{ notice.createdTime }}</span>
              </template>
            </t-list-item>
          </t-list>
          <div v-else class="empty-tip">暂无公告</div>
        </t-card>
      </t-col>
      <t-col :span="4">
        <t-card :bordered="false" title="快捷入口">
          <t-list :split="true">
            <t-list-item v-for="entry in quickEntries" :key="entry.path" @click="router.push(entry.path)">
              <t-space>
                <t-icon :name="entry.icon" />
                <span>{{ entry.title }}</span>
              </t-space>
            </t-list-item>
          </t-list>
        </t-card>
      </t-col>
    </t-row>

    <!-- 公告详情：富文本经 DOMPurify 清洗渲染(复用工单08 查看组件) -->
    <t-dialog v-model:visible="noticeVisible" :header="noticeDetail?.noticeTitle" width="640px" :footer="false">
      <rich-viewer :content="noticeDetail?.content || ''" />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import { onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';

  import { api } from '@/api/jimmer';
  import RichViewer from '@/components/rich-text/rich-viewer.vue';
  import type { HomeController_NoticeBrief, HomeController_NoticeDetail, HomeController_HomeSummary } from '@/api/__generated/model/static';
  import { useDict } from '@/hooks/useDict';
  import { useUserStore } from '@/store';

  const userStore = useUserStore();
  const router = useRouter();
  const noticeTypeDict = useDict('sys_notice_type');

  const summary = ref<HomeController_HomeSummary>();
  const notices = ref<ReadonlyArray<HomeController_NoticeBrief>>([]);
  const noticeVisible = ref(false);
  const noticeDetail = ref<HomeController_NoticeDetail>();

  const quickEntries = [
    { path: '/system/user', title: '用户管理', icon: 'user' },
    { path: '/system/role', title: '角色管理', icon: 'usergroup' },
    { path: '/monitor/logininfor', title: '登录日志', icon: 'time' },
  ];

  async function openNotice(id: number) {
    try {
      noticeDetail.value = await api.homeController.notice({ id });
      noticeVisible.value = true;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  }

  onMounted(async () => {
    try {
      const [summaryResult, noticeResult] = await Promise.all([
        api.homeController.summary(),
        api.homeController.notices(),
      ]);
      summary.value = summaryResult;
      notices.value = noticeResult;
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    }
  });
</script>

<style scoped lang="less">
  .welcome-card {
    margin-bottom: 16px;

    .welcome-text {
      font-size: 20px;
      font-weight: 600;
    }
  }

  .stat-row {
    margin-bottom: 16px;
  }

  .stat-body {
    display: flex;
    align-items: center;
    gap: 16px;

    .stat-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 56px;
      height: 56px;
      border-radius: 8px;
      color: #fff;
    }

    .user-icon {
      background: var(--td-brand-color);
    }

    .role-icon {
      background: var(--td-success-color);
    }

    .login-icon {
      background: var(--td-warning-color);
    }

    .log-icon {
      background: var(--td-error-color);
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      line-height: 1.2;
    }

    .stat-label {
      color: var(--td-text-color-secondary);
      font-size: 13px;
    }
  }

  .notice-title {
    cursor: pointer;
  }

  .notice-time {
    color: var(--td-text-color-secondary);
    font-size: 12px;
  }

  .empty-tip {
    color: var(--td-text-color-secondary);
    text-align: center;
    padding: 24px 0;
  }
</style>
