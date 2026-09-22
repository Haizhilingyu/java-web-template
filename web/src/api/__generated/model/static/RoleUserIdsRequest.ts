/**
 * 分配用户请求体(工单06)：POST/DELETE /role/{id}/users 共用。
 * 
 * <p>必须是顶层类——嵌在 @RestController 里的 public record/类，
 * 其隐式 public 方法会被 jimmer-apt 当成无映射的 API 方法，
 * 编译期直接崩溃(同 LoginLogWriter 不能挂在 Service 上的原因)</p>
 */
export interface RoleUserIdsRequest {
    readonly userIds: ReadonlyArray<number>;
}
