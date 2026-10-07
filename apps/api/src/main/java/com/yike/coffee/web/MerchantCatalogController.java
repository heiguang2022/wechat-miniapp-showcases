package com.yike.coffee.web;

import com.yike.coffee.api.ApiResponse;
import com.yike.coffee.service.CatalogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchant")
@PreAuthorize("hasRole('MERCHANT_ADMIN')")
public class MerchantCatalogController {
    private final CatalogService service;
    public MerchantCatalogController(CatalogService service){this.service=service;}

    public record StoreBody(@NotBlank String name,@NotBlank String address,@Pattern(regexp="OPEN|CLOSED|DISABLED") String status,@NotBlank String businessHours){}
    public record CategoryBody(@NotBlank String name,@NotNull @Min(0) Integer sortOrder,@NotNull Boolean enabled){}
    public record ProductBody(@NotBlank String categoryId,@NotBlank String name,String description,@NotNull @Min(0) Long basePrice,
                              String imageUrl,@Pattern(regexp="DRAFT|ON_SALE|OFF_SALE") String status,@NotNull @Min(0) Integer sortOrder){}
    public record StatusBody(@Pattern(regexp="DRAFT|ON_SALE|OFF_SALE") String status){}
    public record SpecValue(@NotBlank String name,@NotNull @Min(0) Long priceDelta){}
    public record SpecBody(@NotBlank String name,@NotNull Boolean required,@NotEmpty List<@Valid SpecValue> values){}
    public record Assignment(@NotNull List<String> groupIds){}

    @GetMapping("/stores") ApiResponse<?> stores(HttpServletRequest r){return ApiResponse.ok(service.merchantStores(),r);}
    @PostMapping("/stores") ApiResponse<?> createStore(@Valid @RequestBody StoreBody b,HttpServletRequest r){return ApiResponse.ok(service.saveStore(null,b.name(),b.address(),b.status(),b.businessHours()),r);}
    @PatchMapping("/stores/{id}") ApiResponse<?> updateStore(@PathVariable String id,@Valid @RequestBody StoreBody b,HttpServletRequest r){return ApiResponse.ok(service.saveStore(id,b.name(),b.address(),b.status(),b.businessHours()),r);}
    @GetMapping("/categories") ApiResponse<?> categories(HttpServletRequest r){return ApiResponse.ok(service.merchantCategories(),r);}
    @PostMapping("/categories") ApiResponse<?> createCategory(@Valid @RequestBody CategoryBody b,HttpServletRequest r){return ApiResponse.ok(service.saveCategory(null,b.name(),b.sortOrder(),b.enabled()),r);}
    @PatchMapping("/categories/{id}") ApiResponse<?> updateCategory(@PathVariable String id,@Valid @RequestBody CategoryBody b,HttpServletRequest r){return ApiResponse.ok(service.saveCategory(id,b.name(),b.sortOrder(),b.enabled()),r);}
    @GetMapping("/products") ApiResponse<?> products(HttpServletRequest r){return ApiResponse.ok(service.merchantProducts(),r);}
    @PostMapping("/products") ApiResponse<?> createProduct(@Valid @RequestBody ProductBody b,HttpServletRequest r){return ApiResponse.ok(service.saveProduct(null,b.categoryId(),b.name(),b.description(),b.basePrice(),b.imageUrl(),b.status(),b.sortOrder()),r);}
    @PatchMapping("/products/{id}") ApiResponse<?> updateProduct(@PathVariable String id,@Valid @RequestBody ProductBody b,HttpServletRequest r){return ApiResponse.ok(service.saveProduct(id,b.categoryId(),b.name(),b.description(),b.basePrice(),b.imageUrl(),b.status(),b.sortOrder()),r);}
    @PostMapping("/products/{id}/status") ApiResponse<?> status(@PathVariable String id,@Valid @RequestBody StatusBody b,HttpServletRequest r){return ApiResponse.ok(service.status(id,b.status()),r);}
    @GetMapping("/spec-groups") ApiResponse<?> specs(HttpServletRequest r){return ApiResponse.ok(service.specs(),r);}
    @PostMapping("/spec-groups") ApiResponse<?> createSpec(@Valid @RequestBody SpecBody b,HttpServletRequest r){var values=b.values().stream().map(v->new CatalogService.SpecValue(v.name(),v.priceDelta())).toList();return ApiResponse.ok(service.createSpec(b.name(),b.required(),values),r);}
    @PutMapping("/products/{id}/spec-groups") ApiResponse<?> assign(@PathVariable String id,@Valid @RequestBody Assignment b,HttpServletRequest r){service.assignSpecs(id,b.groupIds());return ApiResponse.ok(Map.of("updated",true),r);}
    @GetMapping("/products/{id}/spec-groups") ApiResponse<?> assigned(@PathVariable String id,HttpServletRequest r){return ApiResponse.ok(service.assignedSpecs(id),r);}
}
