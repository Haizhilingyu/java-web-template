package com.jezetek.modules.system.excel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;

/**
 * 操作日志导出行(工单04)：表头中文；超长的 params/result 不导出。
 * 保留无参构造+存取器：测试需用同一表头模型回读校验
 */
public class OperLogExportRow {

    @ExcelProperty("模块")
    @ColumnWidth(16)
    private String module;

    @ExcelProperty("动作")
    @ColumnWidth(16)
    private String action;

    @ExcelProperty("操作人")
    @ColumnWidth(16)
    private String operator;

    @ExcelProperty("URI")
    @ColumnWidth(30)
    private String uri;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("耗时(ms)")
    @ColumnWidth(12)
    private Long costMs;

    @ExcelProperty("异常信息")
    @ColumnWidth(30)
    private String errorMsg;

    @ExcelProperty("创建时间")
    @ColumnWidth(24)
    private String createdTime;

    public OperLogExportRow() {
    }

    public OperLogExportRow(String module, String action, String operator, String uri, String status,
                            Long costMs, String errorMsg, String createdTime) {
        this.module = module;
        this.action = action;
        this.operator = operator;
        this.uri = uri;
        this.status = status;
        this.costMs = costMs;
        this.errorMsg = errorMsg;
        this.createdTime = createdTime;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }
}
