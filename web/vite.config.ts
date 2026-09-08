import path from 'node:path';

import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import type { ConfigEnv, UserConfig } from 'vite';
import { loadEnv } from 'vite';
import { viteMockServe } from 'vite-plugin-mock';
import svgLoader from 'vite-svg-loader';

const CWD = process.cwd();

// https://vitejs.dev/config/
export default ({ mode }: ConfigEnv): UserConfig => {
  const { VITE_BASE_URL, VITE_API_URL_PREFIX } = loadEnv(mode, CWD);
  return {
    base: VITE_BASE_URL,
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },

    css: {
      preprocessorOptions: {
        less: {
          modifyVars: {
            hack: `true; @import (reference) "${path.resolve('src/style/variables.less')}";`,
          },
          math: 'strict',
          javascriptEnabled: true,
        },
      },
    },

    plugins: [
      vue(),
      vueJsx(),
      viteMockServe({
        mockPath: 'mock',
        enable: true,
      }),
      svgLoader(),
    ],

    server: {
      port: 3002,
      host: '0.0.0.0',
      allowedHosts: true,
      proxy: {
        [VITE_API_URL_PREFIX]: 'http://127.0.0.1:3000/',
      },
    },

    // https://github.com/vueuse/vueuse/issues/5387#issuecomment-4734186040
    build: {
      // 编译产物输出到 static 目录，与 Spring Boot 静态资源目录(classpath:/static)保持一致，
      // 便于 Maven 构建时直接打入 web 模块 jar
      outDir: 'static',
      rolldownOptions: {
        onLog(level, log, defaultHandler) {
          if (log.code === 'INVALID_ANNOTATION') return null;
          else defaultHandler(level, log);
        },
      },
    },
  };
};
