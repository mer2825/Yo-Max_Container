package com.example.acceso.controller;

import com.example.acceso.model.Empresa;
import com.example.acceso.model.Producto;
import com.example.acceso.service.EmpresaService;
import com.example.acceso.service.ProductoService;
import com.example.acceso.service.CloudinaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/empresa")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/listar")
    public String gestionarEmpresa(Model model, HttpSession session) {
        Empresa empresa = empresaService.getEmpresaInfo();
        model.addAttribute("empresa", empresa);

        List<Producto> todosLosProductos = productoService.listarProductosActivos();
        model.addAttribute("todosLosProductos", todosLosProductos);

        model.addAttribute("activeUri", "/empresa/listar");
        return "gestionEmpresa";
    }

    @PostMapping("/api/guardar")
    @ResponseBody
    public ResponseEntity<?> guardarEmpresaApi(@RequestBody Empresa empresa, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Empresa empresaGuardada = empresaService.saveEmpresa(empresa);
            session.setAttribute("empresa", empresaGuardada);

            response.put("success", true);
            response.put("message", "Cambios guardados");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al guardar los cambios: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/subir-logo")
    @ResponseBody
    public ResponseEntity<?> subirLogo(@RequestParam("logoFile") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "El archivo está vacío.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 1. Subir la imagen a Cloudinary
            Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "logos");
            String imageUrl = uploadResult.get("secure_url");

            // 2. Obtener la empresa actual y actualizar su URL de logo
            Empresa empresa = empresaService.getEmpresaInfo();
            empresa.setLogoUrl(imageUrl);

            // 3. Guardar los cambios en la base de datos
            empresaService.saveEmpresa(empresa);
            
            // 4. Devolver la respuesta exitosa
            response.put("success", true);
            response.put("message", "Logo subido y guardado correctamente.");
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al subir el logo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/info")
    @ResponseBody
    public ResponseEntity<Empresa> getEmpresaInfo() {
        Empresa empresa = empresaService.getEmpresaInfo();
        return ResponseEntity.ok(empresa);
    }
}