package com.yike.coffee.web;

import com.yike.coffee.api.ApiResponse;
import com.yike.coffee.domain.DomainEnums.OrderStatus;
import com.yike.coffee.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service){this.service=service;}
    public record Line(@NotBlank String productId,@Min(1) @Max(99) int quantity,List<String> optionIds){}
    public record Create(@NotBlank String storeId,@NotEmpty List<@Valid Line> items){}

    @PostMapping("/api/v1/customer/orders") @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<?> create(@Valid @RequestBody Create body,@RequestHeader(value="X-Idempotency-Key",required=false) String key,HttpServletRequest r){
        return ApiResponse.ok(service.create(new OrderService.CreateOrder(body.storeId(),body.items().stream().map(i->new OrderService.Line(i.productId(),i.quantity(),i.optionIds())).toList()),key),r);}
    @GetMapping("/api/v1/customer/orders") @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<?> customerList(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long pageSize,HttpServletRequest r){return ApiResponse.ok(service.customerOrders(page,pageSize),r);}
    @GetMapping("/api/v1/customer/orders/{id}") @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<?> customerGet(@PathVariable String id,HttpServletRequest r){return ApiResponse.ok(service.customerDetail(id),r);}
    @PostMapping("/api/v1/customer/orders/{id}/cancel") @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<?> cancel(@PathVariable String id,HttpServletRequest r){return ApiResponse.ok(service.cancel(id),r);}

    @GetMapping("/api/v1/merchant/orders") @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    ApiResponse<?> merchantList(@RequestParam(defaultValue="1") long page,@RequestParam(defaultValue="20") long pageSize,HttpServletRequest r){return ApiResponse.ok(service.merchantOrders(page,pageSize),r);}
    @GetMapping("/api/v1/merchant/orders/{id}") @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    ApiResponse<?> merchantGet(@PathVariable String id,HttpServletRequest r){return ApiResponse.ok(service.merchantDetail(id),r);}
    @PostMapping("/api/v1/merchant/orders/{id}/{action:accept|reject|ready|complete}") @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    ApiResponse<?> transition(@PathVariable String id,@PathVariable String action,HttpServletRequest r){OrderStatus target=switch(action){case"accept"->OrderStatus.PREPARING;case"reject"->OrderStatus.REJECTED;case"ready"->OrderStatus.READY;case"complete"->OrderStatus.COMPLETED;default->throw new IllegalArgumentException();};return ApiResponse.ok(service.merchantTransition(id,target),r);}
}
