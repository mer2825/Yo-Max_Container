package com.example.acceso.service;

import com.example.acceso.dto.AuditDetailsDto;
import com.example.acceso.dto.MovimientoProductoDTO;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Producto;
import com.example.acceso.model.ProductoImagen;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.DetalleVentaRepository;
import com.example.acceso.repository.ProductoImagenRepository;
import com.example.acceso.repository.ProductoRepository;
import com.example.acceso.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoImagenRepository productoImagenRepository;
    private final com.example.acceso.repository.StockMovimientoRepository stockMovimientoRepository;
    private final CloudinaryService cloudinaryService;

    public ProductoService(ProductoRepository productoRepository, DetalleVentaRepository detalleVentaRepository, UsuarioRepository usuarioRepository, ProductoImagenRepository productoImagenRepository, com.example.acceso.repository.StockMovimientoRepository stockMovimientoRepository, CloudinaryService cloudinaryService) {
        this.productoRepository = productoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoImagenRepository = productoImagenRepository;
        this.stockMovimientoRepository = stockMovimientoRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepository.findAllByEstadoNot(2);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosActivos() {
        return productoRepository.findByEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarProductosBajoStock() {
        return productoRepository.findByEstado(1).stream()
                .filter(producto -> producto.getStock() != null
                        && producto.getStockMinimo() != null
                        && producto.getStock() <= producto.getStockMinimo())
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarProductos() {
        return productoRepository.countByEstado(1);
    }

    @Transactional(readOnly = true)
    public long contarProductosSinStock() {
        return productoRepository.findByEstado(1).stream()
                .filter(producto -> producto.getStock() != null && producto.getStock() <= 0)
                .count();
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularValorInventario() {
        return productoRepository.findByEstado(1).stream()
                .filter(producto -> producto.getStock() != null && producto.getPrecio() != null)
                .map(producto -> producto.getPrecio().multiply(BigDecimal.valueOf(producto.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Producto guardarProducto(Producto producto) {
        boolean isNew = producto.getId() == null;

        if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        if (producto.getPrecio() == null || producto.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        if (producto.getStock() == null || producto.getStock() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        if (producto.getStockMinimo() == null || producto.getStockMinimo() < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        if (producto.getCategoria() == null || producto.getCategoria().getId() == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }

        Optional<Producto> existingProductByName = productoRepository.findByNombreIgnoreCaseAndEstadoNot(producto.getNombre().trim(), 2);

        if (isNew) {
            if (existingProductByName.isPresent()) {
                throw new IllegalArgumentException("No se puede crear, ya existe un producto con el mismo nombre.");
            }
            producto.setEstado(1);
            producto.setUltimaAccion("Creación");
        } else {
            if (existingProductByName.isPresent() && !existingProductByName.get().getId().equals(producto.getId())) {
                throw new IllegalArgumentException("No se puede actualizar, ya existe otro producto con el mismo nombre.");
            }
            
            Producto productoExistente = productoRepository.findById(producto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para actualizar"));

            producto.setImagenes(productoExistente.getImagenes());
            
            productoExistente.setNombre(producto.getNombre().trim());
            productoExistente.setDescripcion(producto.getDescripcion() != null ? producto.getDescripcion().trim() : null);
            productoExistente.setPrecio(producto.getPrecio());
            productoExistente.setStock(producto.getStock());
            productoExistente.setStockMinimo(producto.getStockMinimo());
            productoExistente.setCategoria(producto.getCategoria());
            productoExistente.setUltimaAccion("Edición");
            
            producto = productoExistente;
        }

        try {
            return productoRepository.save(producto);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Error de integridad de datos. Es posible que el nombre ya exista.");
        }
    }

    @Transactional
    public ProductoImagen agregarImagenAProducto(MultipartFile file, Long productoId) throws IOException {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

        Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "productos");
        String imageUrl = uploadResult.get("secure_url");
        String publicId = uploadResult.get("public_id");

        int nextOrder = producto.getImagenes().stream()
                .mapToInt(ProductoImagen::getOrden)
                .max()
                .orElse(-1) + 1;

        ProductoImagen productoImagen = new ProductoImagen();
        productoImagen.setUrl(imageUrl);
        productoImagen.setPublicId(publicId);
        productoImagen.setProducto(producto);
        productoImagen.setOrden(nextOrder);

        return productoImagenRepository.save(productoImagen);
    }

    @Transactional
    public void actualizarOrdenImagenes(List<Long> idsImagenes) {
        if (idsImagenes == null || idsImagenes.isEmpty()) {
            return;
        }

        AtomicInteger orden = new AtomicInteger(0);
        idsImagenes.forEach(id -> {
            productoImagenRepository.findById(id).ifPresent(imagen -> {
                imagen.setOrden(orden.getAndIncrement());
                productoImagenRepository.save(imagen);
            });
        });
    }

    @Transactional
    public void eliminarImagen(Long idImagen) throws IOException {
        ProductoImagen imagen = productoImagenRepository.findById(idImagen)
                .orElseThrow(() -> new IllegalArgumentException("Imagen no encontrada con ID: " + idImagen));

        if (imagen.getPublicId() != null && !imagen.getPublicId().isEmpty()) {
            cloudinaryService.deleteFile(imagen.getPublicId());
        }

        productoImagenRepository.delete(imagen);
    }

    @Transactional
    public void eliminarImagenesBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<ProductoImagen> imagenes = productoImagenRepository.findAllById(ids);
        for (ProductoImagen imagen : imagenes) {
            try {
                if (imagen.getPublicId() != null && !imagen.getPublicId().isEmpty()) {
                    cloudinaryService.deleteFile(imagen.getPublicId());
                }
            } catch (IOException e) {
                System.err.println("Error al eliminar archivo de Cloudinary: " + e.getMessage());
            }
        }
        productoImagenRepository.deleteByIdIn(ids);
    }

    @Transactional(readOnly = true)
    public Optional<Producto> obtenerProductoPorId(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return productoRepository.findById(id);
    }

    @Transactional
    public void registrarAjusteStock(Long productoId, Integer cantidad, String comentario) {
        com.example.acceso.model.StockMovimiento movimiento = new com.example.acceso.model.StockMovimiento();
        movimiento.setProductoId(productoId);
        movimiento.setFecha(java.time.LocalDateTime.now());
        movimiento.setCantidad(cantidad != null ? cantidad : 0);
        movimiento.setTipo("AJUSTE");
        movimiento.setComentario(comentario);
        stockMovimientoRepository.save(movimiento);
    }

    @Transactional
    public void registrarMovimientoConDetalles(Long productoId, Integer cantidad, String tipoMovimiento,
                                                String motivo, String referenciaDocumento, String proveedor,
                                                String observacion, Integer stockAnterior, Integer stockResultante) {
        com.example.acceso.model.StockMovimiento movimiento = new com.example.acceso.model.StockMovimiento();
        movimiento.setProductoId(productoId);
        movimiento.setFecha(java.time.LocalDateTime.now());
        movimiento.setCantidad(cantidad);
        movimiento.setTipo(tipoMovimiento); // Mantener compatibilidad con campo existente

        // Nuevos campos
        try {
            movimiento.setTipoMovimiento(com.example.acceso.model.TipoMovimiento.valueOf(tipoMovimiento));
        } catch (IllegalArgumentException e) {
            // Si el tipo no es válido, usar null
            movimiento.setTipoMovimiento(null);
        }
        movimiento.setMotivo(motivo);
        movimiento.setReferenciaDocumento(referenciaDocumento);
        movimiento.setProveedor(proveedor);
        movimiento.setObservacion(observacion);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);

        stockMovimientoRepository.save(movimiento);
    }

    @Transactional
    public void registrarMovimientoConDetalles(Long productoId, Integer cantidad, String tipoMovimiento,
                                                String motivo, String referenciaDocumento, String proveedor,
                                                String observacion, Integer stockAnterior, Integer stockResultante,
                                                String usuario) {
        com.example.acceso.model.StockMovimiento movimiento = new com.example.acceso.model.StockMovimiento();
        movimiento.setProductoId(productoId);
        movimiento.setFecha(java.time.LocalDateTime.now());
        movimiento.setCantidad(cantidad);
        movimiento.setTipo(tipoMovimiento); // Mantener compatibilidad con campo existente

        // Nuevos campos
        try {
            movimiento.setTipoMovimiento(com.example.acceso.model.TipoMovimiento.valueOf(tipoMovimiento));
        } catch (IllegalArgumentException e) {
            // Si el tipo no es válido, usar null
            movimiento.setTipoMovimiento(null);
        }
        movimiento.setMotivo(motivo);
        movimiento.setReferenciaDocumento(referenciaDocumento);
        movimiento.setProveedor(proveedor);
        movimiento.setObservacion(observacion);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setComentario(usuario); // Usar campo comentario para usuario (compatibilidad)

        stockMovimientoRepository.save(movimiento);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de producto inválido");
        }
        Producto producto = obtenerProductoPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        producto.getImagenes().forEach(imagen -> {
            try {
                if (imagen.getPublicId() != null && !imagen.getPublicId().isEmpty()) {
                    cloudinaryService.deleteFile(imagen.getPublicId());
                }
            } catch (IOException e) {
                System.err.println("Error al eliminar archivo de Cloudinary: " + e.getMessage());
            }
        });

        producto.setEstado(2);
        producto.setUltimaAccion("Eliminación");
        productoRepository.save(producto);
    }

    @Transactional
    public Optional<Producto> cambiarEstadoProducto(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return obtenerProductoPorId(id).map(producto -> {
            if (producto.getEstado() == 1) {
                producto.setEstado(0);
                producto.setUltimaAccion("Inactivación");
            } else if (producto.getEstado() == 0) {
                producto.setEstado(1);
                producto.setUltimaAccion("Activación");
            }
            return productoRepository.save(producto);
        });
    }

    @Transactional(readOnly = true)
    public List<MovimientoProductoDTO> getMovimientosByProductId(Long productId) {
        List<DetalleVenta> detallesVenta = detalleVentaRepository.findByProductoId(productId);
        List<MovimientoProductoDTO> ventasMovs = detallesVenta.stream()
                .map(detalle -> {
                    MovimientoProductoDTO dto = new MovimientoProductoDTO(
                            detalle.getVenta().getNumeroVenta(),
                            detalle.getVenta().getFechaVenta(),
                            detalle.getPrecioUnitario(),
                            detalle.getCantidad(),
                            detalle.getSubtotal()
                    );
                    // Por ahora no incluimos usuario de ventas ya que Venta no tiene relación con Usuario
                    dto.setUsuario("Sistema"); // Valor por defecto
                    return dto;
                })
                .collect(Collectors.toList());

        List<com.example.acceso.model.StockMovimiento> movimientos = stockMovimientoRepository.findByProductoIdOrderByFechaDesc(productId);
        List<MovimientoProductoDTO> movimientosDTO = movimientos.stream()
                .map(a -> {
                    MovimientoProductoDTO dto = new MovimientoProductoDTO(
                            a.getTipoMovimiento() != null ? a.getTipoMovimiento().name() : a.getTipo(),
                            a.getFecha(),
                            java.math.BigDecimal.ZERO,
                            a.getCantidad(),
                            java.math.BigDecimal.ZERO
                    );
                    // Agregar nuevos campos
                    dto.setStockAnterior(a.getStockAnterior());
                    dto.setStockResultante(a.getStockResultante());
                    dto.setMotivo(a.getMotivo());
                    dto.setReferenciaDocumento(a.getReferenciaDocumento());
                    dto.setObservacion(a.getObservacion());
                    dto.setUsuario(a.getComentario()); // Usar comentario como usuario por ahora
                    return dto;
                })
                .collect(Collectors.toList());

        ventasMovs.addAll(movimientosDTO);
        return ventasMovs;
    }

    @Transactional(readOnly = true)
    public AuditDetailsDto getAuditDetails(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        String creadoPorNombre = "Dato no disponible";
        if (producto.getCreadoPor() != null) {
            creadoPorNombre = usuarioRepository.findById(producto.getCreadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaCreacion = "Dato no disponible";
        if (producto.getFechaCreacion() != null) {
            fechaCreacion = producto.getFechaCreacion().format(formatter);
        }

        String modificadoPorNombre = "N/A";
        if (producto.getModificadoPor() != null) {
            modificadoPorNombre = usuarioRepository.findById(producto.getModificadoPor()).map(Usuario::getNombre).orElse("Usuario no encontrado");
        }

        String fechaModificacion = "N/A";
        if (producto.getFechaModificacion() != null) {
            fechaModificacion = producto.getFechaModificacion().format(formatter);
        }

        String ultimaAccion = producto.getUltimaAccion() != null ? producto.getUltimaAccion() : "N/A";

        return new AuditDetailsDto(creadoPorNombre, fechaCreacion, modificadoPorNombre, fechaModificacion, ultimaAccion);
    }
}