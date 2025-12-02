package com.onepercentgrowth.local_to_smartapi.controller;

import com.onepercentgrowth.local_to_smartapi.model.LoginRequest;
import com.onepercentgrowth.local_to_smartapi.model.LoginResponse;
import com.onepercentgrowth.local_to_smartapi.service.LoginService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return loginService.loginWithTotp(loginRequest);
    }

    @PostMapping("/refresh")
    public Mono<LoginResponse> refresh(@RequestParam String refreshToken,
                                       @RequestHeader("Authorization") String authHeader) {
        String authToken = authHeader.replace("Bearer ", "");
        return loginService.refreshTokens(refreshToken, authToken);
    }

    @PostMapping("/logout")
    public Mono<LoginResponse> logout(@RequestParam String clientCode,
                                      @RequestHeader("Authorization") String authHeader) {
        String authToken = authHeader.replace("Bearer ", "");
        return loginService.logout(clientCode, authToken);
    }
}

