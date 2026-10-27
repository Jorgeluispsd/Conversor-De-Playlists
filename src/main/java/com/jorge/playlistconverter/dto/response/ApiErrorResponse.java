package com.jorge.playlistconverter.dto.response;

import java.util.List;

public record ApiErrorResponse(int status, String code, String message,
                               String path, List<FieldValidationErrorResponse> fieldErrors) {
    public ApiErrorResponse{
        fieldErrors = List.copyOf(fieldErrors);
    }
}
