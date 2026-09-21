package com.jezetek.modules.system.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;

/**
 * 用户导出行(工单03)：表头中文，按当前查询条件全量。
 * 保留无参构造+存取器：测试需用同一表头模型回读校验
 */
public class UserExportRow {

    @ExcelProperty("用户名")
    @ColumnWidth(20)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(20)
    private String nickname;

    @ExcelProperty("部门")
    @ColumnWidth(20)
    private String dept;

    @ExcelProperty("岗位")
    @ColumnWidth(20)
    private String posts;

    @ExcelProperty("角色")
    @ColumnWidth(20)
    private String roles;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("创建时间")
    @ColumnWidth(24)
    private String createdTime;

    public UserExportRow() {
    }

    public UserExportRow(String username, String nickname, String dept, String posts, String roles,
                         String status, String createdTime) {
        this.username = username;
        this.nickname = nickname;
        this.dept = dept;
        this.posts = posts;
        this.roles = roles;
        this.status = status;
        this.createdTime = createdTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getPosts() {
        return posts;
    }

    public void setPosts(String posts) {
        this.posts = posts;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }
}
