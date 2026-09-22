# 07: 头像上传

**What to build:** User 实体加可空 avatar（byte[]），system 建表 SQL 同步加列。`POST /api/v1/auth/avatar`（multipart，≤2MB，图片扩展名白名单）+ `GET /api/v1/auth/avatar`（authenticated 流式返回；JWT 在 header，前端 fetch blob → objectURL 展示，不用 img 直链）。Header 与个人中心显示头像。**不抽通用文件服务**，头像内联实现。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 上传后 Header/个人中心显示新头像，重新登录仍在
- [x] 超 2MB 或非图片类型被拒并提示
- [x] 未登录访问头像接口 401
- [x] 测试覆盖写读+超限拒绝；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit

## Comments

2026-09-22 完成。

- 落地内容：User 实体加可空 avatar（byte[]），main/test 两份 schema 同步加 avatar blob 列（database-validation ERROR 模式下必须同步）；列表/认证/导出等 USER_FETCHER.allScalarFields 处一律 .avatar(false) 防 BLOB 拖库。POST/GET /api/v1/auth/avatar（authenticated）：≤2MB+扩展名白名单 png/jpg/jpeg/gif，魔数识别内容类型流式返回，无头像 404；不抽通用文件服务内联实现。multipart 读取双通道 PartMultipartFile.fromRequest——Spring 包装(MockMvc)优先、生产栈回退 request.getPart（冒烟发现：控制器签名无 MultipartFile 时 Spring 7 不包装 MultipartHttpServletRequest，纯包装方案生产 400）。app yml multipart 10MB。
- 前端：Header 与个人中心 fetch blob→objectURL 展示（JWT 在 header 不用 img 直链），个人中心更换头像。
- 测试：AvatarTest 5 用例（写读回环+png 魔数识别/重登仍在+gif 覆盖/超 2MB 400/非法扩展 400+无头像 404/未登录 401）。
- 浏览器冒烟：curl 生产上传 200+读取 200 image/png，Header 与个人中心头像 blob 加载成功 ✓。
