package com.example.acceso.controller;

import com.example.acceso.model.Empresa;
import com.example.acceso.model.Producto;
import com.example.acceso.service.ApisunatService;
import com.example.acceso.service.EmpresaService;
import com.example.acceso.service.ProductoService;
import com.example.acceso.service.CloudinaryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    @Autowired
    private ApisunatService apisunatService;

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
            // Antes de guardar, asegúrate de que los publicId no se pierdan
            Empresa empresaActual = empresaService.getEmpresaInfo();
            empresa.setLogoPublicId(empresaActual.getLogoPublicId());
            empresa.setQrYapePublicId(empresaActual.getQrYapePublicId());

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

        Empresa empresa = empresaService.getEmpresaInfo();
        String oldPublicId = empresa.getLogoPublicId();

        try {
            Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "logos");
            String newImageUrl = uploadResult.get("secure_url");
            String newPublicId = uploadResult.get("public_id");

            empresa.setLogoUrl(newImageUrl);
            empresa.setLogoPublicId(newPublicId);
            empresaService.saveEmpresa(empresa);

            if (oldPublicId != null && !oldPublicId.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(oldPublicId);
                } catch (IOException e) {
                    System.err.println("Error al eliminar el logo antiguo de Cloudinary: " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "Logo subido y guardado correctamente.");
            response.put("imageUrl", newImageUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al subir el logo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/subir-qr-yape")
    @ResponseBody
    public ResponseEntity<?> subirQrYape(@RequestParam("qrYapeFile") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "El archivo está vacío.");
            return ResponseEntity.badRequest().body(response);
        }

        Empresa empresa = empresaService.getEmpresaInfo();
        String oldPublicId = empresa.getQrYapePublicId();

        try {
            Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "qrs_yape");
            String newImageUrl = uploadResult.get("secure_url");
            String newPublicId = uploadResult.get("public_id");

            empresa.setQrYapeUrl(newImageUrl);
            empresa.setQrYapePublicId(newPublicId);
            empresaService.saveEmpresa(empresa);

            if (oldPublicId != null && !oldPublicId.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(oldPublicId);
                } catch (IOException e) {
                    System.err.println("Error al eliminar el QR Yape antiguo de Cloudinary: " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "QR de Yape subido y guardado correctamente.");
            response.put("imageUrl", newImageUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al subir el QR de Yape: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/api/info")
    @ResponseBody
    public ResponseEntity<Empresa> getEmpresaInfo() {
        Empresa empresa = empresaService.getEmpresaInfo();
        return ResponseEntity.ok(empresa);
    }

    @PostMapping("/api/probar-conexion-apisunat")
    @ResponseBody
    public ResponseEntity<?> probarConexionApisunat() {
        Map<String, Object> response = new HashMap<>();
        ApisunatService.ApisunatConnectionResult resultado = apisunatService.testConnection();

        response.put("success", resultado.isSuccess());
        response.put("message", resultado.getMessage());
        if (resultado.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}