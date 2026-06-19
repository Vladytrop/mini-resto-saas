package com.resto.saas.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueDto {

    private String tenantId;
    private BigDecimal totalRevenue;
    private Long orderCount;
}
