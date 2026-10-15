package com.rvmc.ordertaker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    ProblemDetail invalidCredentials() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(AuthConflictException.class)
    ProblemDetail accountConflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An account with this email already exists");
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            AuthService.InvalidPasswordException.class
    })
    ProblemDetail invalidRequest(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request contains invalid fields");
    }
}
