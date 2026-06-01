package com.example.acceso.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${google.recaptcha.key.secret}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.verify.url}")
    private String recaptchaVerifyUrl;

    public boolean verificarRecaptcha(String recaptchaResponse) {
        // Validar si el usuario no hizo el captcha
        if (recaptchaResponse == null || recaptchaResponse.trim().isEmpty()) {
            return false;
        }

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
        requestMap.add("secret", recaptchaSecret);
        requestMap.add("response", recaptchaResponse);

        try {
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(recaptchaVerifyUrl, requestMap, Map.class);
            Map<String, Object> body = apiResponse.getBody();
            if (body != null) {
                return (Boolean) body.get("success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
