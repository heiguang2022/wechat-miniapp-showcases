package com.yike.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yike.coffee.api.ApiException;
import com.yike.coffee.api.PageResult;
import com.yike.coffee.domain.DomainModels.Merchant;
import com.yike.coffee.domain.DomainEnums.MerchantStatus;
import com.yike.coffee.mapper.MerchantMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {
    private final MerchantMapper merchants;
    private final AuditService audit;
    public PlatformService(MerchantMapper merchants, AuditService audit) { this.merchants=merchants; this.audit=audit; }

    public PageResult<Merchant> list(long page, long pageSize, String status) {
        var query = Wrappers.<Merchant>query().orderByDesc("created_at");
        if (status != null && !status.isBlank()) query.eq("status", status);
        IPage<Merchant> result = merchants.selectPage(Page.of(page, Math.min(pageSize, 100)), query);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
    public Merchant get(String id) {
        Merchant value=merchants.selectById(id);
        if(value==null) throw new ApiException(HttpStatus.NOT_FOUND,"MERCHANT_NOT_FOUND","商家不存在");
        return value;
    }
    @Transactional public Merchant updateStatus(String id, MerchantStatus status, String reason) {
        Merchant merchant=get(id); merchant.status=status.name(); merchant.rejectReason=reason;
        merchants.updateById(merchant); audit.record("MERCHANT_"+status.name(),"MERCHANT",id,reason); return merchant;
    }
}
