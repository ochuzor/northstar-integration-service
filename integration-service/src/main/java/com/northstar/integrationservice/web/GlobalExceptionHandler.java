package com.northstar.integrationservice.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.northstar.integrationservice.application.audit.CustomerSyncAuditNotFoundException;
import com.northstar.integrationservice.domain.customer.CustomerValidationException;
import com.northstar.integrationservice.messaging.customer.CustomerSyncPublicationException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountNotFoundException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountRequestException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountResponseException;
import com.northstar.integrationservice.salesforce.account.SalesforceAccountUnavailableException;
import com.northstar.integrationservice.salesforce.oauth.SalesforceAuthenticationException;
import com.northstar.integrationservice.salesforce.oauth.SalesforceAuthenticationUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleInvalidAccountId() {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_ACCOUNT_ID", "Invalid Salesforce Account ID"));
    }

    @ExceptionHandler({SalesforceAccountNotFoundException.class})
    ResponseEntity<ApiError> handleAccountNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("ACCOUNT_NOT_FOUND", "Salesforce Account was not found"));
    }

    @ExceptionHandler(SalesforceAccountResponseException.class)
    ResponseEntity<ApiError> handleSalesforceAccountResponse() {
        return salesforceUpstreamError();
    }

    @ExceptionHandler(SalesforceAuthenticationException.class)
    ResponseEntity<ApiError> handleSalesforceAuthentication(
            SalesforceAuthenticationException exception) {

        int statusCode = exception.getStatusCode();

        if (statusCode == 429 || (statusCode >= 500 && statusCode <= 599)) {
            return salesforceUnavailable();
        }

        return salesforceUpstreamError();
    }

    @ExceptionHandler(SalesforceAccountRequestException.class)
    ResponseEntity<ApiError> handleSalesforceAccountRequest(
            SalesforceAccountRequestException exception) {

        if (exception.getStatusCode().value() == 429
                || exception.getStatusCode().is5xxServerError()) {
            return salesforceUnavailable();
        }

        return salesforceUpstreamError();
    }

    @ExceptionHandler({SalesforceAccountUnavailableException.class,
            SalesforceAuthenticationUnavailableException.class})
    ResponseEntity<ApiError> handleSalesforceUnavailable() {
        return salesforceUnavailable();
    }

    @ExceptionHandler({CustomerValidationException.class})
    ResponseEntity<ApiError> handleCustomerValidationException() {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ApiError(
                "CUSTOMER_VALIDATION_FAILED", "Salesforce Account cannot be synchronized"));
    }

    @ExceptionHandler({CustomerSyncPublicationException.class})
    ResponseEntity<ApiError> handleCustomerSyncPublicationException() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("SYNC_PUBLICATION_UNAVAILABLE",
                        "Customer synchronization is temporarily unavailable"));
    }

    @ExceptionHandler(CustomerSyncAuditNotFoundException.class)
    ResponseEntity<ApiError> handleCustomerSyncAuditNotFoundException() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("SYNC_AUDIT_NOT_FOUND", "Sync audit was not found"));
    }

    private ResponseEntity<ApiError> salesforceUpstreamError() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(
                "SALESFORCE_UPSTREAM_ERROR", "Salesforce returned an invalid response"));
    }

    private ResponseEntity<ApiError> salesforceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                new ApiError("SALESFORCE_UNAVAILABLE", "Salesforce is temporarily unavailable"));
    }
}
