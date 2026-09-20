import type {Executor} from './';
import {
    AuthController, 
    DeptService, 
    JobService, 
    MenuService, 
    PostService, 
    RoleService, 
    UserService
} from './services/';

export class Api {
    
    readonly authController: AuthController
    
    readonly deptService: DeptService
    
    readonly menuService: MenuService
    
    readonly postService: PostService
    
    readonly roleService: RoleService
    
    readonly userService: UserService
    
    readonly jobService: JobService
    
    constructor(executor: Executor) {
        this.authController = new AuthController(executor);
        this.deptService = new DeptService(executor);
        this.menuService = new MenuService(executor);
        this.postService = new PostService(executor);
        this.roleService = new RoleService(executor);
        this.userService = new UserService(executor);
        this.jobService = new JobService(executor);
    }
}