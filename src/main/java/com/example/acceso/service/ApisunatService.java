package com.example.acceso.service;

import com.example.acceso.model.Cliente;
import com.example.acceso.model.DetalleVenta;
import com.example.acceso.model.Empresa;
import com.example.acceso.model.Venta;
import com.example.acceso.model.NotasCredito;
import com.example.acceso.dto.NotaCreditoItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApisunatService {

    private static final Logger logger = LoggerFactory.getLogger(ApisunatService.class);

    private final String apisunatUrl;
    private final String apisunatToken;
    private final String apisunatPersonaId;
    private final String apisunatPersonaToken;
    private final String apisunatRuc;
    private final String documentsUri;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApisunatService(@Value("${apisunat.url}") String apisunatUrl,
                           @Value("${apisunat.token}") String apisunatToken,
                           @Value("${apisunat.persona-id:}") String apisunatPersonaId,
                           @Value("${apisunat.persona-token:${apisunat.token}}") String apisunatPersonaToken,
                           @Value("${apisunat.ruc:}") String apisunatRuc,
                           @Value("${apisunat.documents-uri:/personas/v1/sendBill}") String documentsUri) {
        this.apisunatUrl = apisunatUrl;
        this.apisunatToken = apisunatToken;
        this.apisunatPersonaId = apisunatPersonaId;
        this.apisunatPersonaToken = apisunatPersonaToken;
        this.apisunatRuc = apisunatRuc;
        this.documentsUri = documentsUri;
        this.restTemplate = new RestTemplate();
    }

    public ApisunatResult emitirBoleta(Venta venta, Empresa empresa, String serie, int correlativo) {
        Cliente cliente = venta.getCliente();
        String tipoDoc;
        String numDoc;
        String rzSocial;

        if (cliente != null && "DNI".equalsIgnoreCase(cliente.getTipoDocumento()) && cliente.getNumeroDocumento() != null) {
            tipoDoc = "1";
            numDoc = cliente.getNumeroDocumento();
            rzSocial = cliente.getNombre();
        } else {
            tipoDoc = "0";
            numDoc = "";
            rzSocial = cliente != null ? cliente.getNombre() : "CONSUMIDOR FINAL";
        }

        return emitirDocumento(venta, empresa, "03", serie, correlativo, tipoDoc, numDoc, rzSocial);
    }

    public ApisunatResult emitirFactura(Venta venta, Empresa empresa, String serie, int correlativo) {
        Cliente cliente = venta.getCliente();
        String tipoDoc = "6";
        String numDoc = null;
        String rzSocial = null;

        if (cliente != null) {
            numDoc = cliente.getRuc();
            if (numDoc == null || numDoc.isBlank()) {
                numDoc = cliente.getNumeroDocumento();
            }
            rzSocial = cliente.getRazonSocial();
            if (rzSocial == null || rzSocial.isBlank()) {
                rzSocial = cliente.getNombre();
            }
        }

        return emitirDocumento(venta, empresa, "01", serie, correlativo, tipoDoc, numDoc, rzSocial);
    }

    public ApisunatResult emitirNotaCredito(Venta ventaOriginal,
                                             String serie,
                                             int correlativo,
                                             String tipoNota,
                                             String motivo,
                                             List<NotaCreditoItemDTO> items,
                                             Empresa empresa) {

        Map<String, Object> documentBody = new LinkedHashMap<>();

        // ==================== CABECERA UBL 2.1 ====================
        documentBody.put("cbc:UBLVersionID", Map.of("_text", "2.1"));
        documentBody.put("cbc:CustomizationID", Map.of("_text", "2.0"));
        documentBody.put("cbc:ID", Map.of("_text", serie + "-" + String.format("%08d", correlativo)));
        documentBody.put("cbc:IssueDate", Map.of("_text", LocalDate.now().toString()));
        documentBody.put("cbc:IssueTime", Map.of("_text", java.time.LocalTime.now().toString().substring(0, 8)));
        documentBody.put("cbc:DocumentCurrencyCode", Map.of("_text", "PEN"));

        // ==================== NOTA (monto en letras) ====================
        BigDecimal totalNC = BigDecimal.ZERO;
        for (NotaCreditoItemDTO item : items) {
            BigDecimal precioConIgv = item.getPrecioUnitario();
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            totalNC = totalNC.add(precioConIgv.multiply(cantidad));
        }
        String montoLetras = convertirNumeroALetras(totalNC);
        Map<String, Object> note = new LinkedHashMap<>();
        note.put("_text", montoLetras);
        note.put("_attributes", Map.of("languageLocaleID", "1000"));
        documentBody.put("cbc:Note", List.of(note));

        // ==================== DISCREPANCY RESPONSE ====================
        Map<String, Object> discrepancyResponse = new LinkedHashMap<>();
        discrepancyResponse.put("cbc:ResponseCode", Map.of("_text", tipoNota));
        discrepancyResponse.put("cbc:Description", Map.of("_text", motivo != null ? motivo : ""));
        documentBody.put("cac:DiscrepancyResponse", discrepancyResponse);

        // ==================== BILLING REFERENCE (comprobante original) ====================
        Cliente cliente = ventaOriginal.getCliente();
        boolean esFactura = "01".equals(ventaOriginal.getTipoComprobante())
                         || "factura".equalsIgnoreCase(ventaOriginal.getTipoComprobante());
        String tipoDocOriginal = esFactura ? "01" : "03";

        String serieCorrelativoOriginal = ventaOriginal.getSerieCorrelativo();

        Map<String, Object> invoiceDocRef = new LinkedHashMap<>();
        invoiceDocRef.put("cbc:ID", Map.of("_text", serieCorrelativoOriginal));
        invoiceDocRef.put("cbc:DocumentTypeCode", Map.of("_text", tipoDocOriginal));

        Map<String, Object> billingReference = new LinkedHashMap<>();
        billingReference.put("cac:InvoiceDocumentReference", invoiceDocRef);
        documentBody.put("cac:BillingReference", billingReference);

        // ==================== ACCOUNTING SUPPLIER PARTY (emisor) ====================
        String rucEmpresa = (empresa != null && empresa.getRucEmpresa() != null && !empresa.getRucEmpresa().isBlank())
            ? empresa.getRucEmpresa() : "20556548745";
        String nombreEmpresa = (empresa != null && empresa.getRazonSocialEmpresa() != null && !empresa.getRazonSocialEmpresa().isBlank())
            ? empresa.getRazonSocialEmpresa() : (empresa != null && empresa.getNombre() != null && !empresa.getNombre().isBlank() ? empresa.getNombre() : "TIenda de Importaciones Yo'Max");
        String direccionEmpresa = (empresa != null && empresa.getDireccion() != null && !empresa.getDireccion().isBlank())
            ? empresa.getDireccion() : "963 Monseñor Francisco Gonzales, Pueblo Nuevo, Ferreñafe";

        Map<String, Object> supplierPartyId = new LinkedHashMap<>();
        supplierPartyId.put("_attributes", Map.of("schemeID", "6"));
        supplierPartyId.put("_text", rucEmpresa);

        Map<String, Object> supplierIdentification = new LinkedHashMap<>();
        supplierIdentification.put("cbc:ID", supplierPartyId);

        Map<String, Object> supplierParty = new LinkedHashMap<>();
        supplierParty.put("cac:PartyIdentification", supplierIdentification);

        Map<String, Object> supplierPartyName = new LinkedHashMap<>();
        supplierPartyName.put("cbc:Name", Map.of("_text", nombreEmpresa));
        supplierParty.put("cac:PartyName", supplierPartyName);

        Map<String, Object> supplierLegalEntity = new LinkedHashMap<>();
        supplierLegalEntity.put("cbc:RegistrationName", Map.of("_text", nombreEmpresa));
        Map<String, Object> registrationAddress = new LinkedHashMap<>();
        registrationAddress.put("cbc:AddressTypeCode", Map.of("_text", "0000"));
        Map<String, Object> addressLine = new LinkedHashMap<>();
        addressLine.put("cbc:Line", Map.of("_text", direccionEmpresa));
        registrationAddress.put("cac:AddressLine", addressLine);
        supplierLegalEntity.put("cac:RegistrationAddress", registrationAddress);
        supplierParty.put("cac:PartyLegalEntity", supplierLegalEntity);

        Map<String, Object> accountingSupplierParty = new LinkedHashMap<>();
        accountingSupplierParty.put("cac:Party", supplierParty);
        documentBody.put("cac:AccountingSupplierParty", accountingSupplierParty);

        // ==================== ACCOUNTING CUSTOMER PARTY (cliente) ====================
        String tipoDocCliente;
        String numDocCliente;
        String nombreCliente;

        if (esFactura) {
            tipoDocCliente = "6";
            numDocCliente = (cliente != null && cliente.getRuc() != null && !cliente.getRuc().isBlank())
                ? cliente.getRuc()
                : (cliente != null && cliente.getNumeroDocumento() != null ? cliente.getNumeroDocumento() : "");
            nombreCliente = (cliente != null && cliente.getRazonSocial() != null && !cliente.getRazonSocial().isBlank())
                ? cliente.getRazonSocial()
                : (cliente != null ? cliente.getNombre() : "CONSUMIDOR FINAL");
        } else {
            if (cliente != null && "DNI".equalsIgnoreCase(cliente.getTipoDocumento()) && cliente.getNumeroDocumento() != null) {
                tipoDocCliente = "1";
                numDocCliente = cliente.getNumeroDocumento();
            } else {
                tipoDocCliente = "0";
                numDocCliente = "";
            }
            nombreCliente = cliente != null ? cliente.getNombre() : "CONSUMIDOR FINAL";
        }

        Map<String, Object> customerPartyId = new LinkedHashMap<>();
        customerPartyId.put("_attributes", Map.of("schemeID", tipoDocCliente));
        customerPartyId.put("_text", numDocCliente);

        Map<String, Object> customerIdentification = new LinkedHashMap<>();
        customerIdentification.put("cbc:ID", customerPartyId);

        Map<String, Object> customerParty = new LinkedHashMap<>();
        customerParty.put("cac:PartyIdentification", customerIdentification);

        Map<String, Object> customerLegalEntity = new LinkedHashMap<>();
        customerLegalEntity.put("cbc:RegistrationName", Map.of("_text", nombreCliente));
        customerParty.put("cac:PartyLegalEntity", customerLegalEntity);

        Map<String, Object> accountingCustomerParty = new LinkedHashMap<>();
        accountingCustomerParty.put("cac:Party", customerParty);
        documentBody.put("cac:AccountingCustomerParty", accountingCustomerParty);

        // ==================== CALCULAR TOTALES ====================
        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;

        // ==================== CREDIT NOTE LINES ====================
        List<Map<String, Object>> creditNoteLines = new ArrayList<>();
        int lineNumber = 1;

        for (NotaCreditoItemDTO item : items) {
            BigDecimal precioConIgv = item.getPrecioUnitario();
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            BigDecimal valorUnit = precioConIgv
                .divide(BigDecimal.valueOf(1.18), 6, RoundingMode.HALF_UP);
            BigDecimal mtoValorVenta = valorUnit
                .multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal igvItem = mtoValorVenta
                .multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalItem = precioConIgv
                .multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

            totalGravada = totalGravada.add(mtoValorVenta);
            totalIgv = totalIgv.add(igvItem);

            // --- CreditNoteLine ---
            Map<String, Object> creditNoteLine = new LinkedHashMap<>();
            creditNoteLine.put("cbc:ID", Map.of("_text", lineNumber++));

            // CreditedQuantity
            Map<String, Object> creditedQuantity = new LinkedHashMap<>();
            creditedQuantity.put("_attributes", Map.of("unitCode", "NIU"));
            creditedQuantity.put("_text", item.getCantidad());
            creditNoteLine.put("cbc:CreditedQuantity", creditedQuantity);

            // LineExtensionAmount
            Map<String, Object> lineExtensionAmount = new LinkedHashMap<>();
            lineExtensionAmount.put("_attributes", Map.of("currencyID", "PEN"));
            lineExtensionAmount.put("_text", mtoValorVenta);
            creditNoteLine.put("cbc:LineExtensionAmount", lineExtensionAmount);

            // PricingReference
            Map<String, Object> pricingReference = new LinkedHashMap<>();
            Map<String, Object> alternativeConditionPrice = new LinkedHashMap<>();
            Map<String, Object> priceAmount = new LinkedHashMap<>();
            priceAmount.put("_attributes", Map.of("currencyID", "PEN"));
            priceAmount.put("_text", precioConIgv);
            alternativeConditionPrice.put("cbc:PriceAmount", priceAmount);
            alternativeConditionPrice.put("cbc:PriceTypeCode", Map.of("_text", "01"));
            pricingReference.put("cac:AlternativeConditionPrice", alternativeConditionPrice);
            creditNoteLine.put("cac:PricingReference", pricingReference);

            // TaxTotal for line
            Map<String, Object> lineTaxTotal = new LinkedHashMap<>();
            Map<String, Object> lineTaxAmount = new LinkedHashMap<>();
            lineTaxAmount.put("_attributes", Map.of("currencyID", "PEN"));
            lineTaxAmount.put("_text", igvItem);
            lineTaxTotal.put("cbc:TaxAmount", lineTaxAmount);

            List<Map<String, Object>> lineTaxSubtotals = new ArrayList<>();
            Map<String, Object> lineTaxSubtotal = new LinkedHashMap<>();

            Map<String, Object> lineTaxableAmount = new LinkedHashMap<>();
            lineTaxableAmount.put("_attributes", Map.of("currencyID", "PEN"));
            lineTaxableAmount.put("_text", mtoValorVenta);
            lineTaxSubtotal.put("cbc:TaxableAmount", lineTaxableAmount);

            Map<String, Object> lineTaxAmount2 = new LinkedHashMap<>();
            lineTaxAmount2.put("_attributes", Map.of("currencyID", "PEN"));
            lineTaxAmount2.put("_text", igvItem);
            lineTaxSubtotal.put("cbc:TaxAmount", lineTaxAmount2);

            Map<String, Object> lineTaxCategory = new LinkedHashMap<>();
            lineTaxCategory.put("cbc:Percent", Map.of("_text", 18));
            lineTaxCategory.put("cbc:TaxExemptionReasonCode", Map.of("_text", "10"));

            Map<String, Object> lineTaxScheme = new LinkedHashMap<>();
            lineTaxScheme.put("cbc:ID", Map.of("_text", "1000"));
            lineTaxScheme.put("cbc:Name", Map.of("_text", "IGV"));
            lineTaxScheme.put("cbc:TaxTypeCode", Map.of("_text", "VAT"));
            lineTaxCategory.put("cac:TaxScheme", lineTaxScheme);

            lineTaxSubtotal.put("cac:TaxCategory", lineTaxCategory);
            lineTaxSubtotals.add(lineTaxSubtotal);
            lineTaxTotal.put("cac:TaxSubtotal", lineTaxSubtotals);
            creditNoteLine.put("cac:TaxTotal", lineTaxTotal);

            // Item
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("cbc:Description", Map.of("_text",
                item.getDescripcion() != null ? item.getDescripcion() : "Producto"));
            creditNoteLine.put("cac:Item", itemMap);

            // Price
            Map<String, Object> price = new LinkedHashMap<>();
            Map<String, Object> priceAmount2 = new LinkedHashMap<>();
            priceAmount2.put("_attributes", Map.of("currencyID", "PEN"));
            priceAmount2.put("_text", valorUnit.setScale(2, RoundingMode.HALF_UP));
            price.put("cbc:PriceAmount", priceAmount2);
            creditNoteLine.put("cac:Price", price);

            creditNoteLines.add(creditNoteLine);
        }

        documentBody.put("cac:CreditNoteLine", creditNoteLines);

        // ==================== TAX TOTAL ====================
        Map<String, Object> taxTotal = new LinkedHashMap<>();
        Map<String, Object> taxAmount = new LinkedHashMap<>();
        taxAmount.put("_attributes", Map.of("currencyID", "PEN"));
        taxAmount.put("_text", totalIgv);
        taxTotal.put("cbc:TaxAmount", taxAmount);

        List<Map<String, Object>> taxSubtotals = new ArrayList<>();
        Map<String, Object> taxSubtotal = new LinkedHashMap<>();

        Map<String, Object> taxableAmount = new LinkedHashMap<>();
        taxableAmount.put("_attributes", Map.of("currencyID", "PEN"));
        taxableAmount.put("_text", totalGravada);
        taxSubtotal.put("cbc:TaxableAmount", taxableAmount);

        Map<String, Object> taxAmount2 = new LinkedHashMap<>();
        taxAmount2.put("_attributes", Map.of("currencyID", "PEN"));
        taxAmount2.put("_text", totalIgv);
        taxSubtotal.put("cbc:TaxAmount", taxAmount2);

        Map<String, Object> taxCategory = new LinkedHashMap<>();
        Map<String, Object> taxScheme = new LinkedHashMap<>();
        taxScheme.put("cbc:ID", Map.of("_text", "1000"));
        taxScheme.put("cbc:Name", Map.of("_text", "IGV"));
        taxScheme.put("cbc:TaxTypeCode", Map.of("_text", "VAT"));
        taxCategory.put("cac:TaxScheme", taxScheme);
        taxSubtotal.put("cac:TaxCategory", taxCategory);
        taxSubtotals.add(taxSubtotal);
        taxTotal.put("cac:TaxSubtotal", taxSubtotals);
        documentBody.put("cac:TaxTotal", taxTotal);

        // ==================== LEGAL MONETARY TOTAL ====================
        Map<String, Object> legalMonetaryTotal = new LinkedHashMap<>();
        Map<String, Object> payableAmount = new LinkedHashMap<>();
        payableAmount.put("_attributes", Map.of("currencyID", "PEN"));
        payableAmount.put("_text", totalGravada.add(totalIgv).setScale(2, RoundingMode.HALF_UP));
        legalMonetaryTotal.put("cbc:PayableAmount", payableAmount);
        documentBody.put("cac:LegalMonetaryTotal", legalMonetaryTotal);

        // ==================== WRAPPER EXTERNO ====================
        String fileName = String.format("%s-07-%s-%08d", rucEmpresa, serie, correlativo);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("personaId", apisunatPersonaId);
        requestBody.put("personaToken", apisunatPersonaToken);
        requestBody.put("fileName", fileName);
        requestBody.put("documentBody", documentBody);

        // Log del payload completo para debug
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            logger.info("APISUNAT emitirNotaCredito payload: {}", json);
        } catch (Exception e) {
            logger.error("APISUNAT emitirNotaCredito -> error serializando payload", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apisunatToken);
        HttpEntity<Map<String, Object>> request =
            new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                buildEndpointUrl(), request, Map.class);
            Map<?, ?> body = response.getBody();
            String rawResponse = body != null ? body.toString() : "";
            String estado = extractString(body, "status", "estado", "resultado");

            if (response.getStatusCode().is2xxSuccessful() && body != null) {
                String pdfUrl = extractPdfUrl(body);
                String xmlUrl = extractString(body, "xml", "xmlUrl", "xml_url", "urlXml");
                String hashCdr = extractString(body, "cdrHash", "hash", "hashCdr", "hash_cdr");
                String documentId = extractString(body, "uuid", "id", "documentId");
                String errorCode = extractString(body, "message", "error", "detalle");
                String normalizedStatus = normalizeStatus(estado);
                logger.info("APISUNAT emitirNotaCredito -> normalizedStatus={} pdfUrl={} xmlUrl={} documentId={}",
                    normalizedStatus, pdfUrl, xmlUrl, documentId);
                return new ApisunatResult(normalizedStatus, pdfUrl, xmlUrl, hashCdr, errorCode, rawResponse, documentId);
            }

            return new ApisunatResult("EXCEPCION", null, null, null,
                extractString(body, "message", "error", "detalle"), rawResponse,
                extractString(body, "uuid", "id", "documentId"));

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            String rawResponse = ex.getResponseBodyAsString();
            logger.error("APISUNAT emitirNotaCredito failed status={} body={}",
                ex.getStatusCode(), rawResponse);
            return new ApisunatResult("EXCEPCION", null, null, null,
                ex.getStatusCode().toString(), rawResponse, null);
        } catch (Exception ex) {
            logger.error("APISUNAT emitirNotaCredito unexpected error", ex);
            return new ApisunatResult("EXCEPCION", null, null, null,
                ex.getMessage(), ex.toString(), null);
        }
    }

    private ApisunatResult emitirDocumento(Venta venta,
                                           Empresa empresa,
                                           String tipoDocumento,
                                           String serie,
                                           int correlativo,
                                           String tipoDoc,
                                           String numDoc,
                                           String rzSocial) {
        Map<String, Object> requestPayload = buildSendBillRequestBody(venta, empresa, tipoDocumento, serie, correlativo, tipoDoc, numDoc, rzSocial);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apisunatToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestPayload, headers);

        try {
            // Log del payload completo para debug
            try {
                String json = objectMapper.writeValueAsString(requestPayload);
                logger.info("APISUNAT emitirDocumento -> URL={} payload={}", buildEndpointUrl(), json);
            } catch (Exception e) {
                logger.error("APISUNAT emitirDocumento -> payload serializing failed", e);
            }

            ResponseEntity<Map> response = restTemplate.postForEntity(buildEndpointUrl(), request, Map.class);
            HttpStatusCode status = response.getStatusCode();
            Map<?, ?> body = response.getBody();
            String rawResponse = body != null ? body.toString() : "";
            String estado = extractString(body, "status", "estado", "resultado");
            
            logger.info("APISUNAT emitirDocumento response -> status={} bodyKeys={} estado={}", 
                status, body != null ? body.keySet() : "null", estado);

            if (status.is2xxSuccessful() && body != null) {
                String pdfUrl = extractPdfUrl(body);
                String xmlUrl = extractString(body, "xml", "xmlUrl", "xml_url", "urlXml");
                String hashCdr = extractString(body, "cdrHash", "hash", "hashCdr", "hash_cdr");
                String documentId = extractString(body, "uuid", "id", "documentId");
                String errorCode = extractString(body, "message", "error", "detalle");
                String normalizedStatus = normalizeStatus(estado);
                logger.info("APISUNAT emitirDocumento -> normalizedStatus={} pdfUrl={} xmlUrl={} documentId={}", 
                    normalizedStatus, pdfUrl, xmlUrl, documentId);
                return new ApisunatResult(normalizedStatus, pdfUrl, xmlUrl, hashCdr, errorCode, rawResponse, documentId);
            }

            return new ApisunatResult("EXCEPCION", null, null, null, extractString(body, "message", "error", "detalle"), rawResponse, extractString(body, "uuid", "id", "documentId"));
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            String rawResponse = ex.getResponseBodyAsString();
            try {
                logger.error("APISUNAT emitirDocumento failed status={} responseHeaders={} responseBody={}", ex.getStatusCode(), ex.getResponseHeaders(), rawResponse);
            } catch (Exception e) {
                logger.error("APISUNAT emitirDocumento failed and failed to log headers", e);
            }
            return new ApisunatResult("EXCEPCION", null, null, null, String.valueOf(ex.getStatusCode()), rawResponse, null);
        } catch (Exception ex) {
            logger.error("APISUNAT emitirDocumento unexpected error", ex);
            return new ApisunatResult("EXCEPCION", null, null, null, ex.getMessage(), ex.toString(), null);
        }
    }

    public Integer getLastDocument(String tipoDocumento, String serie) {
        if (apisunatPersonaId == null || apisunatPersonaId.isBlank() || apisunatPersonaToken == null || apisunatPersonaToken.isBlank()) {
            logger.error("APISUNAT getLastDocument: personaId o personaToken no configurados");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apisunatPersonaToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("personaId", apisunatPersonaId);
        requestBody.put("personaToken", apisunatPersonaToken);
        requestBody.put("type", tipoDocumento);
        requestBody.put("serie", serie);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = apisunatUrl + "/personas/lastDocument";
            logger.info("APISUNAT getLastDocument -> URL={} tipoDocumento={} serie={}", url, tipoDocumento, serie);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            
            if (body != null) {
                String lastNumber = extractString(body, "lastNumber");
                String suggestedNumber = extractString(body, "suggestedNumber");
                
                if (lastNumber != null && !lastNumber.isBlank()) {
                    int lastNum = Integer.parseInt(lastNumber);
                    logger.info("APISUNAT getLastDocument -> lastNumber={} suggestedNumber={}", lastNumber, suggestedNumber);
                    return lastNum;
                }
            }
            logger.warn("APISUNAT getLastDocument -> response no contiene lastNumber");
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("APISUNAT getLastDocument failed status={}", ex.getStatusCode(), ex);
            return null;
        } catch (Exception ex) {
            logger.error("APISUNAT getLastDocument exception", ex);
            return null;
        }
    }

    public ApisunatResult consultarDocumento(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return new ApisunatResult("EXCEPCION", null, null, null, 
                "documentId inválido", "", null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apisunatToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("personaId", apisunatPersonaId);
        requestBody.put("personaToken", apisunatPersonaToken);
        requestBody.put("documentId", documentId);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String url = apisunatUrl + "/personas/getDocument";
            logger.info("APISUNAT consultarDocumento -> URL={} documentId={}", url, documentId);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<?, ?> body = response.getBody();
            String rawResponse = body != null ? body.toString() : "";
            
            if (body != null) {
                String status = extractString(body, "status", "estado");
                String pdfUrl = extractPdfUrl(body);
                String xmlUrl = extractString(body, "xml", "xmlUrl", "xml_url");
                String hashCdr = extractString(body, "cdrHash", "hash", "hashCdr");
                String documentIdResp = extractString(body, "documentId", "uuid", "id");
                
                logger.info("APISUNAT consultarDocumento -> status={} pdfUrl={} xmlUrl={}", 
                    status, pdfUrl, xmlUrl);
                
                return new ApisunatResult(
                    normalizeStatus(status),
                    pdfUrl, xmlUrl, hashCdr,
                    null, rawResponse, documentIdResp
                );
            }
            
            return new ApisunatResult("EXCEPCION", null, null, null, 
                "Respuesta vacía", rawResponse, null);
                
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("APISUNAT consultarDocumento failed status={} body={}", 
                ex.getStatusCode(), ex.getResponseBodyAsString());
            return new ApisunatResult("EXCEPCION", null, null, null,
                ex.getStatusCode().toString(),
                ex.getResponseBodyAsString(), null);
        } catch (Exception ex) {
            logger.error("APISUNAT consultarDocumento unexpected error", ex);
            return new ApisunatResult("EXCEPCION", null, null, null,
                ex.getMessage(), ex.toString(), null);
        }
    }

    public ApisunatConnectionResult testConnection() {
        if (apisunatToken == null || apisunatToken.isBlank()) {
            return new ApisunatConnectionResult(false, "APISUNAT token no configurado.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apisunatToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            String urlToTest = buildEndpointUrl();
            ResponseEntity<String> response = restTemplate.exchange(urlToTest, HttpMethod.OPTIONS, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCodeValue() == 405) {
                return new ApisunatConnectionResult(true, "Conexión APISUNAT exitosa.");
            }
            return new ApisunatConnectionResult(false, "No se pudo validar la conexión con APISUNAT en la ruta " + urlToTest + ".");
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            logger.error("APISUNAT testConnection failed status={} url={}", ex.getStatusCode(), buildEndpointUrl(), ex);
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                return new ApisunatConnectionResult(false, "Token APISUNAT inválido o no autorizado.");
            }
            if (ex.getStatusCode().value() == 404) {
                return new ApisunatConnectionResult(false, "Ruta " + documentsUri + " no encontrada en APISUNAT. Verifica APISUNAT_URL.");
            }
            return new ApisunatConnectionResult(false, "Error de conexión APISUNAT: " + ex.getStatusCode() + ".");
        } catch (Exception ex) {
            logger.error("APISUNAT testConnection unexpected error url={}", buildEndpointUrl(), ex);
            return new ApisunatConnectionResult(false, "Error al conectar con APISUNAT: " + ex.getMessage());
        }
    }

    private String buildEndpointUrl() {
        String base = apisunatUrl.endsWith("/") 
            ? apisunatUrl.substring(0, apisunatUrl.length() - 1) 
            : apisunatUrl;
        String path = documentsUri.startsWith("/") 
            ? documentsUri 
            : "/" + documentsUri;
        return base + path;
    }

    public Map<String, Object> buildSendBillRequestBody(Venta venta,
                                                         Empresa empresa,
                                                         String tipoDocumento,
                                                         String serie,
                                                         int correlativo,
                                                         String tipoDoc,
                                                         String numDoc,
                                                         String rzSocial) {
        Map<String, Object> documentBody = buildDocumentBody(
            venta, tipoDocumento, serie, correlativo, tipoDoc, numDoc, rzSocial
        );

        String ruc = (apisunatRuc != null && !apisunatRuc.isBlank())
            ? apisunatRuc
            : (empresa != null && empresa.getRucEmpresa() != null && !empresa.getRucEmpresa().isBlank())
                ? empresa.getRucEmpresa()
                : "00000000000";

        String fileName = String.format("%s-%s-%s-%08d", ruc, tipoDocumento, serie, correlativo);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("personaId", apisunatPersonaId);
        requestBody.put("personaToken", apisunatPersonaToken);
        requestBody.put("fileName", fileName);
        requestBody.put("documentBody", documentBody);

        String customerEmail = resolveCustomerEmail(venta, empresa);
        if (customerEmail != null && !customerEmail.isBlank()) {
            requestBody.put("customerEmail", customerEmail);
        }

        return requestBody;
    }

    private Map<String, Object> buildDocumentBody(Venta venta,
                                                  String tipoDocumento,
                                                  String serie,
                                                  int correlativo,
                                                  String tipoDoc,
                                                  String numDoc,
                                                  String rzSocial) {
        Map<String, Object> payload = new HashMap<>();

        // UBL 2.1 structure
        payload.put("cbc:UBLVersionID", Map.of("_text", "2.1"));
        payload.put("cbc:CustomizationID", Map.of("_text", "2.0"));
        payload.put("cbc:ID", Map.of("_text", serie + "-" + String.format("%08d", correlativo)));
        payload.put("cbc:IssueDate", Map.of("_text", LocalDate.now().toString()));
        payload.put("cbc:IssueTime", Map.of("_text", java.time.LocalTime.now().toString()));
        
        Map<String, Object> invoiceTypeCode = new HashMap<>();
        invoiceTypeCode.put("_attributes", Map.of("listID", "0101"));
        invoiceTypeCode.put("_text", tipoDocumento);
        payload.put("cbc:InvoiceTypeCode", invoiceTypeCode);
        
        payload.put("cbc:DocumentCurrencyCode", Map.of("_text", "PEN"));

        // Agregar monto en letras como Note
        // Calcularemos el total después de procesar los items

        // AccountingSupplierParty (emisor)
        Map<String, Object> supplierParty = new LinkedHashMap<>();
        Map<String, Object> supplierPartyParty = new LinkedHashMap<>();
        Map<String, Object> supplierPartyIdentification = new LinkedHashMap<>();
        Map<String, Object> supplierId = new LinkedHashMap<>();
        supplierId.put("_attributes", Map.of("schemeID", "6"));
        supplierId.put("_text", apisunatRuc != null && !apisunatRuc.isBlank() ? apisunatRuc : "20556548745");
        supplierPartyIdentification.put("cbc:ID", supplierId);
        supplierPartyParty.put("cac:PartyIdentification", supplierPartyIdentification);
        
        Map<String, Object> supplierPartyLegalEntity = new LinkedHashMap<>();
        supplierPartyLegalEntity.put("cbc:RegistrationName", Map.of("_text", "TIenda de Importaciones Yo'Max"));
        Map<String, Object> supplierRegistrationAddress = new LinkedHashMap<>();
        supplierRegistrationAddress.put("cbc:AddressTypeCode", Map.of("_text", "0000"));
        supplierRegistrationAddress.put("cac:AddressLine", Map.of("cbc:Line", Map.of("_text", "963 Monseñor Francisco Gonzales, Pueblo Nuevo, Ferreñafe")));
        supplierPartyLegalEntity.put("cac:RegistrationAddress", supplierRegistrationAddress);
        supplierPartyParty.put("cac:PartyLegalEntity", supplierPartyLegalEntity);
        
        supplierParty.put("cac:Party", supplierPartyParty);
        payload.put("cac:AccountingSupplierParty", supplierParty);

        // AccountingCustomerParty (cliente)
        Map<String, Object> customerParty = new LinkedHashMap<>();
        Map<String, Object> customerPartyParty = new LinkedHashMap<>();
        Map<String, Object> customerPartyIdentification = new LinkedHashMap<>();
        Map<String, Object> customerId = new LinkedHashMap<>();
        customerId.put("_attributes", Map.of("schemeID", tipoDoc));
        customerId.put("_text", numDoc != null && !numDoc.isBlank() ? numDoc : "00000000");
        customerPartyIdentification.put("cbc:ID", customerId);
        customerPartyParty.put("cac:PartyIdentification", customerPartyIdentification);
        
        Map<String, Object> customerPartyLegalEntity = new LinkedHashMap<>();
        String customerName = (rzSocial != null && !rzSocial.isBlank()) ? rzSocial : "CONSUMIDOR FINAL";
        customerPartyLegalEntity.put("cbc:RegistrationName", Map.of("_text", customerName));
        customerPartyParty.put("cac:PartyLegalEntity", customerPartyLegalEntity);
        
        customerParty.put("cac:Party", customerPartyParty);
        payload.put("cac:AccountingCustomerParty", customerParty);

        // Calculate totals
        BigDecimal totalOperGravadas = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal totalVenta = BigDecimal.ZERO;
        List<Map<String, Object>> invoiceLines = new ArrayList<>();

        if (venta.getDetalles() != null) {
            int lineNumber = 1;
            for (DetalleVenta detalle : venta.getDetalles()) {
                BigDecimal precioConIgv = detalle.getPrecioUnitario() != null
                    ? detalle.getPrecioUnitario()
                    : BigDecimal.ZERO;
                
                BigDecimal cantidad = BigDecimal.valueOf(detalle.getCantidad());
                BigDecimal valorUnitario = precioConIgv
                    .divide(BigDecimal.valueOf(1.18), 6, RoundingMode.HALF_UP);
                BigDecimal lineExtensionAmount = valorUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
                BigDecimal igvItem = lineExtensionAmount.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalItem = precioConIgv.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

                totalOperGravadas = totalOperGravadas.add(lineExtensionAmount);
                totalIgv = totalIgv.add(igvItem);
                totalVenta = totalVenta.add(totalItem);

                // InvoiceLine
                Map<String, Object> invoiceLine = new LinkedHashMap<>();
                invoiceLine.put("cbc:ID", Map.of("_text", lineNumber++));
                
                Map<String, Object> invoicedQuantity = new LinkedHashMap<>();
                invoicedQuantity.put("_attributes", Map.of("unitCode", "NIU"));
                invoicedQuantity.put("_text", detalle.getCantidad());
                invoiceLine.put("cbc:InvoicedQuantity", invoicedQuantity);
                
                Map<String, Object> lineExtensionAmountField = new LinkedHashMap<>();
                lineExtensionAmountField.put("_attributes", Map.of("currencyID", "PEN"));
                lineExtensionAmountField.put("_text", lineExtensionAmount);
                invoiceLine.put("cbc:LineExtensionAmount", lineExtensionAmountField);
                
                // PricingReference
                Map<String, Object> pricingReference = new LinkedHashMap<>();
                Map<String, Object> alternativeConditionPrice = new LinkedHashMap<>();
                Map<String, Object> priceAmount = new LinkedHashMap<>();
                priceAmount.put("_attributes", Map.of("currencyID", "PEN"));
                priceAmount.put("_text", precioConIgv);
                alternativeConditionPrice.put("cbc:PriceAmount", priceAmount);
                alternativeConditionPrice.put("cbc:PriceTypeCode", Map.of("_text", "01"));
                pricingReference.put("cac:AlternativeConditionPrice", alternativeConditionPrice);
                invoiceLine.put("cac:PricingReference", pricingReference);
                
                // TaxTotal for line
                Map<String, Object> lineTaxTotal = new LinkedHashMap<>();
                Map<String, Object> lineTaxAmount = new LinkedHashMap<>();
                lineTaxAmount.put("_attributes", Map.of("currencyID", "PEN"));
                lineTaxAmount.put("_text", igvItem);
                lineTaxTotal.put("cbc:TaxAmount", lineTaxAmount);
                
                List<Map<String, Object>> lineTaxSubtotals = new ArrayList<>();
                Map<String, Object> lineTaxSubtotal = new LinkedHashMap<>();
                
                Map<String, Object> lineTaxableAmount = new LinkedHashMap<>();
                lineTaxableAmount.put("_attributes", Map.of("currencyID", "PEN"));
                lineTaxableAmount.put("_text", lineExtensionAmount);
                lineTaxSubtotal.put("cbc:TaxableAmount", lineTaxableAmount);
                
                Map<String, Object> lineTaxAmount2 = new LinkedHashMap<>();
                lineTaxAmount2.put("_attributes", Map.of("currencyID", "PEN"));
                lineTaxAmount2.put("_text", igvItem);
                lineTaxSubtotal.put("cbc:TaxAmount", lineTaxAmount2);
                
                Map<String, Object> lineTaxCategory = new LinkedHashMap<>();
                lineTaxCategory.put("cbc:Percent", Map.of("_text", 18));
                lineTaxCategory.put("cbc:TaxExemptionReasonCode", Map.of("_text", "10"));
                
                Map<String, Object> lineTaxScheme = new LinkedHashMap<>();
                lineTaxScheme.put("cbc:ID", Map.of("_text", "1000"));
                lineTaxScheme.put("cbc:Name", Map.of("_text", "IGV"));
                lineTaxScheme.put("cbc:TaxTypeCode", Map.of("_text", "VAT"));
                lineTaxCategory.put("cac:TaxScheme", lineTaxScheme);
                
                lineTaxSubtotal.put("cac:TaxCategory", lineTaxCategory);
                lineTaxSubtotals.add(lineTaxSubtotal);
                lineTaxTotal.put("cac:TaxSubtotal", lineTaxSubtotals);
                invoiceLine.put("cac:TaxTotal", lineTaxTotal);
                
                // Item
                Map<String, Object> item = new LinkedHashMap<>();
                String nombreProducto = (detalle.getProducto() != null && detalle.getProducto().getNombre() != null && !detalle.getProducto().getNombre().isBlank())
                    ? detalle.getProducto().getNombre()
                    : "Producto";
                item.put("cbc:Description", Map.of("_text", nombreProducto));
                invoiceLine.put("cac:Item", item);
                
                // Price
                Map<String, Object> price = new LinkedHashMap<>();
                Map<String, Object> priceAmount2 = new LinkedHashMap<>();
                priceAmount2.put("_attributes", Map.of("currencyID", "PEN"));
                priceAmount2.put("_text", valorUnitario);
                price.put("cbc:PriceAmount", priceAmount2);
                invoiceLine.put("cac:Price", price);
                
                invoiceLines.add(invoiceLine);
            }
        }

        payload.put("cac:InvoiceLine", invoiceLines);

        // TaxTotal
        Map<String, Object> taxTotal = new LinkedHashMap<>();
        Map<String, Object> taxAmount = new LinkedHashMap<>();
        taxAmount.put("_attributes", Map.of("currencyID", "PEN"));
        taxAmount.put("_text", totalIgv);
        taxTotal.put("cbc:TaxAmount", taxAmount);
        
        List<Map<String, Object>> taxSubtotals = new ArrayList<>();
        Map<String, Object> taxSubtotal = new LinkedHashMap<>();
        
        Map<String, Object> taxableAmount = new LinkedHashMap<>();
        taxableAmount.put("_attributes", Map.of("currencyID", "PEN"));
        taxableAmount.put("_text", totalOperGravadas);
        taxSubtotal.put("cbc:TaxableAmount", taxableAmount);
        
        Map<String, Object> taxAmount2 = new LinkedHashMap<>();
        taxAmount2.put("_attributes", Map.of("currencyID", "PEN"));
        taxAmount2.put("_text", totalIgv);
        taxSubtotal.put("cbc:TaxAmount", taxAmount2);
        
        Map<String, Object> taxCategory = new LinkedHashMap<>();
        Map<String, Object> taxScheme = new LinkedHashMap<>();
        taxScheme.put("cbc:ID", Map.of("_text", "1000"));
        taxScheme.put("cbc:Name", Map.of("_text", "IGV"));
        taxScheme.put("cbc:TaxTypeCode", Map.of("_text", "VAT"));
        taxCategory.put("cac:TaxScheme", taxScheme);
        
        taxSubtotal.put("cac:TaxCategory", taxCategory);
        taxSubtotals.add(taxSubtotal);
        taxTotal.put("cac:TaxSubtotal", taxSubtotals);
        payload.put("cac:TaxTotal", taxTotal);

        // LegalMonetaryTotal
        Map<String, Object> legalMonetaryTotal = new LinkedHashMap<>();
        
        Map<String, Object> lineExtensionAmount = new LinkedHashMap<>();
        lineExtensionAmount.put("_attributes", Map.of("currencyID", "PEN"));
        lineExtensionAmount.put("_text", totalOperGravadas);
        legalMonetaryTotal.put("cbc:LineExtensionAmount", lineExtensionAmount);
        
        Map<String, Object> taxInclusiveAmount = new LinkedHashMap<>();
        taxInclusiveAmount.put("_attributes", Map.of("currencyID", "PEN"));
        taxInclusiveAmount.put("_text", totalVenta);
        legalMonetaryTotal.put("cbc:TaxInclusiveAmount", taxInclusiveAmount);
        
        Map<String, Object> payableAmount = new LinkedHashMap<>();
        payableAmount.put("_attributes", Map.of("currencyID", "PEN"));
        payableAmount.put("_text", totalVenta);
        legalMonetaryTotal.put("cbc:PayableAmount", payableAmount);
        
        payload.put("cac:LegalMonetaryTotal", legalMonetaryTotal);

        // Agregar monto en letras como Note
        String montoLetras = convertirNumeroALetras(totalVenta);
        Map<String, Object> note = new HashMap<>();
        note.put("_text", montoLetras);
        note.put("_attributes", Map.of("languageLocaleID", "1000"));
        payload.put("cbc:Note", List.of(note));

        return payload;
    }

    public String convertirNumeroALetras(BigDecimal monto) {
        // Implementación de conversión a letras para montos en soles
        int soles = monto.intValue();
        int centavos = monto.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();
        
        // Validar rango máximo para evitar IndexOutOfBoundsException
        if (soles > 999) {
            // Para montos mayores a 999, usar formato numérico simplificado
            return "SON " + monto.setScale(2, RoundingMode.HALF_UP) + " SOLES";
        }
        
        String[] unidades = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE"};
        String[] decenas = {"", "DIEZ", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
        String[] especiales = {"DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"};
        
        String letras = "";
        
        // Manejar centenas
        if (soles >= 100) {
            if (soles == 100) {
                letras = "CIEN ";
            } else {
                letras = "CIENTO ";
            }
            soles %= 100;
        }
        
        // Manejar decenas y unidades
        if (soles >= 10 && soles <= 19) {
            // Números especiales del 10 al 19
            letras += especiales[soles - 10] + " ";
        } else if (soles >= 20) {
            // Decenas
            int decena = soles / 10;
            int unidad = soles % 10;
            letras += decenas[decena] + " ";
            if (unidad > 0) {
                letras += "Y " + unidades[unidad] + " ";
            }
        } else if (soles > 0) {
            // Unidades del 1 al 9
            letras += unidades[soles] + " ";
        }
        
        letras += "CON " + centavos + "/100 SOLES";
        return letras.toUpperCase().trim();
    }

    private String resolveCustomerEmail(Venta venta, Empresa empresa) {
        if (venta != null && venta.getCliente() != null && venta.getCliente().getEmail() != null && !venta.getCliente().getEmail().isBlank()) {
            return venta.getCliente().getEmail();
        }
        if (empresa != null && empresa.getEmail() != null && !empresa.getEmail().isBlank()) {
            return empresa.getEmail();
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "EXCEPCION";
        }
        status = status.trim().toUpperCase();
        if (status.contains("ACEPT")) {
            return "ACEPTADO";
        }
        if (status.contains("RECH")) {
            return "RECHAZADO";
        }
        if (status.contains("EXCEP")) {
            return "EXCEPCION";
        }
        return status;
    }

    private String extractPdfUrl(Map<?, ?> body) {
        if (body == null) {
            return null;
        }
        Object pdfObj = body.get("pdf");
        if (pdfObj instanceof Map) {
            Map<?, ?> pdfMap = (Map<?, ?>) pdfObj;
            // Preferir formato A4, si no existe usar el primero disponible
            if (pdfMap.containsKey("A4")) {
                return pdfMap.get("A4").toString();
            }
            // Si no hay A4, usar el primer valor disponible
            for (Object value : pdfMap.values()) {
                if (value != null) {
                    return value.toString();
                }
            }
        } else if (pdfObj != null) {
            return pdfObj.toString();
        }
        return null;
    }

    private String extractString(Map<?, ?> body, String... keys) {
        if (body == null) {
            return null;
        }
        for (String key : keys) {
            if (body.containsKey(key) && body.get(key) != null) {
                return body.get(key).toString();
            }
        }
        return null;
    }

    public static class ApisunatResult {
        private final String status;
        private final String pdfUrl;
        private final String xmlUrl;
        private final String hashCdr;
        private final String errorCode;
        private final String rawResponse;
        private final String documentId;

        public ApisunatResult(String status,
                              String pdfUrl,
                              String xmlUrl,
                              String hashCdr,
                              String errorCode,
                              String rawResponse,
                              String documentId) {
            this.status = status;
            this.pdfUrl = pdfUrl;
            this.xmlUrl = xmlUrl;
            this.hashCdr = hashCdr;
            this.errorCode = errorCode;
            this.rawResponse = rawResponse;
            this.documentId = documentId;
        }

        public String getStatus() {
            return status;
        }

        public String getPdfUrl() {
            return pdfUrl;
        }

        public String getXmlUrl() {
            return xmlUrl;
        }

        public String getHashCdr() {
            return hashCdr;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public String getDocumentId() {
            return documentId;
        }
    }

    public static class ApisunatConnectionResult {
        private final boolean success;
        private final String message;

        public ApisunatConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}