import type {Executor} from './';
import {
    AuthController, 
    ConfigService, 
    DeptService, 
    DictDataService, 
    DictTypeService, 
    JobService, 
    LogininforService, 
    MenuService, 
    NoticeService, 
    OnlineService, 
    OperLogService, 
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
    
    readonly logininforService: LogininforService
    
    readonly menuService: MenuService
    
    readonly noticeService: NoticeService
    
    readonly onlineService: OnlineService
    
    readonly operLogService: OperLogService
    
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
        this.logininforService = new LogininforService(executor);
        this.menuService = new MenuService(executor);
        this.noticeService = new NoticeService(executor);
        this.onlineService = new OnlineService(executor);
        this.operLogService = new OperLogService(executor);
        this.postService = new PostService(executor);
        this.roleService = new RoleService(executor);
        this.userService = new UserService(executor);
        this.jobService = new JobService(executor);
    }
}