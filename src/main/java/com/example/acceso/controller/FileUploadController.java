package com.example.acceso.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

@Controller
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // Endpoint /api/upload movido a CheckoutController para evitar conflictos

}
