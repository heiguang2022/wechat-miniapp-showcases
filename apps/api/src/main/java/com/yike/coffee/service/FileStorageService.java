package com.yike.coffee.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(MultipartFile file);
    record StoredFile(String id,String url,String contentType,long size){}
}
