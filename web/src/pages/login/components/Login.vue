<template>
  <t-form
    ref="form"
    class="item-container"
    :class="[`login-${type}`]"
    :data="formData"
    :rules="FORM_RULES"
    label-width="0"
    @submit="onSubmit"
  >
    <t-form-item name="username">
      <t-input v-model="formData.username" size="large" clearable placeholder="用户名：admin">
        <template #prefix-icon>
          <t-icon name="user" />
        </template>
      </t-input>
    </t-form-item>

    <t-form-item name="password">
      <t-input
        v-model="formData.password"
        size="large"
        :type="showPsw ? 'text' : 'password'"
        clearable
        placeholder="密码：123456"
      >
        <template #prefix-icon>
          <t-icon name="lock-on" />
        </template>
        <template #suffix-icon>
          <t-icon :name="showPsw ? 'browse' : 'browse-off'" @click="showPsw = !showPsw" />
        </template>
      </t-input>
    </t-form-item>

    <t-form-item v-if="captcha.enabled" name="captchaCode">
      <div class="captcha-row">
        <t-input
          v-model="formData.captchaCode"
          size="large"
          clearable
          placeholder="验证码"
        >
          <template #prefix-icon>
            <t-icon name="verified" />
          </template>
        </t-input>
        <!-- 点击换图：Base64 PNG 直出，免登录接口 -->
        <img
          v-if="captcha.image"
          :src="captcha.image"
          alt="验证码"
          title="点击刷新"
          class="captcha-image"
          @click="refreshCaptcha"
        />
      </div>
    </t-form-item>

    <t-form-item class="btn-container" :status="submitError ? 'error' : undefined" :help="submitError">
      <t-button block size="large" type="submit" :loading="loading"> 登录 </t-button>
    </t-form-item>
  </t-form>
</template>
<script setup lang="ts">
import type { FormInstanceFunctions, FormRule, SubmitContext } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { fetchCaptcha, type CaptchaInfo } from '@/api/auth';
import { useUserStore } from '@/store';

const userStore = useUserStore();

const INITIAL_DATA = {
  username: 'admin',
  password: '123456',
  captchaCode: '',
};

const FORM_RULES = computed<Record<string, FormRule[]>>(() => {
  const rules: Record<string, FormRule[]> = {
    username: [{ required: true, message: '请输入用户名', type: 'error' }],
    password: [{ required: true, message: '请输入密码', type: 'error' }],
  };
  if (captcha.value.enabled) {
    rules.captchaCode = [{ required: true, message: '请输入验证码', type: 'error' }];
  }
  return rules;
});

const type = 'password';

const form = ref<FormInstanceFunctions>();
const formData = ref({ ...INITIAL_DATA });
const showPsw = ref(false);
const loading = ref(false);
const submitError = ref('');
const captcha = ref<CaptchaInfo>({ enabled: false });

const router = useRouter();
const route = useRoute();

async function refreshCaptcha() {
  try {
    captcha.value = await fetchCaptcha();
  } catch {
    // 拉取失败按未开启渲染，登录时后端仍会校验
    captcha.value = { enabled: false };
  }
}

onMounted(() => {
  refreshCaptcha();
});

const onSubmit = async (ctx: SubmitContext) => {
  if (ctx.validateResult !== true) {
    return;
  }
  loading.value = true;
  submitError.value = '';
  try {
    await userStore.login({
      username: formData.value.username,
      password: formData.value.password,
      captchaKey: captcha.value.key,
      captchaCode: formData.value.captchaCode,
    });

    MessagePlugin.success('登录成功');
    const redirect = route.query.redirect as string;
    router.push(redirect ? decodeURIComponent(redirect) : '/');
  } catch (e: unknown) {
    submitError.value = (e as Error).message;
    MessagePlugin.error(submitError.value);
    // 失败后验证码必已消费(一次性)，换一张
    formData.value.captchaCode = '';
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};
</script>
<style lang="less" scoped>
@import '../index.less';

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;

  .captcha-image {
    height: 40px;
    margin-top: 1px;
    border-radius: 3px;
    cursor: pointer;
  }
}
</style>
