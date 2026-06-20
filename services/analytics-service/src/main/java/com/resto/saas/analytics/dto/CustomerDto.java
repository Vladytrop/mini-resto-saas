package com.resto.saas.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerDto {

    private String tenantId;
    private String customerName;
    private Long totalPoints;
    private Long rank;
}
