package com.jezetek.modules.system.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

/**
 * Servlet {@code Part} → Spring {@link MultipartFile} 适配(工单07)。
 *
 * <p>生产栈上 Spring 未必把 multipart 请求包装成 MultipartHttpServletRequest
 * (控制器方法签名不含 MultipartFile 时可能跳过解析)，此时经
 * request.getPart 直读容器解析结果；MockMvc 的 Mock 请求不支持 getPart，
 * 由 Spring 包装优先兜住——两条路径按序回退</p>
 */
public class PartMultipartFile implements MultipartFile {

    private final Part part;

    public PartMultipartFile(Part part) {
        this.part = part;
    }

    /**
     * 从当前请求取字段名为 fieldName 的 multipart 文件：
     * 先走 Spring 包装(兼容 MockMvc)，再回退 Servlet Part(兼容生产)
     */
    @Nullable
    public static MultipartFile fromRequest(HttpServletRequest request, String fieldName)
            throws IOException, jakarta.servlet.ServletException {
        MultipartHttpServletRequest multipart =
                WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        if (multipart != null) {
            return multipart.getFile(fieldName);
        }
        Part part = request.getPart(fieldName);
        return part == null ? null : new PartMultipartFile(part);
    }

    @Override
    @NotNull
    public String getName() {
        return part.getName();
    }

    @Override
    public String getOriginalFilename() {
        return part.getSubmittedFileName();
    }

    @Override
    public String getContentType() {
        return part.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return part.getSize() == 0;
    }

    @Override
    public long getSize() {
        return part.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }

    @Override
    @NotNull
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    @Override
    public void transferTo(@NotNull File dest) throws IOException {
        Files.copy(getInputStream(), Path.of(dest.toURI()));
    }
}
