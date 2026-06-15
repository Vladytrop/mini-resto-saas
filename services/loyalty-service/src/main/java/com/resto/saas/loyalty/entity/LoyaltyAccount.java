package com.resto.saas.loyalty.entity;

import com.resto.saas.shared.tenant.TenantAware;
import com.resto.saas.shared.tenant.TenantEntityListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "loyalty_accounts")
@EntityListeners(TenantEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class LoyaltyAccount implements TenantAware {

    @Id
    @UuidGenerator
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
