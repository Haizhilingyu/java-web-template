import type {Executor} from './';
import {
    AuthController, 
    ConfigService, 
    DeptService, 
    DictDataService, 
    DictTypeService, 
    JobService, 
    MenuService, 
    NoticeService, 
    PostService, 
    RoleService, 
    UserService
} from './services/';

export class Api {
    
    readonly authController: AuthController
    
    readonly configService: ConfigService
    
    readonly deptService: DeptService
    
    readonly dictDataService: DictDataService
    
    readonly dictTypeService: DictTypeService
    
    readonly menuService: MenuService
    
    readonly noticeService: NoticeService
    
    readonly postService: PostService
    
    readonly roleService: RoleService
    
    readonly userService: UserService
    
    readonly jobService: JobService
    
    constructor(executor: Executor) {
        this.authController = new AuthController(executor);
        this.configService = new ConfigService(executor);
        this.deptService = new DeptService(executor);
        this.dictDataService = new DictDataService(executor);
        this.dictTypeService = new DictTypeService(executor);
        this.menuService = new MenuService(executor);
        this.noticeService = new NoticeService(executor);
        this.postService = new PostService(executor);
        this.roleService = new RoleService(executor);
        this.userService = new UserService(executor);
        this.jobService = new JobService(executor);
    }
}