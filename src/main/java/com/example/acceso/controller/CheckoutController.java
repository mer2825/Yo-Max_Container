package com.example.acceso.controller;

import com.example.acceso.model.Usuario;
import com.example.acceso.model.Empresa;
import com.example.acceso.service.EmpresaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class CheckoutController {

    private final EmpresaService empresaService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public CheckoutController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping("/checkout")
    public String mostrarCheckout(Model model, HttpSession session) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuario");
        Empresa empresa = empresaService.getEmpresaInfo();
        model.addAttribute("usuarioLogueado", usuarioLogueado);
        model.addAttribute("empresaGlobal", empresa);
        return "checkout";
    }

    @GetMapping("/api/usuario-actual")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> obtenerUsuarioActual(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario != null) {
            return ResponseEntity.ok(Map.of("id", usuario.getId(), "nombre", usuario.getNombre()));
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "No hay usuario autenticado"));
        }
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> uploadVoucher(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "No se seleccionó ningún archivo");
            return ResponseEntity.badRequest().body(response);
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") &&
            !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            response.put("success", false);
            response.put("message", "El archivo debe ser JPG, PNG o WEBP");
            return ResponseEntity.badRequest().body(response);
        }

        // Validar tamaño (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            response.put("success", false);
            response.put("message", "El archivo no debe superar 5MB");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Crear directorio si no existe
            File uploadDirectory = new File(uploadDir + "vouchers/");
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            // Generar nombre único
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = "voucher_" + UUID.randomUUID().toString() + extension;

            Path path = Paths.get(uploadDir + "vouchers/" + filename);
            Files.write(path, file.getBytes());

            response.put("success", true);
            response.put("message", "Archivo subido correctamente");
            response.put("filePath", "/uploads/vouchers/" + filename);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al subir el archivo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
