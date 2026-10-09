package com.arthur.leaveflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarErroValidacao(MethodArgumentNotValidException e) {

        Map<String, String> erros = new HashMap<>();

        e.getBindingResult().getFieldErrors().forEach(erroCampo -> {
            erros.put(erroCampo.getField(), erroCampo.getDefaultMessage());
        });

        return erros;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String tratarErroEmailExistente(EmailAlreadyExistsException e) {
        return e.getMessage();
    }
}
