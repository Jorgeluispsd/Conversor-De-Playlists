package com.jorge.playlistconverter.errors.http;


import com.jorge.playlistconverter.dto.response.ApiErrorResponse;
import com.jorge.playlistconverter.dto.response.FieldValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Objects;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                             HttpServletRequest request){
        List<FieldValidationErrorResponse> fieldErros = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationErrorResponse(error.getField(),
                        Objects.requireNonNullElse(error.getDefaultMessage(),"Valor inválido.")))
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Existem campos inválidos na requisição.", request.getRequestURI(), fieldErros);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpServletRequest request){
        ApiErrorResponse response = new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST_BODY",
                "O corpo JSON está ausente, malformado ou contém um valor inválido.",
                request.getRequestURI(), List.of());

        return ResponseEntity.badRequest().body(response);
    }
}
