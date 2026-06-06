$(document).ready(function() {
    let dataTable;
    let boletaModal, editVentaModal;

    const API_BASE = '/ventas/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        print: (id) => `/ventas/imprimir/${id}`,
    };

    initializeDataTable();
    boletaModal = new bootstrap.Modal(document.getElementById('boletaModal'));
    editVentaModal = new bootstrap.Modal(document.getElementById('editVentaModal'));
    setupEventListeners();

    function initializeDataTable(url = ENDPOINTS.list) {
        if (dataTable) dataTable.destroy();
        dataTable = $('#tablaVentas').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: url, dataSrc: 'data' },
            columns: [
                { data: 'id' }, { data: 'numeroVenta' }, { data: 'nombreCliente' },
                { data: 'fechaVenta', render: data => new Date(data).toLocaleString('es-PE') },
                { data: 'metodoPago' }, { data: 'tipoComprobante' },
                { data: 'descuento', render: data => `S/ ${parseFloat(data).toFixed(2)}` }, // Nueva columna
                { data: 'total', render: data => `S/ ${parseFloat(data).toFixed(2)}` },
                { data: 'nota' },
                {
                    data: null, orderable: false, searchable: false,
                    render: (data, type, row) => `
                        <button class="btn btn-sm btn-info action-edit" data-id="${row.id}" title="Editar Venta"><i class="bi bi-pencil-fill"></i></button>
                        <button class="btn btn-sm btn-danger action-delete" data-id="${row.id}" title="Anular Venta"><i class="bi bi-trash-fill"></i></button>
                        <a href="/ventas/imprimir/${row.id}" target="_blank" class="btn btn-sm btn-primary" title="Imprimir Boleta"><i class="bi bi-printer-fill"></i></a>
                    `
                }
            ],
            language: { url: "//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" },
            order: [[0, 'desc']]
        });
    }

    function setupEventListeners() {
        $('#btnFiltrar').on('click', handleFiltrar);
        $('#btnLimpiar').on('click', handleLimpiar);
        $('#tablaVentas tbody').on('click', '.action-print', handlePrint);
        $('#tablaVentas tbody').on('click', '.action-delete', handleDelete);
        $('#tablaVentas tbody').on('click', '.action-edit', handleEdit);
        $('#btnPrintBoleta').on('click', () => document.getElementById('boletaIframe').contentWindow.print());

        // Escuchar mensajes desde el iframe de edición
        window.addEventListener('message', function(event) {
            if (event.data === 'ventaActualizada') {
                editVentaModal.hide();
                dataTable.ajax.reload();
                showNotification('Venta actualizada con éxito.', 'success');
            }
        });
    }

    function handleEdit() {
        const ventaId = $(this).data('id');
        console.log('Editando venta ID:', ventaId);
        const url = `/ventas/modificar/${ventaId}`;
        console.log('URL de edición:', url);
        
        // Cargar el contenido mediante AJAX
        $.get(url, function(data) {
            console.log('Datos recibidos:', data.length);
            // Crear un elemento temporal para parsear el HTML
            const tempDiv = $('<div>').html(data);
            // Extraer el contenido principal (excluyendo scripts y estilos)
            const content = tempDiv.find('.container-fluid').first();
            console.log('Contenido extraído:', content.length);
            $('#editVentaModal .modal-body').html(content);
            console.log('Contenido cargado en modal');
            
            // Configurar eventos de filtros en el modal
            setupModalFilters();
            
            // Cargar datos de la venta existente
            cargarDatosVentaModal(ventaId);
            
            editVentaModal.show();
        }).fail(function(xhr, status, error) {
            console.error('Error al cargar:', error);
            showNotification('Error al cargar el formulario de edición.', 'error');
        });
    }

    function cargarDatosVentaModal(ventaId) {
        $.get(`/ventas/api/detalle/${ventaId}`, function(response) {
            if (response.success && response.data) {
                const venta = response.data;
                console.log('Datos de venta cargados:', venta);
                
                // Cargar productos existentes en el carrito
                if (venta.detalles && venta.detalles.length > 0) {
                    const tbody = $('#editVentaModal #carrito-items');
                    tbody.empty();
                    
                    venta.detalles.forEach(detalle => {
                        const newRow = `<tr>
                            <td>${detalle.producto.nombre}</td>
                            <td><input type="number" class="form-control form-control-sm cantidad-item" value="${detalle.cantidad}" data-id="${detalle.producto.id}" min="1" max="${detalle.producto.stock || 999}"></td>
                            <td>S/ ${detalle.precioUnitario.toFixed(2)}</td>
                            <td><button class="btn btn-danger btn-sm remover-item" data-id="${detalle.producto.id}"><i class="bi bi-x-circle"></i></button></td>
                        </tr>`;
                        tbody.append(newRow);
                    });
                    
                    // Configurar evento para eliminar items
                    $('#editVentaModal').off('click', '.remover-item')
                        .on('click', '.remover-item', function() {
                            $(this).closest('tr').remove();
                            actualizarTotalModal();
                            showNotification('Producto eliminado del carrito', 'info');
                        });
                    
                    // Configurar evento para cambiar cantidad
                    $('#editVentaModal').off('change', '.cantidad-item')
                        .on('change', '.cantidad-item', function() {
                            actualizarTotalModal();
                        });
                    
                    actualizarTotalModal();
                }
                
                // Cargar otros datos de la venta
                if (venta.metodoPago) {
                    $('#editVentaModal #metodoPago').val(venta.metodoPago);
                }
                if (venta.nota) {
                    $('#editVentaModal #notaVenta').val(venta.nota);
                }
                if (venta.descuento && venta.descuento > 0) {
                    $('#editVentaModal #descuentoVenta').val(venta.descuento.toFixed(2));
                }
                
                showNotification('Datos de venta cargados correctamente', 'success');
            }
        }).fail(function() {
            showNotification('Error al cargar datos de la venta', 'error');
        });
    }

    function setupModalFilters() {
        // Usar delegación de eventos para los filtros
        $('#editVentaModal').off('keyup input change', '#filtroNombre, #filtroPrecioMin, #filtroPrecioMax')
            .on('keyup input change', '#filtroNombre, #filtroPrecioMin, #filtroPrecioMax', function() {
                filtrarProductosModal();
            });
        
        // Configurar eventos de categorías
        $('#editVentaModal').off('click', '.category-header')
            .on('click', '.category-header', function() {
                const target = $(this).data('bs-target');
                $(target).collapse('toggle');
            });
        
        // Configurar evento para agregar al carrito
        $('#editVentaModal').off('click', '.add-to-cart-btn')
            .on('click', '.add-to-cart-btn', function(e) {
                e.stopPropagation();
                const productItem = $(this).closest('.product-list-item');
                const productoId = productItem.data('product-id');
                agregarProductoAlCarritoModal(productoId);
            });
        
        // También permitir hacer clic en el item completo
        $('#editVentaModal').off('click', '.product-list-item')
            .on('click', '.product-list-item', function(e) {
                if (!$(e.target).hasClass('add-to-cart-btn')) {
                    const productoId = $(this).data('product-id');
                    agregarProductoAlCarritoModal(productoId);
                }
            });
        
        console.log('Eventos de filtros configurados en modal');
    }

    function agregarProductoAlCarritoModal(productoId) {
        // Simular la función agregarAlCarrito del modificar-venta.js
        const item = $('#editVentaModal .product-list-item[data-product-id="' + productoId + '"]');
        const producto = {
            id: item.data('product-id'),
            nombre: item.data('product-nombre'),
            precio: parseFloat(item.data('product-precio')),
            foto: item.data('product-foto'),
            stock: parseInt(item.data('product-stock'))
        };
        
        // Verificar si el producto ya está en el carrito
        const carritoItems = $('#editVentaModal #carrito-items tr');
        let itemExistente = null;
        carritoItems.each(function() {
            const nombre = $(this).find('td:first').text();
            if (nombre === producto.nombre) {
                itemExistente = $(this);
                return false;
            }
        });
        
        if (itemExistente) {
            const cantidadInput = itemExistente.find('.cantidad-item');
            const cantidadActual = parseInt(cantidadInput.val());
            if (cantidadActual + 1 > producto.stock) {
                showNotification(`No puedes agregar más. Stock disponible: ${producto.stock}`, 'warning');
                return;
            }
            cantidadInput.val(cantidadActual + 1);
            cantidadInput.trigger('change');
        } else {
            // Agregar nuevo item al carrito
            const newRow = `<tr>
                <td>${producto.nombre}</td>
                <td><input type="number" class="form-control form-control-sm cantidad-item" value="1" data-id="${producto.id}" min="1" max="${producto.stock}"></td>
                <td>S/ ${producto.precio.toFixed(2)}</td>
                <td><button class="btn btn-danger btn-sm remover-item" data-id="${producto.id}"><i class="bi bi-x-circle"></i></button></td>
            </tr>`;
            $('#editVentaModal #carrito-items').append(newRow);
            actualizarTotalModal();
        }
        
        showNotification('Producto agregado al carrito', 'success');
    }

    function actualizarTotalModal() {
        let subtotal = 0;
        $('#editVentaModal #carrito-items tr').each(function() {
            const cantidad = parseInt($(this).find('.cantidad-item').val());
            const precioText = $(this).find('td:nth-child(3)').text().replace('S/ ', '');
            const precio = parseFloat(precioText);
            subtotal += cantidad * precio;
        });
        
        const descuento = parseFloat($('#editVentaModal #descuentoVenta').val()) || 0;
        const tipoDescuento = $('#editVentaModal #tipoDescuento').val();
        let descuentoCalculado = 0;
        
        if (tipoDescuento === 'porcentaje') {
            descuentoCalculado = (subtotal * descuento) / 100;
        } else {
            descuentoCalculado = descuento;
        }
        
        const total = subtotal - descuentoCalculado;
        
        $('#editVentaModal #venta-subtotal').text(`S/ ${subtotal.toFixed(2)}`);
        $('#editVentaModal #venta-descuento').text(`S/ ${descuentoCalculado.toFixed(2)}`);
        $('#editVentaModal #venta-total').text(`S/ ${total.toFixed(2)}`);
    }

    function filtrarProductosModal() {
        const filtroNombre = $('#editVentaModal #filtroNombre').val().toLowerCase();
        const filtroPrecioMin = parseFloat($('#editVentaModal #filtroPrecioMin').val()) || 0;
        const filtroPrecioMax = parseFloat($('#editVentaModal #filtroPrecioMax').val()) || Infinity;
        
        const hayFiltroActivo = filtroNombre || filtroPrecioMin > 0 || filtroPrecioMax < Infinity;

        // Eliminar completamente cualquier contenedor de productos filtrados existente
        $('#editVentaModal #productos-filtrados-container').remove();

        if (hayFiltroActivo) {
            // Ocultar todas las categorías y encabezados de categoría
            $('#editVentaModal .category-group').hide();
            $('#editVentaModal .category-header').hide();
            
            // Crear un contenedor para los productos filtrados
            const container = $('<div id="productos-filtrados-container" class="mb-3"></div>');
            const lista = $('<ul class="product-list"></ul>');
            
            // Filtrar y agregar productos coincidentes
            $('#editVentaModal .product-list-item').each(function() {
                const item = $(this);
                const nombre = item.data('product-nombre').toLowerCase();
                const precio = parseFloat(item.data('product-precio'));

                const cumpleNombre = nombre.includes(filtroNombre);
                const cumplePrecioMin = precio >= filtroPrecioMin;
                const cumplePrecioMax = precio <= filtroPrecioMax;

                if (cumpleNombre && cumplePrecioMin && cumplePrecioMax) {
                    const productoClonado = $(this).clone();
                    productoClonado.show();
                    lista.append(productoClonado);
                }
            });
            
            if (lista.children().length > 0) {
                container.append(lista);
                $('#editVentaModal #product-selection-area').prepend(container);
            }
        } else {
            // Restaurar la vista normal con categorías
            $('#editVentaModal .category-group').show();
            $('#editVentaModal .category-header').show();
            $('#editVentaModal .collapse').removeClass('show');
            
            // Mostrar todos los productos originales
            $('#editVentaModal .category-group .product-list-item').show();
            
            // Ocultar categorías que no tengan productos visibles
            $('#editVentaModal .category-group').each(function() {
                const group = $(this);
                const productosVisibles = group.find('.product-list-item:visible').length;
                if (productosVisibles === 0) {
                    group.hide();
                } else {
                    group.show();
                }
            });
        }
    }

    function handleFiltrar() {
        const desde = $('#fechaDesde').val(), hasta = $('#fechaHasta').val();
        if (desde && hasta) {
            initializeDataTable(`${ENDPOINTS.list}?desde=${desde}&hasta=${hasta}`);
            showNotification('Mostrando ventas para el rango seleccionado.', 'info');
        } else {
            showNotification('Por favor, seleccione ambas fechas para filtrar.', 'error');
        }
    }

    function handleLimpiar() {
        $('#fechaDesde').val('');
        $('#fechaHasta').val('');
        initializeDataTable();
        showNotification('Filtros limpiados.', 'success');
    }

    function handlePrint() {
        const ventaId = $(this).data('id');
        console.log('Imprimiendo venta ID:', ventaId);
        if (ventaId) {
            const url = ENDPOINTS.print(ventaId);
            console.log('URL de impresión:', url);
            $('#boletaIframe').attr('src', url);
            boletaModal.show();
        } else {
            showNotification('No se pudo obtener el ID de la venta para imprimir.', 'error');
        }
    }

    function handleDelete() {
        const ventaId = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "La venta será marcada como eliminada lógicamente y dejará de mostrarse en el listado de ventas. No podrás revertir esto.",
            icon: 'warning',
            showCancelButton: true, confirmButtonColor: '#d33', cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, anular', cancelButtonText: 'Cancelar'
        }).then(result => {
            if (result.isConfirmed) {
                $.ajax({
                    url: ENDPOINTS.delete(ventaId), type: 'DELETE',
                    success: function(response) {
                        showNotification(response.message, response.success ? 'success' : 'error');
                        if (response.success) dataTable.ajax.reload();
                    },
                    error: (xhr) => showNotification(xhr.responseJSON?.message || 'Error al anular la venta.', 'error')
                });
            }
        });
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        const toastClass = type === 'success' ? 'text-bg-success' : (type === 'error' ? 'text-bg-danger' : 'text-bg-info');
        const toastHTML = `
            <div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>`;
        toastContainer.append(toastHTML);
        const toast = new bootstrap.Toast(toastContainer.children().last(), { delay: 3000 });
        toast.show();
    }
});
