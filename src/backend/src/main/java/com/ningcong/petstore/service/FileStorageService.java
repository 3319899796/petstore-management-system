package com.ningcong.petstore.service;

import com.ningcong.petstore.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储：把上传的照片写到磁盘，返回可访问的相对路径。
 *
 * <p>设计要点（答辩可讲）：
 * 1. 只存路径不存二进制 —— 数据库不膨胀、备份轻；
 * 2. 文件名用 UUID —— 防止同名覆盖，也防止有人用 ../../ 做路径穿越；
 * 3. 白名单扩展名 + 大小上限 —— 防止上传可执行文件。</p>
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.url-prefix}")
    private String urlPrefix;

    /**
     * 保存文件并返回形如 /files/2026/11/xxxx.jpg 的访问路径。
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片不能超过 5MB");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extensionOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("只支持 jpg / jpeg / png / gif / webp 图片");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path dir = Paths.get(uploadDir, datePath).toAbsolutePath();
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }
        return urlPrefix + "/" + datePath + "/" + fileName;
    }

    /** 取上传根目录（供静态资源映射使用）。 */
    public Path rootPath() {
        return Paths.get(uploadDir).toAbsolutePath();
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
