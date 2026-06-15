package com.resto.saas.shared.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationEvent {

    private String tenantId;
    private String cacheKeyPattern;

}
