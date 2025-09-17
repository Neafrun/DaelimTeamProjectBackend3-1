package org.benefitmap.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/login/success")
    public String loginSuccess() {
        return "로그인 성공!";
    }
}
