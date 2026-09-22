package com.jezetek.modules.system.excel;

import cn.idev.excel.FastExcel;
import com.jezetek.core.runtime.BusinessException;
import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.DeptRepository;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.dto.UserSpecification;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户 Excel 导入导出(工单03)：普通 @RestController 端点。
 * multipart/流式下载与 jimmer 远程服务参数序列化不搭，照 AuthController 的写法——
 * 方法签名不出现 servlet/multipart 类型(规避 jimmer-apt 编译期 NPE)，
 * 一律经当前请求上下文获取。
 *
 * <p>导出走与列表一致的 Specification + 部门树过滤；数据范围不在此生效
 * (导出权限点仅授予管理员，范围收紧时再接入)</p>
 */
@RestController
@RequestMapping("/api/v1/user")
@Transactional
public class UserExcelController implements Fetchers {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");

    /** 导入行数上限：读取后校验拒绝(内存兜底另有 multipart 10MB 上限) */
    private static final int MAX_IMPORT_ROWS = 1000;

    /** 导入的默认初始密码(与种子冒烟账号一致) */
    private static final String DEFAULT_IMPORT_PASSWORD = "123456";

    private final UserRepository userRepository;

    private final DeptRepository deptRepository;

    private final PasswordEncoder passwordEncoder;

    public UserExcelController(
            UserRepository userRepository,
            DeptRepository deptRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.deptRepository = deptRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 按当前查询条件全量导出(不分页)，表头中文
     */
    @Log(module = "用户管理", action = "导出用户")
    @PreAuthorize("@perm.has('system:user:export')")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Long deptId
    ) throws IOException {
        UserSpecification specification = new UserSpecification();
        specification.setKeyword(blankToNull(keyword));
        specification.setEnabled(enabled);
        specification.setRoleName(blankToNull(roleName));
        Collection<Long> treeDeptIds = deptId == null ? null : deptRepository.findSelfAndDescendantIds(deptId);

        List<User> users = userRepository.listAll(specification, treeDeptIds, EXPORT_FETCHER);
        List<UserExportRow> rows = users.stream().map(UserExcelController::toExportRow).toList();

        HttpServletResponse response = currentResponse();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode("用户列表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=users.xlsx; filename*=UTF-8''" + encoded + ".xlsx");
        // inMemory：SXSSF 的临时文件在 Windows 上偶发删除失败("Can not close IO")，改纯内存写
        FastExcel.write(response.getOutputStream(), UserExportRow.class)
                .inMemory(true)
                .sheet("用户数据")
                .doWrite(rows);
    }

    /**
     * 导入模板下载：只有表头的空表
     */
    @Log(module = "用户管理", action = "下载导入模板")
    @PreAuthorize("@perm.has('system:user:import')")
    @GetMapping("/import-template")
    public void importTemplate() throws IOException {
        HttpServletResponse response = currentResponse();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode("用户导入模板", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=user-import-template.xlsx; filename*=UTF-8''" + encoded + ".xlsx");
        FastExcel.write(response.getOutputStream(), UserImportRow.class)
                .inMemory(true)
                .sheet("用户数据")
                .doWrite(List.of());
    }

    /**
     * Excel 导入：成功行入库、失败行回显行号+原因(与若依行为一致)。
     * 行号 = Excel 数据行号(表头为第 1 行，首条数据为第 2 行)
     */
    @Log(module = "用户管理", action = "导入用户")
    @PreAuthorize("@perm.has('system:user:import')")
    @PostMapping("/import")
    public ImportResult importUsers() throws IOException, jakarta.servlet.ServletException {
        MultipartFile file = currentMultipartFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }
        List<UserImportRow> rows = new ArrayList<>();
        FastExcel.read(file.getInputStream(), UserImportRow.class,
                new cn.idev.excel.event.AnalysisEventListener<UserImportRow>() {
                    @Override
                    public void invoke(UserImportRow row, cn.idev.excel.context.AnalysisContext context) {
                        rows.add(row);
                    }

                    @Override
                    public void doAfterAllAnalysed(cn.idev.excel.context.AnalysisContext context) {
                        // 收集模式：全部行已在 rows，无需收尾
                    }
                }).sheet().doRead();
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BusinessException("单次最多导入" + MAX_IMPORT_ROWS + "行");
        }

