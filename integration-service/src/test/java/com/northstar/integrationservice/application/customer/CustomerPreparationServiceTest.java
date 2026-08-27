package com.northstar.integrationservice.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.integrationservice.application.account.SalesforceAccountResult;
import com.northstar.integrationservice.application.account.SalesforceAccountService;
import com.northstar.integrationservice.domain.customer.Customer;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class CustomerPreparationServiceTest {
    @Mock
    private SalesforceAccountService accountService;
    private final SalesforceAccountToCustomerMapper mapper = new SalesforceAccountToCustomerMapper();

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final CustomerValidator customerValidator = new CustomerValidator(validator);

    private CustomerPreparationService service;

    @BeforeEach
    void setUp() {
        service = new CustomerPreparationService(accountService, mapper, customerValidator);
    }

    @Test
    void preparesValidCustomerForSynchronization() {
        String salesforceAccountId = "test-id";
        when(accountService.fetchAccount(salesforceAccountId)).thenReturn(
                new SalesforceAccountResult(salesforceAccountId, "Biz Name", "Biz-Id", "London"));

        Customer customer = service.prepareCustomer(salesforceAccountId);

        assertThat(customer.sourceCustomerId()).isEqualTo(salesforceAccountId);
        assertThat(customer.name()).isEqualTo("Biz Name");
        assertThat(customer.businessId()).isEqualTo("Biz-Id");
        assertThat(customer.billingCity()).isEqualTo("London");

        verify(accountService).fetchAccount(salesforceAccountId);
    }

    @Test
    void rejectsInvalidMappedCustomer() {
        String salesforceAccountId = "test-id";

        when(accountService.fetchAccount(salesforceAccountId)).thenReturn(
                new SalesforceAccountResult(salesforceAccountId, "Business Name", "   ", "London"));

        assertThatThrownBy(() -> service.prepareCustomer(salesforceAccountId))
                .isInstanceOfSatisfying(CustomerValidationException.class, exception -> {
                    assertThat(exception.getInvalidFields()).containsExactly("businessId");
                    assertThat(exception).hasMessage("Customer validation failed");
                });

        verify(accountService).fetchAccount(salesforceAccountId);
    }
}
