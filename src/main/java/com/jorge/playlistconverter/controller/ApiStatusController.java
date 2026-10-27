package com.jorge.playlistconverter.controller;

import com.jorge.playlistconverter.dto.response.ApiStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class ApiStatusController {

    @GetMapping
    public ApiStatusResponse getStatus() {
        return new ApiStatusResponse("UP");
    }
}
