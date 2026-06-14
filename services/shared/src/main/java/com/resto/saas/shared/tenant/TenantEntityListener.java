package com.resto.saas.shared.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void setTenant(TenantAware entity) {
//        if (!TenantContext.isSet()) {
//            throw new IllegalStateException(
//                "TenantContext not set. Ensure X-Tenant-ID header is present."
//            );
//        }
        entity.setTenantId(TenantContext.getTenant());
    }
}
