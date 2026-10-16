package com.resto.saas.loyalty.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Бросается при конфликте оптимистической блокировки
 * если кто-то изменил данные между чтением и записью
 */
@ResponseStatus(HttpStatus.CONFLICT) // автоматически вернет http 409
public class ConcurrencyConflictException extends RuntimeException {

    public ConcurrencyConflictException(String message) {
        super(message);
    }
}
