package com.yike.coffee.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yike.coffee.api.ApiException;
import com.yike.coffee.api.PageResult;
import com.yike.coffee.domain.DomainModels.*;
import com.yike.coffee.domain.DomainEnums.*;
import com.yike.coffee.mapper.*;
import com.yike.coffee.security.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {
    private final OrderMapper orders; private final ProductMapper products; private final StoreMapper stores;
    private final MerchantMapper merchants; private final JdbcTemplate jdbc;
    public OrderService(OrderMapper orders,ProductMapper products,StoreMapper stores,MerchantMapper merchants,JdbcTemplate jdbc){
        this.orders=orders;this.products=products;this.stores=stores;this.merchants=merchants;this.jdbc=jdbc;
    }

    public record Line(String productId,int quantity,List<String> optionIds){}
    public record CreateOrder(String storeId,List<Line> items){}

    @Transactional
    public Map<String,Object> create(CreateOrder request,String idempotencyKey){
        String customer=CurrentUser.required().userId();
        if(idempotencyKey==null||idempotencyKey.isBlank()||idempotencyKey.length()>80)
            throw new ApiException(HttpStatus.BAD_REQUEST,"IDEMPOTENCY_REQUIRED","请提供有效的 X-Idempotency-Key");
        List<String> existing=jdbc.query("SELECT id FROM orders WHERE customer_id=? AND idempotency_key=?",(r,n)->r.getString(1),customer,idempotencyKey);
        if(!existing.isEmpty()) return detail(existing.get(0),customer,false);
        Store store=stores.selectById(request.storeId());
        if(store==null||!StoreStatus.OPEN.name().equals(store.status)) throw bad("STORE_UNAVAILABLE","门店当前不可下单");
        Merchant merchant=merchants.selectById(store.merchantId);
        if(merchant==null||!MerchantStatus.APPROVED.name().equals(merchant.status)) throw bad("MERCHANT_UNAVAILABLE","商家当前不可下单");
        if(request.items()==null||request.items().isEmpty()) throw bad("EMPTY_ORDER","订单中没有商品");

        record Priced(Line line,Product product,long unit,List<Map<String,Object>> selected){}
        List<Priced> priced=new ArrayList<>();long total=0;int count=0;
        for(Line line:request.items()){
            if(line.quantity()<1||line.quantity()>99) throw bad("INVALID_QUANTITY","商品数量必须为 1-99");
            Product product=products.selectById(line.productId());
            if(product==null||!store.merchantId.equals(product.merchantId)||!ProductStatus.ON_SALE.name().equals(product.status))
                throw bad("PRODUCT_UNAVAILABLE","商品已下架或不属于当前门店");
            List<Map<String,Object>> available=jdbc.queryForList("SELECT o.id optionId,o.group_id groupId,o.name optionName,o.price_delta priceDelta,g.name groupName,g.required_flag requiredFlag " +
                "FROM product_spec_group pg JOIN spec_group g ON g.id=pg.group_id JOIN spec_option o ON o.group_id=g.id AND o.enabled=TRUE WHERE pg.product_id=? AND pg.merchant_id=?",product.id,store.merchantId);
            Map<String,Map<String,Object>> byId=new HashMap<>();Set<String> requiredGroups=new HashSet<>();
            for(var option:available){byId.put(String.valueOf(option.get("optionId")),option);if(Boolean.TRUE.equals(option.get("requiredFlag")))requiredGroups.add(String.valueOf(option.get("groupId")));}
            List<Map<String,Object>> selected=new ArrayList<>();Set<String> selectedGroups=new HashSet<>();long delta=0;
            for(String optionId:line.optionIds()==null?List.<String>of():line.optionIds()){
                Map<String,Object> option=byId.get(optionId);if(option==null)throw bad("INVALID_OPTION","商品规格无效");
                String group=String.valueOf(option.get("groupId"));if(!selectedGroups.add(group))throw bad("DUPLICATE_OPTION","同一规格组只能选择一项");
                delta+=((Number)option.get("priceDelta")).longValue();selected.add(option);
            }
            if(!selectedGroups.containsAll(requiredGroups))throw bad("REQUIRED_OPTION_MISSING","请选择完整的必选规格");
            long unit=Math.addExact(product.basePrice,delta);long lineAmount=Math.multiplyExact(unit,line.quantity());
            total=Math.addExact(total,lineAmount);count=Math.addExact(count,line.quantity());priced.add(new Priced(line,product,unit,selected));
        }
        String orderId=UUID.randomUUID().toString();String orderNo="YK"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+ThreadLocalRandom.current().nextInt(1000,9999);
        try{jdbc.update("INSERT INTO orders(id,order_no,idempotency_key,merchant_id,store_id,customer_id,status,total_amount,item_count) VALUES(?,?,?,?,?,?,?,?,?)",
            orderId,orderNo,idempotencyKey,store.merchantId,store.id,customer,OrderStatus.PENDING_ACCEPTANCE.name(),total,count);}catch(DuplicateKeyException ex){
            String id=jdbc.queryForObject("SELECT id FROM orders WHERE customer_id=? AND idempotency_key=?",String.class,customer,idempotencyKey);return detail(id,customer,false);
        }
        for(Priced value:priced){String itemId=UUID.randomUUID().toString();long amount=value.unit()*value.line().quantity();
            jdbc.update("INSERT INTO order_item(id,order_id,product_id,product_name,unit_price,quantity,line_amount) VALUES(?,?,?,?,?,?,?)",itemId,orderId,value.product().id,value.product().name,value.unit(),value.line().quantity(),amount);
            for(var option:value.selected())jdbc.update("INSERT INTO order_item_option(id,order_item_id,option_id,group_name,option_name,price_delta) VALUES(?,?,?,?,?,?)",UUID.randomUUID().toString(),itemId,option.get("optionId"),option.get("groupName"),option.get("optionName"),option.get("priceDelta"));
        }
        log(orderId,customer,null,OrderStatus.PENDING_ACCEPTANCE.name());return detail(orderId,customer,false);
    }

    public PageResult<Map<String,Object>> customerOrders(long page,long pageSize){return list("customer_id",CurrentUser.required().userId(),page,pageSize);}
    public PageResult<Map<String,Object>> merchantOrders(long page,long pageSize){ensureMerchantActive();return list("merchant_id",CurrentUser.merchantId(),page,pageSize);}
    public Map<String,Object> customerDetail(String id){return detail(id,CurrentUser.required().userId(),false);}
    public Map<String,Object> merchantDetail(String id){ensureMerchantActive();return detail(id,CurrentUser.merchantId(),true);}

    @Transactional public Map<String,Object> cancel(String id){return transition(id,false,OrderStatus.PENDING_ACCEPTANCE,OrderStatus.CANCELLED);}
    @Transactional public Map<String,Object> merchantTransition(String id,OrderStatus target){
        ensureMerchantActive();OrderStatus expected=switch(target){case PREPARING->OrderStatus.PENDING_ACCEPTANCE;case REJECTED->OrderStatus.PENDING_ACCEPTANCE;case READY->OrderStatus.PREPARING;case COMPLETED->OrderStatus.READY;default->throw bad("INVALID_STATUS","不支持的订单状态");};
        return transition(id,true,expected,target);
    }

    private Map<String,Object> transition(String id,boolean merchantScope,OrderStatus expected,OrderStatus target){
        String owner=merchantScope?CurrentUser.merchantId():CurrentUser.required().userId();String column=merchantScope?"merchant_id":"customer_id";
        int changed=jdbc.update("UPDATE orders SET status=?,version=version+1 WHERE id=? AND "+column+"=? AND status=?",target.name(),id,owner,expected.name());
        if(changed==0)throw new ApiException(HttpStatus.CONFLICT,"ORDER_STATE_CONFLICT","订单状态已变化或无权操作");
        log(id,CurrentUser.required().userId(),expected.name(),target.name());return detail(id,owner,merchantScope);
    }

    private PageResult<Map<String,Object>> list(String ownerColumn,String owner,long page,long pageSize){long size=Math.min(Math.max(pageSize,1),100);long current=Math.max(page,1);long offset=(current-1)*size;
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE "+ownerColumn+"=?",Long.class,owner);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,order_no orderNo,store_id storeId,status,total_amount totalAmount,item_count itemCount,payment_status paymentStatus,created_at createdAt FROM orders WHERE "+ownerColumn+"=? ORDER BY created_at DESC LIMIT ? OFFSET ?",owner,size,offset);
        return new PageResult<>(rows,total,current,size);
    }
    private Map<String,Object> detail(String id,String owner,boolean merchantScope){String column=merchantScope?"merchant_id":"customer_id";
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,order_no orderNo,merchant_id merchantId,store_id storeId,customer_id customerId,status,total_amount totalAmount,item_count itemCount,payment_status paymentStatus,payment_provider paymentProvider,version,created_at createdAt,updated_at updatedAt FROM orders WHERE id=? AND "+column+"=?",id,owner);
        if(rows.isEmpty())throw new ApiException(HttpStatus.NOT_FOUND,"ORDER_NOT_FOUND","订单不存在");Map<String,Object> result=new LinkedHashMap<>(rows.get(0));
        List<Map<String,Object>> items=jdbc.queryForList("SELECT id,product_id productId,product_name productName,unit_price unitPrice,quantity,line_amount lineAmount FROM order_item WHERE order_id=?",id);
        for(var item:items)item.put("options",jdbc.queryForList("SELECT group_name groupName,option_name optionName,price_delta priceDelta FROM order_item_option WHERE order_item_id=?",item.get("id")));
        result.put("items",items);result.put("statusLogs",jdbc.queryForList("SELECT from_status fromStatus,to_status toStatus,created_at createdAt FROM order_status_log WHERE order_id=? ORDER BY created_at",id));return result;
    }
    private void ensureMerchantActive(){Merchant m=merchants.selectById(CurrentUser.merchantId());if(m==null||!MerchantStatus.APPROVED.name().equals(m.status))throw new ApiException(HttpStatus.FORBIDDEN,"MERCHANT_NOT_APPROVED","商家尚未审核通过或已停用");}
    private void log(String order,String operator,String from,String to){jdbc.update("INSERT INTO order_status_log(id,order_id,operator_id,from_status,to_status) VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),order,operator,from,to);}
    private ApiException bad(String code,String message){return new ApiException(HttpStatus.BAD_REQUEST,code,message);}
}
