# 07: 头像上传

**What to build:** User 实体加可空 avatar（byte[]），system 建表 SQL 同步加列。`POST /api/v1/auth/avatar`（multipart，≤2MB，图片扩展名白名单）+ `GET /api/v1/auth/avatar`（authenticated 流式返回；JWT 在 header，前端 fetch blob → objectURL 展示，不用 img 直链）。Header 与个人中心显示头像。**不抽通用文件服务**，头像内联实现。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] 上传后 Header/个人中心显示新头像，重新登录仍在
- [ ] 超 2MB 或非图片类型被拒并提示
- [ ] 未登录访问头像接口 401
- [ ] 测试覆盖写读+超限拒绝；system 测试全绿；vue-tsc 零错误；浏览器冒烟；一个 commit
