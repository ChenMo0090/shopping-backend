package com.shop.common.controller;

import com.shop.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${shop.upload.path:uploads}")
    private String uploadPath;

    @Value("${shop.upload.url-prefix:}")
    private String urlPrefix;

    @Operation(summary = "上传图片")
    @PostMapping
    public R<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        // 校验类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return R.error("只允许上传图片文件");
        }

        // 按日期分目录存储
        String dateDir = LocalDate.now().toString().replace("-", "/");
        String ext = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保目录存在
        File dir = new File(uploadPath + "/" + dateDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, fileName);
        file.transferTo(dest);

        // 构建访问 URL
        String relativePath = "/uploads/" + dateDir + "/" + fileName;
        String url = (urlPrefix != null && !urlPrefix.isBlank())
                ? urlPrefix + relativePath
                : request.getScheme() + "://" + request.getServerName()
                  + ":" + request.getServerPort() + relativePath;

        return R.ok(Map.of("url", url));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
