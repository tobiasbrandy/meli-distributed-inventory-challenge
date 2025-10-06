package com.tobiasbrandy.meli.inventory.central.config;

import com.tobiasbrandy.meli.inventory.exceptions.*;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static ProblemDetail errorResponse(final HttpStatus status, final String title, final Exception e) {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problemDetail.setTitle(title);
        return problemDetail;
    }

    @ExceptionHandler(InvalidStoreIdException.class)
    public ProblemDetail invalidStoreId(final InvalidStoreIdException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Invalid Store ID", e);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail insufficientStock(final InsufficientStockException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Insufficient Stock", e);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail productNotFound(final ProductNotFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, "Product Not Found", e);
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ProblemDetail productAlreadyExists(final ProductAlreadyExistsException e) {
        return errorResponse(HttpStatus.CONFLICT, "Product Already Exists", e);
    }

    @ExceptionHandler(StoreUnavailableException.class)
    public ProblemDetail storeUnavailable(final StoreUnavailableException e) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Store Unavailable", e);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail methodNotSupported(final HttpRequestMethodNotSupportedException e) {
        return e.getBody();
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail internalServerError(final Exception e) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", e);
    }
}
