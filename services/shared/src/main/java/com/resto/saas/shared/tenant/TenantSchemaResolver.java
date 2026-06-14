package com.resto.saas.shared.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TenantSchemaResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.isSet() ? TenantContext.getTenant() : "";
        log.info(">>> TenantSchemaResolver: TenantContext.isSet() = {}, tenant = '{}'", TenantContext.isSet(), tenant);
        return tenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
