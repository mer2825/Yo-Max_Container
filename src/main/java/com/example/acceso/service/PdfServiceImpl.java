package com.example.acceso.service;

import com.example.acceso.model.DetallePedidoWeb;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.PedidoWeb;
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
}
