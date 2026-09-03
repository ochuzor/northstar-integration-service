package com.northstar.mockerp.persistence.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "erp_customers")
public class ErpCustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_customer_id", nullable = false, length = 18)
    private String sourceCustomerId;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(nullable = false)
    private String name;

    @Column(name = "billing_city")
    private String billingCity;

    protected ErpCustomerEntity() {
    }

    public ErpCustomerEntity(String sourceCustomerId, String businessId, String name,
            String billingCity) {
        this.sourceCustomerId = sourceCustomerId;
        this.businessId = businessId;
        this.name = name;
        this.billingCity = billingCity;
    }

    public Long getId() {
        return id;
    }

    public String getSourceCustomerId() {
        return sourceCustomerId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public String getBillingCity() {
        return billingCity;
    }
}
