package com.resto.saas.analytics.controller;

import com.resto.saas.analytics.dto.CustomerDto;
import com.resto.saas.analytics.dto.RevenueDto;
import com.resto.saas.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/top-customers")
    public List<CustomerDto> getTopCustomers() {

        return analyticsService.getTopCustomers();
    }

    @GetMapping("/revenue-by-tenant")
    public List<RevenueDto> getRevenueByTenant() {

        return analyticsService.getRevenueByTenant();
    }
}
