import type {AuthModels_RouteMeta} from './';

/**
 * 与前端 tdesign-starter 的 RouteItem 对齐：
 * 目录 component 固定 "LAYOUT"，页面 component 为 pages 下的组件路径字符串
 */
export interface AuthModels_RouteVO {
    readonly name: string;
    readonly path: string;
    readonly component?: string | undefined;
    readonly redirect?: string | undefined;
    readonly meta: AuthModels_RouteMeta;
    readonly children?: ReadonlyArray<AuthModels_RouteVO> | undefined;
}
