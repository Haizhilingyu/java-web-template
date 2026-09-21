package com.jezetek.modules.system.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;

/**
 * 用户导入行 + 模板表头(工单03)：与若依同构，一行一个账号。
 * 部门用编号(部门名非全局唯一，编号无歧义)；状态用中文 启用/禁用。
 * 注意 system 模块注解处理器为 jimmer-apt(无 lombok)，属性手写存取器
 */
public class UserImportRow {

    @ExcelProperty("用户名")
    @ColumnWidth(20)
    private String username;

    @ExcelProperty("昵称")
    @ColumnWidth(20)
    private String nickname;

    @ExcelProperty("密码")
    @ColumnWidth(20)
    private String password;

    @ExcelProperty("部门编号")
    @ColumnWidth(12)
    private Long deptId;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
