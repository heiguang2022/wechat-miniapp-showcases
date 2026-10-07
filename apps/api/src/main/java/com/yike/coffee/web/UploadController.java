package com.yike.coffee.web;

import com.yike.coffee.api.ApiResponse;
import com.yike.coffee.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {
    private final FileStorageService storage;
    public UploadController(FileStorageService storage){this.storage=storage;}
    @PostMapping("/images")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_ADMIN')")
    ApiResponse<?> image(@RequestPart("file") MultipartFile file,HttpServletRequest request){return ApiResponse.ok(storage.store(file),request);}
}
