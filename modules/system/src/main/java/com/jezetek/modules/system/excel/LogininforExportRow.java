package com.jezetek.modules.system.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;

/**
 * 登录日志导出行(工单04)：表头中文。
 * 保留无参构造+存取器：测试需用同一表头模型回读校验
 */
public class LogininforExportRow {

    @ExcelProperty("账号")
    @ColumnWidth(20)
    private String username;

    @ExcelProperty("IP")
    @ColumnWidth(20)
    private String ip;

    @ExcelProperty("结果")
    @ColumnWidth(30)
    private String message;

    @ExcelProperty("创建时间")
    @ColumnWidth(24)
    private String createdTime;

    public LogininforExportRow() {
    }

    public LogininforExportRow(String username, String ip, String message, String createdTime) {
        this.username = username;
        this.ip = ip;
        this.message = message;
        this.createdTime = createdTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }
}