        int successCount = 0;
        List<RowFailure> failures = new ArrayList<>();
        Set<String> fileUsernames = new HashSet<>();
        int rowNum = 1;
        for (UserImportRow row : rows) {
            rowNum++;
            String reason = validate(row, fileUsernames);
            if (reason != null) {
                failures.add(new RowFailure(rowNum, reason));
                continue;
            }
            fileUsernames.add(row.getUsername().trim());
            boolean enabled = row.getStatus() == null || row.getStatus().isBlank()
                    || "启用".equals(row.getStatus().trim());
            User entity = UserDraft.$.produce(draft -> {
                draft.setUsername(row.getUsername().trim());
                String rawPassword = row.getPassword() == null || row.getPassword().isBlank()
                        ? DEFAULT_IMPORT_PASSWORD
                        : row.getPassword().trim();
                draft.setPassword(passwordEncoder.encode(rawPassword));
                if (row.getNickname() != null && !row.getNickname().isBlank()) {
                    draft.setNickname(row.getNickname().trim());
                }
                draft.setEnabled(enabled);
                if (row.getDeptId() != null) {
                    draft.setDeptId(row.getDeptId());
                }
            });
            userRepository.saveCommand(entity)
                    .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.NON_IDEMPOTENT_UPSERT)
                    .execute();
            successCount++;
        }
        return new ImportResult(rows.size(), successCount, failures);
    }

    /** 行级校验：通过返回 null，否则返回失败原因 */
    private String validate(UserImportRow row, Set<String> fileUsernames) {
        String username = row.getUsername() == null ? "" : row.getUsername().trim();
        if (username.isEmpty()) {
            return "用户名不能为空";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "用户名须为3~50位字母、数字或下划线";
        }
        if (fileUsernames.contains(username)) {
            return "文件内用户名重复";
        }
        if (userRepository.findByUsername(username, null).isPresent()) {
            return "用户名已存在";
        }
        if (row.getDeptId() != null && !deptRepository.existsById(row.getDeptId())) {
            return "部门编号" + row.getDeptId() + "不存在";
        }
        String status = row.getStatus() == null || row.getStatus().isBlank()
                ? null : row.getStatus().trim();
        if (status != null && !"启用".equals(status) && !"禁用".equals(status)) {
            return "状态仅支持 启用/禁用";
        }
        return null;
    }

    private static UserExportRow toExportRow(User user) {
        return new UserExportRow(
                user.username(),
                user.nickname(),
                user.dept() != null ? user.dept().name() : "",
                user.posts().stream().map(post -> post.name()).distinct().collect(java.util.stream.Collectors.joining("、")),
                user.roles().stream().map(role -> role.name()).distinct().collect(java.util.stream.Collectors.joining("、")),
                user.enabled() ? "启用" : "禁用",
                user.createdTime() == null ? "" : user.createdTime().toString()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 从当前请求取 multipart 文件：签名不出现 multipart 类型(见类注释)。
     * Spring 包装(MockMvc)优先，生产回退 Servlet Part(见 PartMultipartFile)
     */
    private static MultipartFile currentMultipartFile() throws IOException, jakarta.servlet.ServletException {
        return com.jezetek.modules.system.security.PartMultipartFile.fromRequest(currentRequest(), "file");
    }

    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private static HttpServletResponse currentResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    }

    /**
     * 导出抓取形状：用户标量 + 部门/岗位/角色名称
     */
    private static final Fetcher<User> EXPORT_FETCHER =
            USER_FETCHER
                    .allScalarFields()
                    .avatar(false)
                    .tenant(false)
                    .dept(DEPT_FETCHER.name())
                    .posts(POST_FETCHER.name())
                    .roles(ROLE_FETCHER.name());

    /**
     * 导入结果：total=文件数据行数；failures 回显行号+原因
     */
    public record ImportResult(int total, int successCount, List<RowFailure> failures) {
    }

    public record RowFailure(int rowNum, String reason) {
    }
}
