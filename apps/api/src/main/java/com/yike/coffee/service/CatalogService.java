package com.yike.coffee.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yike.coffee.api.ApiException;
import com.yike.coffee.domain.DomainModels.*;
import com.yike.coffee.domain.DomainEnums.MerchantStatus;
import com.yike.coffee.domain.DomainEnums.ProductStatus;
import com.yike.coffee.mapper.*;
import com.yike.coffee.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class CatalogService {
    private final MerchantMapper merchants; private final StoreMapper stores; private final CategoryMapper categories;
    private final ProductMapper products; private final SpecGroupMapper groups; private final SpecOptionMapper options;
    private final JdbcTemplate jdbc;
    public CatalogService(MerchantMapper merchants, StoreMapper stores, CategoryMapper categories, ProductMapper products,
                          SpecGroupMapper groups, SpecOptionMapper options, JdbcTemplate jdbc) {
        this.merchants=merchants;this.stores=stores;this.categories=categories;this.products=products;
        this.groups=groups;this.options=options;this.jdbc=jdbc;
    }

    public List<Merchant> publicMerchants(){return merchants.selectList(Wrappers.<Merchant>query().eq("status",MerchantStatus.APPROVED.name()).orderByAsc("name"));}
    public List<Store> publicStores(String merchantId){return stores.selectList(Wrappers.<Store>query().eq("merchant_id",merchantId).ne("status","DISABLED").orderByAsc("name"));}
    public Map<String,Object> menu(String storeId){
        Store store=stores.selectById(storeId);
        if(store==null||"DISABLED".equals(store.status)) throw new ApiException(HttpStatus.NOT_FOUND,"STORE_NOT_FOUND","门店不存在");
        Merchant merchant=merchants.selectById(store.merchantId);
        if(merchant==null||!MerchantStatus.APPROVED.name().equals(merchant.status)) throw new ApiException(HttpStatus.NOT_FOUND,"STORE_NOT_FOUND","门店不存在");
        List<Category> cats=categories.selectList(Wrappers.<Category>query().eq("merchant_id",store.merchantId).eq("enabled",true).orderByAsc("sort_order"));
        List<Product> ps=products.selectList(Wrappers.<Product>query().eq("merchant_id",store.merchantId).eq("status",ProductStatus.ON_SALE.name()).orderByAsc("sort_order"));
        List<Map<String,Object>> productViews=new ArrayList<>();
        for(Product p:ps){
            List<Map<String,Object>> specs=jdbc.queryForList("SELECT g.id groupId,g.name groupName,g.required_flag requiredFlag,o.id optionId,o.name optionName,o.price_delta priceDelta " +
                "FROM product_spec_group pg JOIN spec_group g ON g.id=pg.group_id JOIN spec_option o ON o.group_id=g.id AND o.enabled=TRUE WHERE pg.product_id=? ORDER BY g.name,o.sort_order",p.id);
            productViews.add(Map.of("id",p.id,"categoryId",p.categoryId,"name",p.name,"description",p.description==null?"":p.description,
                "basePrice",p.basePrice,"imageUrl",p.imageUrl==null?"":p.imageUrl,"specs",specs));
        }
        return Map.of("merchant",merchant,"store",store,"categories",cats,"products",productViews);
    }

    public List<Store> merchantStores(){return stores.selectList(Wrappers.<Store>query().eq("merchant_id",activeMerchantId()).orderByAsc("created_at"));}
    public Store saveStore(String id,String name,String address,String status,String hours){
        String merchantId=activeMerchantId(); Store s=id==null?new Store():ownedStore(id,merchantId);
        if(id==null){s.id=UUID.randomUUID().toString();s.merchantId=merchantId;s.deleted=false;}
        s.name=name;s.address=address;s.status=status;s.businessHours=hours;
        if(id==null) stores.insert(s); else stores.updateById(s); return s;
    }
    public List<Category> merchantCategories(){return categories.selectList(Wrappers.<Category>query().eq("merchant_id",activeMerchantId()).orderByAsc("sort_order"));}
    public Category saveCategory(String id,String name,Integer sortOrder,Boolean enabled){
        String m=activeMerchantId();Category c=id==null?new Category():ownedCategory(id,m);
        if(id==null){c.id=UUID.randomUUID().toString();c.merchantId=m;}c.name=name;c.sortOrder=sortOrder;c.enabled=enabled;
        if(id==null)categories.insert(c);else categories.updateById(c);return c;
    }
    public List<Product> merchantProducts(){return products.selectList(Wrappers.<Product>query().eq("merchant_id",activeMerchantId()).orderByAsc("sort_order"));}
    public Product saveProduct(String id,String categoryId,String name,String description,Long basePrice,String imageUrl,String status,Integer sortOrder){
        String m=activeMerchantId();ownedCategory(categoryId,m);Product p=id==null?new Product():ownedProduct(id,m);
        if(id==null){p.id=UUID.randomUUID().toString();p.merchantId=m;p.deleted=false;}p.categoryId=categoryId;p.name=name;p.description=description;
        p.basePrice=basePrice;p.imageUrl=imageUrl;p.status=status;p.sortOrder=sortOrder;if(id==null)products.insert(p);else products.updateById(p);return p;
    }
    public Product status(String id,String status){Product p=ownedProduct(id,activeMerchantId());p.status=status;products.updateById(p);return p;}

    @Transactional public Map<String,Object> createSpec(String name,Boolean required,List<SpecValue> values){
        String m=activeMerchantId();SpecGroup g=new SpecGroup();g.id=UUID.randomUUID().toString();g.merchantId=m;g.name=name;g.requiredFlag=required;groups.insert(g);
        List<SpecOption> created=new ArrayList<>();int sort=0;for(SpecValue v:values){SpecOption o=new SpecOption();o.id=UUID.randomUUID().toString();o.merchantId=m;o.groupId=g.id;o.name=v.name();o.priceDelta=v.priceDelta();o.enabled=true;o.sortOrder=sort+=10;options.insert(o);created.add(o);}return Map.of("group",g,"options",created);
    }
    public List<Map<String,Object>> specs(){return jdbc.queryForList("SELECT g.id,g.name,g.required_flag requiredFlag,o.id optionId,o.name optionName,o.price_delta priceDelta,o.enabled FROM spec_group g LEFT JOIN spec_option o ON o.group_id=g.id WHERE g.merchant_id=? ORDER BY g.created_at,o.sort_order",activeMerchantId());}
    public List<String> assignedSpecs(String productId){String m=activeMerchantId();ownedProduct(productId,m);return jdbc.query("SELECT group_id FROM product_spec_group WHERE product_id=? AND merchant_id=?",(r,n)->r.getString(1),productId,m);}
    @Transactional public void assignSpecs(String productId,List<String> groupIds){String m=activeMerchantId();ownedProduct(productId,m);jdbc.update("DELETE FROM product_spec_group WHERE product_id=? AND merchant_id=?",productId,m);for(String gid:groupIds){SpecGroup g=groups.selectById(gid);if(g==null||!m.equals(g.merchantId))throw forbidden();jdbc.update("INSERT INTO product_spec_group(product_id,group_id,merchant_id) VALUES(?,?,?)",productId,gid,m);}}

    public record SpecValue(String name,Long priceDelta){}
    private Store ownedStore(String id,String m){Store x=stores.selectById(id);if(x==null||!m.equals(x.merchantId))throw forbidden();return x;}
    private Category ownedCategory(String id,String m){Category x=categories.selectById(id);if(x==null||!m.equals(x.merchantId))throw forbidden();return x;}
    private Product ownedProduct(String id,String m){Product x=products.selectById(id);if(x==null||!m.equals(x.merchantId))throw forbidden();return x;}
    private String activeMerchantId(){
        String id=CurrentUser.merchantId();Merchant merchant=merchants.selectById(id);
        // 商家被停用或尚未审核时，服务端统一禁止经营写入与查询。
        if(merchant==null||!MerchantStatus.APPROVED.name().equals(merchant.status))throw new ApiException(HttpStatus.FORBIDDEN,"MERCHANT_NOT_APPROVED","商家尚未审核通过或已停用");
        return id;
    }
    private ApiException forbidden(){return new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","资源不存在");}
}
