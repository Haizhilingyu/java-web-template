package com.jezetek.modules.system.excel;

import cn.idev.excel.FastExcel;
import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Logininfor;
import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.repository.LogininforRepository;
import com.jezetek.modules.system.repository.OperLogRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 操作日志/登录日志导出(工单04)：复用工单03 的 FastExcel 依赖与
 * 流式下载写法(签名不出现 servlet 类型，见 UserExcelController 类注释)。
 * 权限沿用日志族约定：system:log:export
 */
@RestController
@Transactional
public class LogExcelController implements Fetchers {

    private final OperLogRepository operLogRepository;

    private final LogininforRepository logininforRepository;

    public LogExcelController(OperLogRepository operLogRepository, LogininforRepository logininforRepository) {
        this.operLogRepository = operLogRepository;
        this.logininforRepository = logininforRepository;
    }

    @Log(module = "操作日志", action = "导出操作日志")
    @PreAuthorize("@perm.has('system:log:export')")
    @GetMapping("/api/v1/operlog/export")
    public void exportOperLogs(@RequestParam(required = false) String keyword) throws IOException {
        List<OperLog> logs = operLogRepository.listAll(blankToNull(keyword), OPER_LOG_FETCHER.allScalarFields());
        List<OperLogExportRow> rows = logs.stream()
                .map(log -> new OperLogExportRow(
                        log.module(),
                        log.action(),
                        log.operator(),
                        log.uri(),
                        log.success() ? "成功" : "失败",
                        log.costMs(),
                        log.errorMsg(),
                        log.createdTime() == null ? "" : log.createdTime().toString()))
                .toList();
        writeSheet("操作日志", "operlog-export.xlsx", OperLogExportRow.class, rows);
    }

    @Log(module = "登录日志", action = "导出登录日志")
    @PreAuthorize("@perm.has('system:log:export')")
    @GetMapping("/api/v1/logininfor/export")
    public void exportLogininfors(@RequestParam(required = false) String keyword) throws IOException {
        List<Logininfor> logs = logininforRepository.listAll(blankToNull(keyword), LOGININFOR_FETCHER.allScalarFields());
        List<LogininforExportRow> rows = logs.stream()
                .map(log -> new LogininforExportRow(
                        log.username(),
                        log.ip(),
                        log.message(),
                        log.createdTime() == null ? "" : log.createdTime().toString()))
                .toList();
        writeSheet("登录日志", "logininfor-export.xlsx", LogininforExportRow.class, rows);
    }

    private void writeSheet(String sheetTitle, String filename, Class<?> head, List<?> rows) throws IOException {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getResponse();
        if (response == null) {
            throw new IllegalStateException("无当前响应上下文");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(sheetTitle, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + filename + "; filename*=UTF-8''" + encoded + ".xlsx");
        // inMemory：SXSSF 临时文件在 Windows 上偶发删除失败，改纯内存写(见 UserExcelController)
        FastExcel.write(response.getOutputStream(), head)
                .inMemory(true)
                .sheet(sheetTitle)
                .doWrite(rows);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
