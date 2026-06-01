package com.example.acceso.controller;

import com.example.acceso.model.Empresa;
import com.example.acceso.model.Producto;
import com.example.acceso.service.EmpresaService;
import com.example.acceso.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/empresa")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;
    @Autowired
    private ProductoService productoService; // Inyectar ProductoService

    private static final String UPLOAD_DIRECTORY = "C:/acceso/Images/";

    @GetMapping("/listar")
    public String gestionarEmpresa(Model model, HttpSession session) {
        // Cargar la información de la empresa
        Empresa empresa = empresaService.getEmpresaInfo();
        model.addAttribute("empresa", empresa);

        // Cargar todos los productos activos para el selector
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
            // El objeto 'empresa' que llega de JS ya contiene la lista de productos destacados
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
            // Obtener la información de la empresa para encontrar el logo antiguo
            Empresa empresa = empresaService.getEmpresaInfo();
            String oldLogoUrl = empresa.getLogoUrl();

            // Si existe un logo antiguo, intentar eliminarlo
            if (oldLogoUrl != null && !oldLogoUrl.isEmpty()) {
                try {
                    String filename = oldLogoUrl.substring(oldLogoUrl.lastIndexOf("/") + 1);
                    Path oldFilePath = Paths.get(UPLOAD_DIRECTORY).resolve(filename);
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    System.err.println("No se pudo eliminar el logo antiguo: " + e.getMessage());
                }
            }

            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFileName = "logo_" + UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath);

            String imageUrl = "/images/" + uniqueFileName;

            response.put("success", true);
            response.put("message", "Logo subido correctamente.");
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
