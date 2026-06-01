package com.example.acceso.controller;

import com.example.acceso.model.Categoria;
import com.example.acceso.model.Producto;
import com.example.acceso.service.CategoriaService;
import com.example.acceso.service.ProductoService;
import com.example.acceso.service.VentaService;
import com.example.acceso.dto.ProductoMasVendidoDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public DashboardController(VentaService ventaService, ProductoService productoService, CategoriaService categoriaService) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/")
    public String mostrarDashboard(Model model) {
        long ventasDiariasNumero = ventaService.obtenerNumeroVentasDiarias();
        BigDecimal ventasDiariasTotal = ventaService.obtenerTotalVentasDiarias();
        long ventasMensualesNumero = ventaService.obtenerNumeroVentasMensuales();
        BigDecimal ventasMensualesTotal = ventaService.obtenerTotalVentasMensuales();

        List<ProductoMasVendidoDTO> top5Productos = ventaService.obtenerProductosMasVendidosDeHoy();
        List<ProductoMasVendidoDTO> top5ProductosSemana = ventaService.obtenerTop5ProductosMasVendidosDeLaSemana();
        List<Producto> lowStockProductos = productoService.listarProductosBajoStock();
        List<Producto> productosActivos = productoService.listarProductosActivos();
        List<Categoria> categoriasActivas = categoriaService.listarCategoriasActivas();

        long totalProductos = productoService.contarProductos();
        long totalCategorias = categoriasActivas.size();
        long sinStock = productoService.contarProductosSinStock();
        BigDecimal valorInventario = productoService.calcularValorInventario();

        Map<String, Long> productosPorCategoria = categoriasActivas.stream()
                .collect(Collectors.toMap(
                        Categoria::getNombre,
                        categoria -> productosActivos.stream()
                                .filter(producto -> producto.getCategoria() != null && producto.getCategoria().getId().equals(categoria.getId()))
                                .count()
                ));

        model.addAttribute("ventasDiariasNumero", ventasDiariasNumero);
        model.addAttribute("ventasDiariasTotal", ventasDiariasTotal);
        model.addAttribute("ventasMensualesNumero", ventasMensualesNumero);
        model.addAttribute("ventasMensualesTotal", ventasMensualesTotal);
        model.addAttribute("top5Productos", top5Productos);
        model.addAttribute("top5ProductosSemana", top5ProductosSemana);
        model.addAttribute("lowStockProductos", lowStockProductos);
        model.addAttribute("lowStockCount", lowStockProductos.size());
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalCategorias", totalCategorias);
        model.addAttribute("sinStock", sinStock);
        model.addAttribute("valorInventario", valorInventario);
        model.addAttribute("categoriasActivas", categoriasActivas);
        model.addAttribute("productosPorCategoria", productosPorCategoria);

        return "index";
    }
}
