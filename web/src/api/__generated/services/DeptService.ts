import type {Executor} from '../';
import type {DeptDto} from '../model/dto/';
import type {DeptInput} from '../model/static/';

export class DeptService {
    
    constructor(private executor: Executor) {}
    
    /**
     * 生命周期约束：存在子部门或部门下有用户时禁止删除；
     * 禁用不追溯，已挂用户照常生效
     */
    readonly deleteDept: (options: DeptServiceOptions['deleteDept']) => Promise<
        void
    > = async(options) => {
        let _uri = '/api/v1/dept/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'DELETE'})) as Promise<void>;
    }
    
    readonly findDept: (options: DeptServiceOptions['findDept']) => Promise<
        DeptDto['DeptService/DEFAULT_FETCHER'] | undefined
    > = async(options) => {
        let _uri = '/api/v1/dept/';
        _uri += encodeURIComponent(options.id);
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<DeptDto['DeptService/DEFAULT_FETCHER'] | undefined>;
    }
    
    /**
     * 部门树：只查根节点，子部门由递归 fetcher 抓取。
     * 用户表单的部门选择器等也要消费，任何登录用户可见(与菜单树一致)
     */
    readonly findDepts: () => Promise<
        ReadonlyArray<DeptDto['DeptService/TREE_FETCHER']>
    > = async() => {
        let _uri = '/api/v1/dept/list';
        return (await this.executor({uri: _uri, method: 'GET'})) as Promise<ReadonlyArray<DeptDto['DeptService/TREE_FETCHER']>>;
    }
    
    readonly saveDept: (options: DeptServiceOptions['saveDept']) => Promise<
        DeptDto['DeptService/DEFAULT_FETCHER']
    > = async(options) => {
        let _uri = '/api/v1/dept';
        return (await this.executor({uri: _uri, method: 'PUT', body: options.body})) as Promise<DeptDto['DeptService/DEFAULT_FETCHER']>;
    }
}

export type DeptServiceOptions = {
    'findDepts': {}, 
    'findDept': {
        readonly id: number
    }, 
    'saveDept': {
        readonly body: DeptInput
    }, 
    'deleteDept': {
        readonly id: number
    }
}
