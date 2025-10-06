package com.tobiasbrandy.meli.inventory.store.config;

import com.tobiasbrandy.meli.inventory.exceptions.InsufficientStockException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductAlreadyExistsException;
import com.tobiasbrandy.meli.inventory.exceptions.ProductNotFoundException;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static ProblemDetail errorResponse(final HttpStatus status, final String title, final Exception e) {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problemDetail.setTitle(title);
        return problemDetail;
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

    @ExceptionHandler(Exception.class)
    public ProblemDetail fallbackHandler(final Exception e) {
        if (e instanceof ErrorResponse) {
            return ((ErrorResponse) e).getBody();
        }
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", e);
    }
}
