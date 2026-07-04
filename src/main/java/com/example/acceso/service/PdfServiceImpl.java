package com.example.acceso.service;

import com.example.acceso.dto.EventoAuditoriaDTO;
import com.example.acceso.model.DetallePedidoWeb;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.PedidoWeb;
import com.example.acceso.model.SesionCaja;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfServiceImpl implements PdfService {

    private final EmpresaService empresaService;

    public PdfServiceImpl(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @Override
    public ByteArrayInputStream generarEspecificacionCompra(PedidoWeb pedido) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            Empresa empresa = empresaService.getEmpresaInfo();
            String nombreEmpresa = (empresa != null && empresa.getNombre() != null) ? empresa.getNombre() : "Empresa";

            Paragraph title = new Paragraph("Especificación de Compra y Pago", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("Número de Pedido: " + pedido.getNumeroPedido(), normalFont));
            if (pedido.getPdfKey() != null) {
                document.add(new Paragraph("Código de Verificación: " + pedido.getPdfKey(), normalFont));
            }
            document.add(new Paragraph("Fecha y Hora: " + pedido.getFechaPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), normalFont));
            document.add(new Paragraph("Realizado a: " + nombreEmpresa, normalFont));
            document.add(new Paragraph("Realizado por (Cliente): " + pedido.getNombreCliente(), normalFont));
            document.add(new Paragraph("DNI: " + pedido.getDniCliente(), normalFont));
            document.add(new Paragraph("Teléfono: " + pedido.getTelefonoCliente(), normalFont));
            document.add(new Paragraph("Método de Pago: " + pedido.getMetodoPago(), normalFont));
            document.add(new Paragraph(" "));

            // Tabla de productos
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{4, 1, 2, 2});
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            PdfPCell c1 = new PdfPCell(new Phrase("Producto", boldFont));
            c1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c1);

            PdfPCell c2 = new PdfPCell(new Phrase("Cant.", boldFont));
            c2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c2);

            PdfPCell c3 = new PdfPCell(new Phrase("Precio Unit.", boldFont));
            c3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c3);

            PdfPCell c4 = new PdfPCell(new Phrase("Subtotal", boldFont));
            c4.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c4);

            for (DetallePedidoWeb detalle : pedido.getItems()) {
                table.addCell(detalle.getProducto().getNombre());
                
                PdfPCell cantCell = new PdfPCell(new Phrase(String.valueOf(detalle.getCantidad())));
                cantCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cantCell);
                
                PdfPCell puCell = new PdfPCell(new Phrase("S/ " + detalle.getPrecioUnitario()));
                puCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(puCell);
                
                PdfPCell subCell = new PdfPCell(new Phrase("S/ " + detalle.getSubtotal()));
                subCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(subCell);
            }

            document.add(table);

            Paragraph total = new Paragraph("Total Pagado: S/ " + pedido.getTotal(), titleFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingAfter(20);
            document.add(total);

            document.add(new Paragraph("Comprobante de Pago:", boldFont));
            if (pedido.getVoucherImagen() != null && !pedido.getVoucherImagen().isEmpty()) {
                try {
                    String imageUrl = pedido.getVoucherImagen();
                    if (!imageUrl.startsWith("http")) {
                        // En local, podrías necesitar otra lógica si es ruta de archivo, 
                        // pero Cloudinary siempre da URLs absolutas. Asumimos URL.
                    }
                    Image img = Image.getInstance(new URL(imageUrl));
                    img.scaleToFit(400, 400);
                    img.setAlignment(Element.ALIGN_CENTER);
                    document.add(img);
                } catch (Exception e) {
                    document.add(new Paragraph("No se pudo cargar la imagen del comprobante."));
                }
            } else {
                document.add(new Paragraph("No hay comprobante adjunto."));
            }

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public ByteArrayInputStream generarReporteSesionCaja(SesionCaja sesion, 
                                                         Map<String, Object> detalle, 
                                                         List<EventoAuditoriaDTO> logAuditoria) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Encabezado
            Paragraph header = new Paragraph("Reporte de Sesión de Caja #" + sesion.getId(), titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(10);
            document.add(header);

            document.add(new Paragraph("Generado: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smallFont));
            document.add(new Paragraph(" "));

            // Información de la sesión
            document.add(new Paragraph("Información de la Sesión", boldFont));
            document.add(new Paragraph("N° Sesión: " + sesion.getId(), normalFont));
            document.add(new Paragraph("Estado: " + (sesion.getEstado() != null ? sesion.getEstado() : "N/A"), normalFont));
            document.add(new Paragraph("Fecha Apertura: " + (sesion.getFechaApertura() != null ? sesion.getFechaApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"), normalFont));
            document.add(new Paragraph("Fecha Cierre: " + (sesion.getFechaCierre() != null ? sesion.getFechaCierre().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Sesión activa"), normalFont));
            document.add(new Paragraph("Cajero Apertura: " + (sesion.getUsuarioApertura() != null ? sesion.getUsuarioApertura().getNombre() : "N/A"), normalFont));
            document.add(new Paragraph("Cajero Cierre: " + (sesion.getUsuarioCierre() != null ? sesion.getUsuarioCierre().getNombre() : "-"), normalFont));
            document.add(new Paragraph("Fondo Inicial: S/ " + String.format("%.2f", sesion.getMontoInicial()), normalFont));
            document.add(new Paragraph("Total Ventas: S/ " + String.format("%.2f", sesion.getMontoCierreEsperado() != null ? sesion.getMontoCierreEsperado() : 0), normalFont));
            document.add(new Paragraph("Efectivo Esperado: S/ " + String.format("%.2f", sesion.getMontoCierreEsperado() != null ? sesion.getMontoCierreEsperado() : 0), normalFont));
            document.add(new Paragraph("Efectivo Declarado: S/ " + String.format("%.2f", sesion.getMontoCierreDeclarado() != null ? sesion.getMontoCierreDeclarado() : 0), normalFont));
            document.add(new Paragraph("Diferencia: S/ " + String.format("%.2f", sesion.getDiferencia() != null ? sesion.getDiferencia() : 0), normalFont));
            document.add(new Paragraph(" "));

            // Resumen por método de pago
            if (detalle != null && detalle.containsKey("ventasPorMetodoPago")) {
                document.add(new Paragraph("Resumen por Método de Pago", boldFont));
                
                @SuppressWarnings("unchecked")
                Map<String, BigDecimal> ventasPorMetodo = (Map<String, BigDecimal>) detalle.get("ventasPorMetodoPago");
                if (ventasPorMetodo != null && !ventasPorMetodo.isEmpty()) {
                    PdfPTable table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setWidths(new int[]{3, 2, 2, 2});

                    PdfPCell h1 = new PdfPCell(new Phrase("Método de Pago", boldFont));
                    h1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(h1);

                    PdfPCell h2 = new PdfPCell(new Phrase("Total", boldFont));
                    h2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(h2);

                    PdfPCell h3 = new PdfPCell(new Phrase("% del Total", boldFont));
                    h3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(h3);

                    PdfPCell h4 = new PdfPCell(new Phrase("Transacciones", boldFont));
                    h4.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(h4);

                    Object totalVentasObj = detalle.get("totalVentas");
                    double totalVentas = (totalVentasObj instanceof Number) ? ((Number) totalVentasObj).doubleValue() : 0;

                    for (Map.Entry<String, BigDecimal> entry : ventasPorMetodo.entrySet()) {
                        table.addCell(entry.getKey());
                        
                        double value = entry.getValue() != null ? entry.getValue().doubleValue() : 0;
                        table.addCell(new Phrase("S/ " + String.format("%.2f", value), normalFont));
                        
                        double porcentaje = totalVentas > 0 ? (value / totalVentas) * 100 : 0;
                        table.addCell(new Phrase(String.format("%.1f%%", porcentaje), normalFont));
                        
                        // Cantidad de transacciones (aproximada)
                        table.addCell(new Phrase("-", normalFont));
                    }

                    document.add(table);
                    document.add(new Paragraph(" "));
                }
            }

            // Log de auditoría
            if (logAuditoria != null && !logAuditoria.isEmpty()) {
                document.add(new Paragraph("Movimientos de la Sesión", boldFont));
                document.add(new Paragraph(" "));

                PdfPTable logTable = new PdfPTable(5);
                logTable.setWidthPercentage(100);
                logTable.setWidths(new int[]{2, 2, 3, 2, 2});

                PdfPCell lh1 = new PdfPCell(new Phrase("Fecha/Hora", boldFont));
                lh1.setHorizontalAlignment(Element.ALIGN_CENTER);
                logTable.addCell(lh1);

                PdfPCell lh2 = new PdfPCell(new Phrase("Evento", boldFont));
                lh2.setHorizontalAlignment(Element.ALIGN_CENTER);
                logTable.addCell(lh2);

                PdfPCell lh3 = new PdfPCell(new Phrase("Descripción", boldFont));
                lh3.setHorizontalAlignment(Element.ALIGN_CENTER);
                logTable.addCell(lh3);

                PdfPCell lh4 = new PdfPCell(new Phrase("Usuario", boldFont));
                lh4.setHorizontalAlignment(Element.ALIGN_CENTER);
                logTable.addCell(lh4);

                PdfPCell lh5 = new PdfPCell(new Phrase("Monto", boldFont));
                lh5.setHorizontalAlignment(Element.ALIGN_CENTER);
                logTable.addCell(lh5);

                for (EventoAuditoriaDTO evento : logAuditoria) {
                    logTable.addCell(new Phrase(evento.getFecha() != null ? evento.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A", smallFont));
                    logTable.addCell(new Phrase(evento.getNombre() != null ? evento.getNombre() : "N/A", smallFont));
                    logTable.addCell(new Phrase(evento.getDescripcion() != null ? evento.getDescripcion() : "N/A", smallFont));
                    logTable.addCell(new Phrase(evento.getUsuario() != null ? evento.getUsuario() : "N/A", smallFont));
                    
                    if (evento.getMonto() != null) {
                        logTable.addCell(new Phrase("S/ " + String.format("%.2f", evento.getMonto()), smallFont));
                    } else {
                        logTable.addCell(new Phrase("-", smallFont));
                    }
                }

                document.add(logTable);
            }

            // Pie de página
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Este documento es un reporte generado automáticamente por el sistema de caja.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
