package com.tanaguru.config;

import com.tanaguru.domain.dto.ErrorDTO;
import com.tanaguru.domain.exception.ForbiddenException;
import com.tanaguru.domain.exception.InvalidEntityException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import javax.persistence.EntityNotFoundException;
import java.util.NoSuchElementException;

/**
 * @author rcharre
 */
@ControllerAdvice
public class TanaguruControllerAdvice {

    @ResponseBody
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDTO entityNotFoundHandler(EntityNotFoundException ex) {
        return new ErrorDTO(ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDTO noSuchElementExceptionHandler(NoSuchElementException ex) {
        return new ErrorDTO(ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ErrorDTO entityForbidden(ForbiddenException ex) {
        return new ErrorDTO(ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorDTO badCredential(BadCredentialsException ex) {
        return new ErrorDTO(ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorDTO entityForbidden(HttpClientErrorException.Unauthorized ex) {
        return new ErrorDTO(ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(InternalError.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorDTO internalError(InternalError ex) {
        return new ErrorDTO(ex.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(InvalidEntityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDTO invalidEntityError(InvalidEntityException ex) {
        return new ErrorDTO(ex.getMessage());
    }
}
