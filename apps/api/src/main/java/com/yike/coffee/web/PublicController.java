package com.yike.coffee.web;

import com.yike.coffee.api.ApiResponse;
import com.yike.coffee.service.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
public class PublicController {
    private final CatalogService service;
    public PublicController(CatalogService service){this.service=service;}
    @GetMapping("/merchants") ApiResponse<?> merchants(HttpServletRequest r){return ApiResponse.ok(service.publicMerchants(),r);}
    @GetMapping("/stores") ApiResponse<?> stores(@RequestParam String merchantId,HttpServletRequest r){return ApiResponse.ok(service.publicStores(merchantId),r);}
    @GetMapping("/stores/{id}/menu") ApiResponse<?> menu(@PathVariable String id,HttpServletRequest r){return ApiResponse.ok(service.menu(id),r);}
}
