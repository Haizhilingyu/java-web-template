java-web-template 发行包
========================

运行要求
--------
JDK 21 或更高版本，且 java 命令在 PATH 中。

启动方式
--------
Windows:     双击 start.bat，或在命令行执行 start.bat
Linux/macOS: ./start.sh
通用:        java -jar app.jar

启动后浏览器访问 http://localhost:8080/ 即可打开前端页面。

说明
----
1. app.jar 为 Spring Boot 可执行 jar，web 模块(Vue3)的编译产物已内嵌在
   jar 的 static 资源目录中，前端页面由后端直接托管，无需单独部署前端。
2. 前端使用 history 路由，直接访问深层路由(如 /login)也可正常返回页面。
3. 如需修改端口: java -jar app.jar --server.port=9090
