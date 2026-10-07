package com.yike.coffee.service;

import com.yike.coffee.api.ApiException;
import com.yike.coffee.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class LocalFileStorageService implements FileStorageService {
    private final Path root; private final String baseUrl; private final JdbcTemplate jdbc;
    public LocalFileStorageService(@Value("${app.upload-dir}") String root,@Value("${app.public-base-url}") String baseUrl,JdbcTemplate jdbc){
        this.root=Path.of(root).toAbsolutePath().normalize();this.baseUrl=baseUrl.replaceAll("/$","");this.jdbc=jdbc;
    }
    @Override public StoredFile store(MultipartFile file){
        if(file.isEmpty()||file.getSize()>5L*1024*1024)throw bad("图片不能为空且不能超过 5MB");
        try{
            byte[] head=file.getInputStream().readNBytes(16);String type=detect(head);String ext=switch(type){case"image/jpeg"->".jpg";case"image/png"->".png";default->".webp";};
            String supplied=file.getContentType();if(supplied==null||!supplied.equals(type))throw bad("图片内容与 MIME 类型不一致");
            Files.createDirectories(root);String stored=UUID.randomUUID()+ext;Path target=root.resolve(stored).normalize();
            if(!target.startsWith(root))throw bad("非法文件路径");file.transferTo(target);
            String id=UUID.randomUUID().toString();String url=baseUrl+"/uploads/"+stored;var user=CurrentUser.required();
            jdbc.update("INSERT INTO upload_file(id,merchant_id,original_name,stored_name,content_type,size_bytes,public_url,created_by) VALUES(?,?,?,?,?,?,?,?)",
                id,user.merchantId(),Optional.ofNullable(file.getOriginalFilename()).orElse("image"),stored,type,file.getSize(),url,user.userId());
            return new StoredFile(id,url,type,file.getSize());
        }catch(IOException ex){throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"UPLOAD_FAILED","图片保存失败");}
    }
    private String detect(byte[] b){
        if(b.length>=3&&(b[0]&255)==0xff&&(b[1]&255)==0xd8&&(b[2]&255)==0xff)return"image/jpeg";
        if(b.length>=8&&(b[0]&255)==0x89&&b[1]=='P'&&b[2]=='N'&&b[3]=='G')return"image/png";
        if(b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P')return"image/webp";
        throw bad("仅支持 JPEG、PNG 和 WebP 图片");
    }
    private ApiException bad(String message){return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_IMAGE",message);}
}
