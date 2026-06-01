package com.example.acceso.controller;

import com.example.acceso.model.Categoria;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.Producto;
import com.example.acceso.service.CategoriaService;
import com.example.acceso.service.EmpresaService;
import com.example.acceso.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CatalogoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final EmpresaService empresaService;

    public CatalogoController(ProductoService productoService, CategoriaService categoriaService, EmpresaService empresaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.empresaService = empresaService;
    }

    private void cargarDatosCatalogo(Model model) {
        Empresa empresa = empresaService.getEmpresaInfo();
        model.addAttribute("empresaGlobal", empresa);

        List<Categoria> categoriasActivas = categoriaService.listarCategoriasActivas();
        List<Producto> productosActivos = productoService.listarProductosActivos();
        Map<Categoria, List<Producto>> productosPorCategoria = categoriasActivas.stream()
                .collect(Collectors.toMap(
                        categoria -> categoria,
                        categoria -> productosActivos.stream()
                                .filter(producto -> producto.getCategoria() != null && producto.getCategoria().getId().equals(categoria.getId()))
                                .collect(Collectors.toList())
                ))
                .entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        model.addAttribute("productosPorCategoria", productosPorCategoria);
    }

    @GetMapping("/catalogo")
    public String verCatalogo(Model model) {
        cargarDatosCatalogo(model);
        return "catalogo";
    }

    @GetMapping("/catalogo/opiniones")
    public String verCatalogoOpiniones(Model model) {
        cargarDatosCatalogo(model);
        return "catalogo"; // Devuelve la misma vista, pero ahora en una URL dedicada
    }

    @GetMapping("/catalogo/api/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCatalogoData() {
        Empresa empresa = empresaService.getEmpresaInfo();
        List<Categoria> categoriasActivas = categoriaService.listarCategoriasActivas();
        List<Producto> productosActivos = productoService.listarProductosActivos();

        Map<Long, List<Producto>> productosPorCategoriaId = categoriasActivas.stream()
                .collect(Collectors.toMap(
                        Categoria::getId,
                        categoria -> productosActivos.stream()
                                .filter(p -> p.getCategoria() != null && p.getCategoria().getId().equals(categoria.getId()))
                                .collect(Collectors.toList())
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("empresaGlobal", empresa);
        response.put("productosPorCategoria", productosPorCategoriaId);
        response.put("categorias", categoriasActivas);

        return ResponseEntity.ok(response);
    }
}
