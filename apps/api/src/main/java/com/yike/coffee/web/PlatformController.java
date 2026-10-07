package com.yike.coffee.web;

import com.yike.coffee.api.ApiResponse;
import com.yike.coffee.domain.DomainEnums.MerchantStatus;
import com.yike.coffee.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform/merchants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformController {
    private final PlatformService service;
    public PlatformController(PlatformService service){this.service=service;}
    public record Decision(String reason){}
    @GetMapping ApiResponse<?> list(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long pageSize,
                                    @RequestParam(required=false) String status,HttpServletRequest request){return ApiResponse.ok(service.list(page,pageSize,status),request);}
    @GetMapping("/{id}") ApiResponse<?> get(@PathVariable String id,HttpServletRequest request){return ApiResponse.ok(service.get(id),request);}
    @PostMapping("/{id}/approve") ApiResponse<?> approve(@PathVariable String id,HttpServletRequest r){return ApiResponse.ok(service.updateStatus(id,MerchantStatus.APPROVED,null),r);}
    @PostMapping("/{id}/reject") ApiResponse<?> reject(@PathVariable String id,@RequestBody Decision body,HttpServletRequest r){return ApiResponse.ok(service.updateStatus(id,MerchantStatus.REJECTED,body.reason()),r);}
    @PostMapping("/{id}/suspend") ApiResponse<?> suspend(@PathVariable String id,@RequestBody(required=false) Decision body,HttpServletRequest r){return ApiResponse.ok(service.updateStatus(id,MerchantStatus.SUSPENDED,body==null?null:body.reason()),r);}
}
