package com.northstar.mockerp.persistence.customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ErpCustomerRepository extends JpaRepository<ErpCustomerEntity, Long> {

    Optional<ErpCustomerEntity> findBySourceCustomerId(String sourceCustomerId);
}
