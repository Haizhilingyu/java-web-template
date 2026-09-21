import { mkdir, writeFile, rm } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { pathToFileURL } from 'node:url';
import type { Schema } from './schema.js';
import { genModuleFiles } from './gen/module.js';

interface Args {
    schema: string;
    out: string;
    clean: boolean;
}

function parseArgs(argv: string[]): Args {
    const args: Args = { schema: 'schemas/rbac.ts', out: 'out', clean: true };
    const it = argv[Symbol.iterator]();
    for (const a of it) {
        switch (a) {
            case '-s': case '--schema': args.schema = it.next().value ?? ''; break;
            case '-o': case '--out': args.out = it.next().value ?? 'out'; break;
            case '--no-clean': args.clean = false; break;
            default:
                if (!a.startsWith('-')) {
                    args.schema = a;
                }
        }
    }
    if (!args.schema) {
        console.error('用法: npm run gen -- [-s schema文件] [-o 输出目录] [--no-clean]');
        process.exit(1);
    }
    return args;
}

async function loadSchema(path: string): Promise<Schema> {
    const abs = resolve(path);
    const mod = await import(pathToFileURL(abs).href);
    const schema = (mod.default ?? mod.schema) as Schema | undefined;
    if (!schema?.project?.moduleCode || !Array.isArray(schema.entities)) {
        throw new Error(`${path} 缺少默认导出的 Schema 对象(需含 project.moduleCode 与 entities)`);
    }
    return schema;
}

async function main() {
    const args = parseArgs(process.argv.slice(2));
    const schema = await loadSchema(args.schema);
    const outDir = resolve(args.out);

    const files = genModuleFiles(schema);

    if (args.clean) {
        await rm(outDir, { recursive: true, force: true });
    }
    for (const [path, content] of Object.entries(files)) {
        const target = `${outDir}/${path}`;
        await mkdir(dirname(target), { recursive: true });
        await writeFile(target, content, 'utf8');
    }

    console.log(`已生成 ${Object.keys(files).length} 个文件 -> ${outDir}`);
    console.log(Object.keys(files).sort().map(f => `  ${f}`).join('\n'));
}

main().catch(err => {
    console.error(err);
    process.exit(1);
});
