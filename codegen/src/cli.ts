import { mkdir, writeFile, rm } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { pathToFileURL } from 'node:url';
import type { Schema } from './schema.js';
import { packagePath } from './support.js';
import { genBaseEntity, genEntity, genTenantAware } from './gen/entity.js';
import { genRepository } from './gen/repository.js';
import { genDto, genDtoGeneration, genService } from './gen/service.js';
import { genRuntimeFiles } from './gen/runtime.js';
import { genDataSql, genSchemaSql } from './gen/sql.js';
import { genPoms } from './gen/pom.js';

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
    if (!schema?.project || !Array.isArray(schema.entities)) {
        throw new Error(`${path} 缺少默认导出的 Schema 对象`);
    }
    return schema;
}

async function main() {
    const args = parseArgs(process.argv.slice(2));
    const schema = await loadSchema(args.schema);
    const outDir = resolve(args.out);
    const srcPath = (module: string) =>
        `${outDir}/${module}/src/main/java/${packagePath(schema.project.javaPackage)}`;

    const files: Record<string, string> = {};

    // ---- Maven poms ----
    Object.assign(files, prefixKeys(genPoms(schema), outDir));

    // ---- model 模块: 实体 + 公共基类 ----
    const modelDir = srcPath('model') + '/model';
    const usesTenant = schema.entities.some(e => e.tenantAware);
    for (const entity of schema.entities) {
        files[`${modelDir}/${entity.name}.java`] = genEntity(schema, entity);
    }
    if (schema.entities.some(e => e.baseEntity !== false)) {
        files[`${modelDir}/common/BaseEntity.java`] = genBaseEntity(schema.project.javaPackage);
    }
    if (usesTenant) {
        files[`${modelDir}/common/TenantAware.java`] = genTenantAware(schema.project.javaPackage);
    }

    // ---- repository 模块 ----
    const repoDir = srcPath('repository') + '/repository';
    for (const entity of schema.entities) {
        files[`${repoDir}/${entity.name}Repository.java`] = genRepository(schema, entity);
    }

    // ---- runtime 模块 ----
    const runtimeFiles = genRuntimeFiles(schema);
    for (const [name, content] of Object.entries(runtimeFiles)) {
        files[`${srcPath('runtime')}/runtime/${name}`] = content;
    }

    // ---- service 模块: 服务 + DTO + APT 开关 ----
    const serviceDir = srcPath('service') + '/service';
    files[`${serviceDir}/DtoGeneration.java`] = genDtoGeneration(schema.project.javaPackage);
    for (const entity of schema.entities) {
        files[`${serviceDir}/${entity.name}Service.java`] = genService(schema, entity);
        files[`${outDir}/service/src/main/dto/${entity.name}.dto`] = genDto(schema, entity);
    }

    // ---- SQL ----
    files[`${outDir}/sql/h2-schema.sql`] = genSchemaSql(schema);
    const dataSql = genDataSql(schema);
    if (dataSql) {
        files[`${outDir}/sql/h2-data.sql`] = dataSql;
    }

    // ---- 写出 ----
    if (args.clean) {
        await rm(outDir, { recursive: true, force: true });
    }
    for (const [path, content] of Object.entries(files)) {
        await mkdir(dirname(path), { recursive: true });
        await writeFile(path, content, 'utf8');
    }

    console.log(`已生成 ${Object.keys(files).length} 个文件 -> ${outDir}`);
    console.log([...Object.keys(files)].sort().map(f => `  ${f}`).join('\n'));
}

function prefixKeys(map: Record<string, string>, prefix: string): Record<string, string> {
    return Object.fromEntries(
        Object.entries(map).map(([k, v]) => [`${prefix}/${k}`, v])
    );
}

main().catch(err => {
    console.error(err);
    process.exit(1);
});
