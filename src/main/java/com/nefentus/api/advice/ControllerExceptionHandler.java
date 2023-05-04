package com.nefentus.api.advice;

import com.nefentus.api.Errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleInvalidArgument(MethodArgumentNotValidException ex) {
        Map<String, String> errorMap = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errorMap.put(error.getField(), error.getDefaultMessage());
        });
        return errorMap;
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST; // default to 400
        if (ex instanceof AccessDeniedException) {
            httpStatus = HttpStatus.UNAUTHORIZED; // set status to 401
        }
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                httpStatus,
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, httpStatus);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleBusinessException(AuthenticationException ex) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                httpStatus,
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, httpStatus);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Object> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                httpStatus,
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, httpStatus);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleExceptionBadRequest(BadRequestException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<Object> handleExceptionEmail(EmailSendException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }


    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<Object> handleExceptionInactiveAccount(InactiveUserException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<Object> handleExceptionIncorrectPassword(IncorrectPasswordException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<Object> handleInternalServerErrorException(InternalServerException ex) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                httpStatus,
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, httpStatus);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleExceptionInvalidToken(InvalidTokenException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<Object> handleExceptionTokenNotFound(TokenNotFoundException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Object> handleExceptionUserAlreadyExist(UserAlreadyExistsException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleExceptionUserNotFound(UserNotFoundException ex) {
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                ex.getHttpStatus(),
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, ex.getHttpStatus());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Object> handleExceptionIO(IOException ex) {
        HttpStatus httpStatus = (ex instanceof FileNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
        ApiRequestException apiRequestException = new ApiRequestException(
                ex.getMessage(),
                httpStatus,
                ZonedDateTime.now(ZoneId.of("Z"))
        );
        log.error("Call Api Error with message {}", ex.getMessage());
        return new ResponseEntity<>(apiRequestException, httpStatus);
    }

}
