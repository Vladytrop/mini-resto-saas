package com.resto.saas.analytics.service;

import com.resto.saas.analytics.dto.CustomerDto;
import com.resto.saas.analytics.dto.RevenueDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<CustomerDto> getTopCustomers() {

        String sql = """
                select tenant_id, customer_name, points,
                    row number() over (order by points desc) as rank
                from loyalty_accounts
                order by points desc
                lomit 10
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> results =
                entityManager.createNativeQuery(sql).getResultList();

        return results.stream()
                .map(row -> CustomerDto.builder()
                        .tenantId((String) row[0])
                        .customerName((String) row[1])
                        .totalPoints((Integer) row[2])
                        .rank((Integer) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    public List<RevenueDto> getRevenueByTenant() {

        String sql = """
            SELECT tenant_id, SUM(total_amount), COUNT(*)
            FROM orders
            GROUP BY tenant_id
            ORDER BY SUM(total_amount) DESC
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        return results.stream()
                .map(row -> RevenueDto.builder()
                        .tenantId((String) row[0])
                        .totalRevenue((BigDecimal) row[1])
                        .orderCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }
}
