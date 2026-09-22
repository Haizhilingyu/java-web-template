<template>
  <t-row :gutter="[16, 16]">
    <t-col :span="5">
      <t-card :bordered="false" title="基本资料">
        <!-- 头像(工单07)：JWT 在 header，fetch blob → objectURL 展示，上传 ≤2MB 图片 -->
        <div class="avatar-section">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-image" />
          <div v-else class="avatar-placeholder">
            <t-icon name="user" size="48px" />
          </div>
          <div class="avatar-actions">
            <input ref="avatarInput" type="file" accept=".png,.jpg,.jpeg,.gif" style="display: none" @change="onAvatarChange" />
            <t-button variant="outline" size="small" @click="avatarInput?.click()"> 更换头像 </t-button>
            <span class="avatar-tip">支持 png/jpg/gif，≤2MB</span>
          </div>
        </div>
        <div class="profile-item">
          <span class="profile-label">用户名</span>
          <span>{{ profile?.username ?? '-' }}</span>
        </div>
        <div class="profile-item">
          <span class="profile-label">昵称</span>
          <span>{{ profile?.nickname ?? '-' }}</span>
        </div>
        <div class="profile-item">
          <span class="profile-label">部门</span>
          <span>{{ profile?.deptName ?? '-' }}</span>
        </div>
        <div class="profile-item">
          <span class="profile-label">角色</span>
          <t-space size="small">
            <t-tag v-for="role in profile?.roleNames ?? []" :key="role" variant="outline">{{ role }}</t-tag>
          </t-space>
        </div>
        <div class="profile-item">
          <span class="profile-label">岗位</span>
          <t-space size="small">
            <t-tag v-for="post in profile?.postNames ?? []" :key="post" theme="warning" variant="outline">
              {{ post }}
            </t-tag>
            <span v-if="!profile?.postNames?.length">-</span>
          </t-space>
        </div>
        <t-button variant="outline" @click="nicknameVisible = true"> 修改昵称 </t-button>
      </t-card>
    </t-col>
    <t-col :span="7">
      <t-card :bordered="false" title="修改密码">
        <t-form ref="passwordFormInstance" :data="passwordForm" :rules="passwordRules" label-width="90px" @submit.prevent>
          <t-form-item label="旧密码" name="oldPassword">
            <t-input v-model="passwordForm.oldPassword" type="password" />
          </t-form-item>
          <t-form-item label="新密码" name="newPassword">
            <t-input v-model="passwordForm.newPassword" type="password" placeholder="至少 6 位" />
          </t-form-item>
          <t-form-item label="确认新密码" name="confirmPassword">
            <t-input v-model="passwordForm.confirmPassword" type="password" />
          </t-form-item>
          <t-form-item>
            <t-space>
              <t-button theme="primary" :loading="changing" @click="changePassword"> 确认修改 </t-button>
              <span class="password-tip">修改成功后所有登录会话将失效，需重新登录</span>
            </t-space>
          </t-form-item>
        </t-form>
      </t-card>
    </t-col>

    <t-dialog
      v-model:visible="nicknameVisible"
      header="修改昵称"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="420px"
      @confirm="changeNickname"
      @closed="nicknameFormInstance?.reset()"
    >
      <t-form ref="nicknameFormInstance" :data="nicknameForm" :rules="nicknameRules" label-width="80px" @submit.prevent>
        <t-form-item label="昵称" name="nickname">
          <t-input v-model="nicknameForm.nickname" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </t-row>
</template>

<script setup lang="ts">
  import { MessagePlugin } from 'tdesign-vue-next';
  import type { FormInstanceFunctions, FormRules } from 'tdesign-vue-next';
  import { onMounted, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';

  import type { AuthModels_ProfileResponse } from '@/api/__generated/model/static';
  import { clearToken } from '@/api/auth';
  import { api } from '@/api/jimmer';
  import { fetchAvatar, uploadAvatar } from '@/api/extra';

  const router = useRouter();

  // 头像(工单07)
  const avatarUrl = ref('');
  const avatarInput = ref<HTMLInputElement>();

  onMounted(() => {
    fetchAvatar()
      .then((info) => {
        if (info.hasAvatar && info.url) {
          avatarUrl.value = info.url;
        }
      })
      .catch(() => {
        // 无头像/接口异常保持占位图
      });
  });

  async function onAvatarChange(event: Event) {
    const files = (event.target as HTMLInputElement).files;
    if (!files || !files.length) {
      return;
    }
    try {
      await uploadAvatar(files[0]);
      MessagePlugin.success('头像已更新');
      const info = await fetchAvatar();
      if (info.hasAvatar && info.url) {
        avatarUrl.value = info.url;
      }
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      if (avatarInput.value) {
        avatarInput.value.value = '';
      }
    }
  }

  const profile = ref<AuthModels_ProfileResponse>();
  const loading = ref(false);

  const nicknameVisible = ref(false);
  const saving = ref(false);
  const nicknameFormInstance = ref<FormInstanceFunctions>();
  const nicknameForm = reactive({ nickname: '' });
  const nicknameRules: FormRules = {
    nickname: [{ required: true, message: '昵称不能为空', type: 'error' }],
  };

  const changing = ref(false);
  const passwordFormInstance = ref<FormInstanceFunctions>();
  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const passwordRules: FormRules = {
    oldPassword: [{ required: true, message: '旧密码不能为空', type: 'error' }],
    newPassword: [
      { required: true, message: '新密码不能为空', type: 'error' },
      { min: 6, message: '至少 6 位', type: 'error' },
    ],
    confirmPassword: [
      { required: true, message: '请再次输入新密码', type: 'error' },
      {
        validator: (value) => value === passwordForm.newPassword,
        message: '两次输入的新密码不一致',
        type: 'error',
      },
    ],
  };

  async function loadProfile() {
    loading.value = true;
    try {
      profile.value = await api.authController.profile();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      loading.value = false;
    }
  }

  async function changeNickname() {
    const valid = await nicknameFormInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    saving.value = true;
    try {
      await api.authController.changeNickname({ body: { nickname: nicknameForm.nickname } });
      MessagePlugin.success('昵称已更新');
      nicknameVisible.value = false;
      loadProfile();
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      saving.value = false;
    }
  }

  async function changePassword() {
    const valid = await passwordFormInstance.value?.validate();
    if (valid !== true) {
      return;
    }
    changing.value = true;
    try {
      await api.authController.changePassword({
        body: {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword,
        },
      });
      MessagePlugin.success('密码已修改，请重新登录');
      // 改密作废全部会话(含当前)，本地令牌一并清除
      clearToken();
      await router.push('/login');
    } catch (error) {
      MessagePlugin.error((error as Error).message);
    } finally {
      changing.value = false;
    }
  }

  onMounted(loadProfile);
</script>

<style scoped lang="less">
  .avatar-section {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .avatar-image {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      object-fit: cover;
    }

    .avatar-placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 72px;
      height: 72px;
      border-radius: 50%;
      color: var(--td-text-color-placeholder);
      background: var(--td-bg-color-component);
    }

    .avatar-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .avatar-tip {
      font-size: 12px;
      color: var(--td-text-color-placeholder);
    }
  }

  .profile-item {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .profile-label {
    width: 72px;
    color: var(--td-text-color-secondary);
  }

  .password-tip {
    font-size: 12px;
    color: var(--td-text-color-placeholder);
  }
</style>
