/**
 * 从后端下载 jimmer 自动生成的 TypeScript API 客户端(/ts.zip)并解压到 src/api/__generated。
 * 用法: 先启动后端(app 模块, 端口 8080)，然后 npm run gen:api
 * 参考: jimmer-examples/rest-client/scripts/generate-api.js
 */
import { createWriteStream, existsSync, rmSync, unlinkSync } from 'node:fs';
import { mkdir, copyFile, readdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { get as httpGet } from 'node:http';
import AdmZip from 'adm-zip';

const sourceUrl = process.env.API_URL ?? 'http://localhost:8080/ts.zip';
const generatePath = 'src/api/__generated';
const tmpFile = join(tmpdir(), `jimmer-ts-${Date.now()}.zip`);

console.log(`Downloading ${sourceUrl} ...`);
await download(sourceUrl, tmpFile);
console.log(`Saved: ${tmpFile}`);

if (existsSync(generatePath)) {
  rmSync(generatePath, { recursive: true });
}
const zip = new AdmZip(tmpFile);
zip.extractAllTo(`${generatePath}__tmp`, true);

// __tmp 下直接就是 Api.ts / model / services 等，拍平到 __generated
await flatten(`${generatePath}__tmp`, generatePath);
rmSync(`${generatePath}__tmp`, { recursive: true });
unlinkSync(tmpFile);
console.log(`Generated -> ${generatePath}`);

async function download(url, target) {
  await new Promise((resolve, reject) => {
    const file = createWriteStream(target);
    httpGet(url, (response) => {
      if (response.statusCode !== 200) {
        reject(new Error(`下载失败: HTTP ${response.statusCode}，请确认后端已启动`));
        return;
      }
      response.pipe(file);
      file.on('finish', () => file.close(resolve));
    }).on('error', reject);
  });
}

async function flatten(src, dest) {
  await mkdir(dest, { recursive: true });
  const entries = await readdir(src, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory()) {
      await flatten(join(src, entry.name), join(dest, entry.name));
    } else {
      await copyFile(join(src, entry.name), join(dest, entry.name));
    }
  }
}
