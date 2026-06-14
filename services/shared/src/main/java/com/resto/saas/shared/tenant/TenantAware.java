package com.resto.saas.shared.tenant;

public interface TenantAware {
    void setTenantId(String tenantId);
    String getTenantId();
}
