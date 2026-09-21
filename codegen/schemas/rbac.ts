import type { Schema } from '../src/schema.js';

/**
 * 示例模块 Schema：课程管理(教师/课程)。
 * 展示模块骨架的完整约定：/api/v1/{code} 路径、{code}:{实体}:{动作} 权限点、
 * 多对一关联、业务键、超级 QBE。产物仅供对照参考，复制进工程前需按业务调整
 */
const schema: Schema = {
    project: {
        moduleCode: 'course',
        moduleName: '课程管理',
        groupId: 'com.jezetek.modules',
        version: '0.0.1-SNAPSHOT',
        rootProject: { groupId: 'com.jezetek', artifactId: 'java-web-template', version: '0.0.1-SNAPSHOT' },
        jimmerVersion: '0.12.0',
        javaVersion: '21',
    },
    entities: [
        {
            name: 'Teacher',
            table: 'course_teacher',
            comment: '教师',
            label: '教师',
            tenantAware: true,
            defaultSort: 'code asc',
            icon: 'user-1',
            props: [
                { kind: 'id' },
                {
                    kind: 'scalar', name: 'code', type: 'string', length: 50, key: true,
                    comment: '工号，全局唯一',
                    validation: ['@jakarta.validation.constraints.NotBlank(message = "工号不能为空")'],
                },
                {
                    kind: 'scalar', name: 'name', type: 'string', length: 50,
                    validation: ['@jakarta.validation.constraints.NotBlank(message = "姓名不能为空")'],
                    comment: '姓名',
                },
                { kind: 'scalar', name: 'enabled', type: 'boolean', comment: '是否在职' },
            ],
            search: {
                keyword: ['code', 'name'],
                eq: ['enabled'],
            },
        },
        {
            name: 'Course',
            table: 'course_course',
            comment: '课程',
            label: '课程',
            tenantAware: true,
            defaultSort: 'name asc',
            icon: 'book',
            props: [
                { kind: 'id' },
                {
                    kind: 'scalar', name: 'name', type: 'string', length: 100,
                    validation: ['@jakarta.validation.constraints.NotBlank(message = "课程名不能为空")'],
                    comment: '课程名',
                },
                { kind: 'scalar', name: 'score', type: 'int', comment: '学分' },
                {
                    kind: 'manyToOne', name: 'teacher', target: 'Teacher',
                    comment: '授课教师，删除被引用教师需先调整课程',
                },
            ],
            search: {
                keyword: ['name'],
            },
        },
    ],
    seeds: {
        Teacher: ["(1, 'T001', '张三', true, 'default', current_timestamp(), current_timestamp())"],
        Course: ["(1, 'jimmer 入门', 3, 1, 'default', current_timestamp(), current_timestamp())"],
    },
};

export default schema;
