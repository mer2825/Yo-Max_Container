package com.example.acceso.service;

public interface EmailService {
    
    /**
     * Envía un email de confirmación cuando un pedido es aprobado
     * @param emailDestino Email del cliente
     * @param numeroPedido Número del pedido aprobado
     */
    void enviarEmailAprobacion(String emailDestino, String numeroPedido);
    
    /**
     * Envía un email de rechazo cuando un pedido es rechazado
     * @param emailDestino Email del cliente
     * @param numeroPedido Número del pedido rechazado
     * @param motivo Motivo del rechazo
     */
    void enviarEmailRechazo(String emailDestino, String numeroPedido, String motivo);
    
    /**
     * Envía un email de venta/boleta al cliente
     * @param ventaId ID de la venta
     * @throws Exception si ocurre un error al enviar el email
     */
    void enviarEmailVenta(Long ventaId) throws Exception;
}
