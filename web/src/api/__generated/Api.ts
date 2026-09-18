import type {Executor} from './';
import {
    AuthController, 
    JobService, 
    MenuService, 
    RoleService, 
    UserService
} from './services/';

export class Api {
    
    readonly authController: AuthController
    
    readonly menuService: MenuService
    
    readonly roleService: RoleService
    
    readonly userService: UserService
    
    readonly jobService: JobService
    
    constructor(executor: Executor) {
        this.authController = new AuthController(executor);
        this.menuService = new MenuService(executor);
        this.roleService = new RoleService(executor);
        this.userService = new UserService(executor);
        this.jobService = new JobService(executor);
    }
}