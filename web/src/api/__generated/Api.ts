import type {Executor} from './';
import {MenuService, RoleService, UserService} from './services/';

export class Api {
    
    readonly menuService: MenuService
    
    readonly roleService: RoleService
    
    readonly userService: UserService
    
    constructor(executor: Executor) {
        this.menuService = new MenuService(executor);
        this.roleService = new RoleService(executor);
        this.userService = new UserService(executor);
    }
}