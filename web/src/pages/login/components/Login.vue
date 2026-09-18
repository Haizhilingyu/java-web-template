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

    <t-form-item class="btn-container" :status="submitError ? 'error' : undefined" :help="submitError">
      <t-button block size="large" type="submit" :loading="loading"> 登录 </t-button>
    </t-form-item>
  </t-form>
</template>
<script setup lang="ts">
import type { FormInstanceFunctions, FormRule, SubmitContext } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useUserStore } from '@/store';

const userStore = useUserStore();

const INITIAL_DATA = {
  username: 'admin',
  password: '123456',
};

const FORM_RULES = computed<Record<string, FormRule[]>>(() => ({
  username: [{ required: true, message: '请输入用户名', type: 'error' }],
  password: [{ required: true, message: '请输入密码', type: 'error' }],
}));

const type = 'password';

const form = ref<FormInstanceFunctions>();
const formData = ref({ ...INITIAL_DATA });
const showPsw = ref(false);
const loading = ref(false);
const submitError = ref('');

const router = useRouter();
const route = useRoute();

const onSubmit = async (ctx: SubmitContext) => {
  if (ctx.validateResult !== true) {
    return;
  }
  loading.value = true;
  submitError.value = '';
  try {
    await userStore.login(formData.value);

    MessagePlugin.success('登录成功');
    const redirect = route.query.redirect as string;
    router.push(redirect ? decodeURIComponent(redirect) : '/');
  } catch (e: unknown) {
    submitError.value = (e as Error).message;
    MessagePlugin.error(submitError.value);
  } finally {
    loading.value = false;
  }
};
</script>
<style lang="less" scoped>
@import '../index.less';
</style>
