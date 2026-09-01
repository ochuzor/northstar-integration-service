package com.northstar.mockerp.application.customer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.northstar.mockerp.domain.customer.ErpCustomer;
import com.northstar.mockerp.domain.customer.ErpCustomerValidationException;
import com.northstar.mockerp.messaging.customer.CustomerSyncPayload;
import com.northstar.mockerp.messaging.customer.CustomerSyncRequestedEvent;

class CustomerSyncEventHandlerTest {
    @Test
    void mapsAndValidatesCustomerBeforeAcceptingEvent() {
        CustomerSyncPayloadToErpCustomerMapper mapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(mapper, validator);
        CustomerSyncPayload payload = new CustomerSyncPayload("customer-id", "business-id",
                "Customer Name", "Helsinki");
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);
        ErpCustomer customer = new ErpCustomer("customer-id", "business-id", "Customer Name",
                "Helsinki");
        when(mapper.map(payload)).thenReturn(customer);
        when(validator.validate(customer)).thenReturn(customer);

        handler.handle("business-id", event);

        InOrder processingOrder = inOrder(mapper, validator);
        processingOrder.verify(mapper).map(payload);
        processingOrder.verify(validator).validate(customer);
    }

    @Test
    void propagatesErpCustomerValidationFailure() {
        CustomerSyncPayloadToErpCustomerMapper mapper = mock(
                CustomerSyncPayloadToErpCustomerMapper.class);
        ErpCustomerValidator validator = mock(ErpCustomerValidator.class);
        CustomerSyncEventHandler handler = new CustomerSyncEventHandler(mapper, validator);
        CustomerSyncPayload payload = new CustomerSyncPayload("customer-id", null, "Customer Name",
                null);
        CustomerSyncRequestedEvent event = new CustomerSyncRequestedEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                Instant.parse("2026-08-28T10:15:30Z"), 1, "CUSTOMER_SYNC_REQUESTED", "SALESFORCE",
                payload);
        ErpCustomer customer = new ErpCustomer("customer-id", null, "Customer Name", null);
        ErpCustomerValidationException validationFailure = new ErpCustomerValidationException(
                Set.of("businessId"));
        when(mapper.map(payload)).thenReturn(customer);
        when(validator.validate(customer)).thenThrow(validationFailure);

        assertThatThrownBy(() -> handler.handle(null, event)).isSameAs(validationFailure);
    }
}
