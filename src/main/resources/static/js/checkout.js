// Checkout - Sistema de Pedidos Web con Yape

console.log('checkout.js loaded');

document.addEventListener('DOMContentLoaded', function() {
    console.log('checkout.js DOMContentLoaded fired');
    const uploadArea = document.getElementById('uploadArea');
    const selectFileBtn = document.getElementById('selectFileBtn');
    const voucherFile = document.getElementById('voucherFile');
    const previewContainer = document.getElementById('previewContainer');
    const previewImage = document.getElementById('previewImage');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const fileError = document.getElementById('fileError');
    const checkoutForm = document.getElementById('checkoutForm');
    const confirmBtn = document.getElementById('confirmBtn');
    const continueToPaymentBtn = document.getElementById('continueToPaymentBtn');
    const backToStep1Btn = document.getElementById('backToStep1Btn');
    const cartItems = document.getElementById('cartItems');
    const subtotalAmount = document.getElementById('subtotalAmount');
    const totalAmount = document.getElementById('totalAmount');
    const paymentAmount = document.getElementById('paymentAmount');
    const ultraCompactSummary = document.getElementById('ultraCompactSummary');
    
    const dniClienteInput = document.getElementById('dniCliente');
    const nombreClienteInput = document.getElementById('nombreCliente');
    const dniError = document.getElementById('dni-error');
    const dniSpinner = document.getElementById('dniSpinner');
    const emailClienteInput = document.getElementById('emailCliente');
    const emailError = document.getElementById('emailError');

    let uploadedFile = null;
    let cartData = [];
    let currentStep = 1;

    // --- VALIDACIÓN DE EMAIL EN TIEMPO REAL ---
    emailClienteInput.addEventListener('input', function() {
        const email = this.value.trim();
        const emailRegex = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
        if (email.length > 0 && !emailRegex.test(email)) {
            emailError.style.display = 'block';
            emailError.textContent = 'Ingresa un correo electrónico válido.';
        } else {
            emailError.style.display = 'none';
        }
    });

    emailClienteInput.addEventListener('blur', function() {
        const email = this.value.trim();
        const emailRegex = /^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/;
        if (email.length > 0 && !emailRegex.test(email)) {
            emailError.style.display = 'block';
        } else {
            emailError.style.display = 'none';
        }
    });

    // --- LÓGICA DE CONSULTA DE DNI ---
    dniClienteInput.addEventListener('blur', async function() {
        const dni = this.value.trim();

        // Limpiar estado previo
        nombreClienteInput.value = '';
        dniError.style.display = 'none';
        dniError.textContent = 'El DNI debe tener 8 dígitos numéricos.'; // Mensaje por defecto
        
        if (dni.length !== 8 || !/^[0-9]{8}$/.test(dni)) {
            dniError.style.display = 'block';
            return;
        }

        dniSpinner.style.display = 'inline-block';

        try {
            const response = await fetch(`/clientes/api/consultar-dni/${dni}`);
            const result = await response.json();

            if (response.ok && result.success) {
                nombreClienteInput.value = result.data.nombre;
            } else {
                dniError.textContent = result.message || 'DNI no encontrado.';
                dniError.style.display = 'block';
            }
        } catch (error) {
            console.error('Error al consultar DNI:', error);
            dniError.textContent = 'Error al conectar con el servicio. Intente de nuevo.';
            dniError.style.display = 'block';
        } finally {
            dniSpinner.style.display = 'none';
        }
    });

    // Cargar carrito desde localStorage
    function loadCart() {
        const cart = localStorage.getItem('cart');
        console.log('Cart from localStorage:', cart);
        if (cart) {
            cartData = JSON.parse(cart);
            console.log('Parsed cart data:', cartData);
            renderCart();
        } else {
            console.log('No cart found, redirecting to /');
            window.location.href = '/';
        }
    }

    // Renderizar carrito
    function renderCart() {
        console.log('renderCart called, cartData.length:', cartData.length);
        if (cartData.length === 0) {
            console.log('Cart is empty, redirecting to /');
            window.location.href = '/';
            return;
        }

        let subtotal = 0;
        cartItems.innerHTML = '';

        cartData.forEach((item, index) => {
            console.log('Processing item:', item);
            const precio = item.precio || item.price;
            const cantidad = item.cantidad || item.quantity;
            const stock = item.stock || 999; // Stock disponible del producto
            const nombre = item.nombre || item.name;
            const itemTotal = precio * cantidad;
            subtotal += itemTotal;

            const cartItem = document.createElement('div');
            cartItem.className = 'cart-item';
            cartItem.innerHTML = `
                <div style="flex: 1;">
                    <strong>${nombre}</strong>
                    <div class="d-flex align-items-center gap-2 mt-1">
                        <button type="button" class="btn btn-sm btn-outline-secondary btn-decrease-cart" data-index="${index}" ${cantidad <= 1 ? 'disabled' : ''}>−</button>
                        <input type="number" class="form-control form-control-sm text-center cart-qty-input" value="${cantidad}" min="1" max="${stock}" data-index="${index}" data-stock="${stock}" style="width: 60px;">
                        <button type="button" class="btn btn-sm btn-outline-secondary btn-increase-cart" data-index="${index}" ${cantidad >= stock ? 'disabled' : ''}>+</button>
                        <small class="text-muted ms-2">Disponible: ${stock}</small>
                    </div>
                    <small class="text-muted d-block">S/ ${precio.toFixed(2)} c/u</small>
                </div>
                <div class="text-end">
                    <span class="fw-bold">S/ ${itemTotal.toFixed(2)}</span>
                </div>
            `;
            cartItems.appendChild(cartItem);
        });

        const total = subtotal;
        subtotalAmount.textContent = `S/ ${subtotal.toFixed(2)}`;
        totalAmount.textContent = `S/ ${total.toFixed(2)}`;
        paymentAmount.textContent = `S/ ${total.toFixed(2)}`;
        generateUltraCompactSummary(total);
    }

    function generateUltraCompactSummary(total) {
        if (cartData.length === 0) return;
        let summaryText = '';
        if (cartData.length === 1) {
            const item = cartData[0];
            const nombre = item.nombre || item.name;
            const cantidad = item.cantidad || item.quantity;
            summaryText = `${nombre} x${cantidad}`;
        } else {
            const firstItem = cartData[0];
            const nombre = firstItem.nombre || firstItem.name;
            const cantidad = firstItem.cantidad || firstItem.quantity;
            const remainingCount = cartData.length - 1;
            summaryText = `${nombre} x${cantidad} + ${remainingCount} producto${remainingCount > 1 ? 's' : ''} más`;
        }
        summaryText += ` · Total: S/ ${total.toFixed(2)}`;
        ultraCompactSummary.textContent = summaryText;
    }

    function goToStep(step) {
        currentStep = step;
        document.querySelectorAll('.checkout-screen').forEach(screen => screen.classList.remove('active'));
        if (step === 1) {
            document.getElementById('screen1').classList.add('active');
            updateProgressBar(2);
        } else if (step === 2) {
            document.getElementById('screen2').classList.add('active');
            updateProgressBar(3);
        } else if (step === 3) {
            document.getElementById('screen3').classList.add('active');
            updateProgressBar(4);
        }
    }

    function updateProgressBar(activeStep) {
        document.querySelectorAll('.step').forEach((stepEl, index) => {
            const stepNumber = index + 1;
            stepEl.classList.remove('completed', 'active');
            if (stepNumber < activeStep) {
                stepEl.classList.add('completed');
            } else if (stepNumber === activeStep) {
                stepEl.classList.add('active');
            }
        });
    }

    function validateStep1() {
        const nombreCliente = document.getElementById('nombreCliente').value.trim();
        const dniCliente = document.getElementById('dniCliente').value.trim();
        const telefonoCliente = document.getElementById('telefonoCliente').value.trim();
        const emailCliente = document.getElementById('emailCliente').value.trim();
        const telefonoError = document.getElementById('telefonoError');
        const emailError = document.getElementById('emailError');
        
        if (!nombreCliente) {
            alert('Por favor, ingresa un DNI válido para autocompletar el nombre.');
            return false;
        }
        if (!dniCliente || !/^[0-9]{8}$/.test(dniCliente)) {
            dniError.style.display = 'block';
            return false;
        }
        if (!telefonoCliente || !/^9[0-9]{8}$/.test(telefonoCliente)) {
            telefonoError.style.display = 'block';
            return false;
        }
        if (!emailCliente || !/^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/.test(emailCliente)) {
            emailError.style.display = 'block';
            return false;
        }
        return true;
    }

    function validateFile(file) {
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        const maxSize = 5 * 1024 * 1024; // 5MB
        if (!validTypes.includes(file.type)) return 'El archivo debe ser JPG, PNG o WEBP';
        if (file.size > maxSize) return 'El archivo no debe superar 5MB';
        return null;
    }

    function showPreview(file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            previewImage.src = e.target.result;
            previewContainer.style.display = 'block';
            uploadArea.classList.add('has-file');
        };
        reader.readAsDataURL(file);
    }

    function clearFile() {
        uploadedFile = null;
        voucherFile.value = '';
        previewContainer.style.display = 'none';
        uploadArea.classList.remove('has-file');
        fileError.style.display = 'none';
        validateForm();
    }

    function validateForm() {
        confirmBtn.disabled = !uploadedFile;
    }

    uploadArea.addEventListener('click', () => voucherFile.click());
    uploadArea.addEventListener('dragover', (e) => { e.preventDefault(); uploadArea.classList.add('dragover'); });
    uploadArea.addEventListener('dragleave', () => uploadArea.classList.remove('dragover'));
    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) handleFile(e.dataTransfer.files[0]);
    });
    selectFileBtn.addEventListener('click', (e) => { e.stopPropagation(); voucherFile.click(); });
    voucherFile.addEventListener('change', (e) => { if (e.target.files.length > 0) handleFile(e.target.files[0]); });
    removeFileBtn.addEventListener('click', clearFile);

    function handleFile(file) {
        const error = validateFile(file);
        if (error) {
            fileError.textContent = error;
            fileError.style.display = 'block';
            return;
        }
        fileError.style.display = 'none';
        uploadedFile = file;
        showPreview(file);
        validateForm();
    }

    continueToPaymentBtn.addEventListener('click', () => { if (validateStep1()) goToStep(2); });
    backToStep1Btn.addEventListener('click', () => goToStep(1));

    // Event listeners para controles de cantidad en el carrito
    cartItems.addEventListener('click', function(e) {
        if (e.target.classList.contains('btn-increase-cart')) {
            const index = parseInt(e.target.dataset.index);
            changeCartQuantity(index, 1);
        } else if (e.target.classList.contains('btn-decrease-cart')) {
            const index = parseInt(e.target.dataset.index);
            changeCartQuantity(index, -1);
        }
    });

    cartItems.addEventListener('input', function(e) {
        if (e.target.classList.contains('cart-qty-input')) {
            const index = parseInt(e.target.dataset.index);
            const stock = parseInt(e.target.dataset.stock);
            const item = cartData[index];
            const itemName = item.nombre || item.name;
            let value = parseInt(e.target.value);

            if (isNaN(value) || value < 1) {
                value = 1;
            } else if (value > stock) {
                value = stock;
                showCheckoutStockAlert(itemName, stock);
            }

            e.target.value = value;
            cartData[index].cantidad = value;
            localStorage.setItem('cart', JSON.stringify(cartData));
            renderCart();
        }
    });

    function changeCartQuantity(index, delta) {
        const item = cartData[index];
        const stock = item.stock || 999;
        const itemName = item.nombre || item.name;
        const nuevaCantidad = item.cantidad + delta;

        if (nuevaCantidad <= 0) {
            // Eliminar item del carrito
            cartData.splice(index, 1);
        } else if (nuevaCantidad > stock) {
            showCheckoutStockAlert(itemName, stock);
            return;
        } else {
            item.cantidad = nuevaCantidad;
        }

        localStorage.setItem('cart', JSON.stringify(cartData));
        
        if (cartData.length === 0) {
            window.location.href = '/';
        } else {
            renderCart();
        }
    }

    confirmBtn.addEventListener('click', async () => {
        if (!uploadedFile) {
            fileError.textContent = 'Debes subir el comprobante de pago';
            fileError.style.display = 'block';
            return;
        }
        confirmBtn.disabled = true;
        confirmBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Procesando...';

        try {
            const formData = new FormData();
            formData.append('file', uploadedFile);
            const uploadResponse = await fetch('/api/upload', { method: 'POST', body: formData });
            if (!uploadResponse.ok) throw new Error('Error al subir el comprobante');
            const uploadData = await uploadResponse.json();
            const voucherPath = uploadData.filePath;

            const clienteId = document.getElementById('clienteId').value;
            const nombreCliente = document.getElementById('nombreCliente').value.trim();
            const dniCliente = document.getElementById('dniCliente').value.trim();
            const telefonoCliente = document.getElementById('telefonoCliente').value.trim();
            const emailCliente = document.getElementById('emailCliente').value.trim();

            const pedidoData = {
                cliente: clienteId ? { id: parseInt(clienteId) } : null,
                nombreCliente: nombreCliente,
                dniCliente: dniCliente,
                telefonoCliente: telefonoCliente,
                emailCliente: emailCliente,
                metodoPago: 'Yape',
                voucherImagen: voucherPath,
                items: cartData.map(item => ({
                    producto: { id: item.id },
                    cantidad: item.cantidad || item.quantity,
                    precioUnitario: item.precio || item.price
                }))
            };

            const pedidoResponse = await fetch('/pedidos_web/api/crear', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(pedidoData)
            });

            if (!pedidoResponse.ok) {
                const errorText = await pedidoResponse.text();
                console.error('Error al crear el pedido:', errorText);
                throw new Error('Error al crear el pedido');
            }

            const pedidoResult = await pedidoResponse.json();
            localStorage.removeItem('cart');
            updateCartCount();

            document.getElementById('confirmationOrderNumber').textContent = pedidoResult.pedidoId;
            document.getElementById('confirmationPhone').textContent = telefonoCliente;
            
            const downloadBtn = document.getElementById('downloadPdfBtn');
            if(downloadBtn) {
                downloadBtn.href = `/pedidos_web/api/descargar-especificacion/${pedidoResult.pedidoId}`;
            }

            goToStep(3);

        } catch (error) {
            console.error('Error:', error);
            alert('Hubo un error al procesar tu pedido. Por favor, intenta nuevamente.');
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="bi bi-check-circle"></i> Confirmar Pedido';
        }
    });

    function updateCartCount() {
        const cart = localStorage.getItem('cart');
        const count = cart ? JSON.parse(cart).length : 0;
        const cartCountElement = document.getElementById('cart-count');
        if (cartCountElement) cartCountElement.textContent = count;
    }

    function checkYapeConfig() {
        const yapeConfigAlert = document.getElementById('yapeConfigAlert');
        if (!yapeConfigAlert) return;
        const mainElement = document.querySelector('.checkout-container');
        const isAdmin = mainElement && mainElement.getAttribute('data-is-admin') === 'true';
        if (!isAdmin) return;
        const yapeNumberElement = document.querySelector('.yape-number');
        const yapeTitularElement = document.querySelector('.yape-titular');
        if (yapeNumberElement && yapeTitularElement) {
            const yapeNumber = yapeNumberElement.textContent.trim();
            const yapeTitular = yapeTitularElement.textContent.trim();
            if ((yapeNumber === '---' || yapeNumber === '') && (yapeTitular === '---' || yapeTitular === '')) {
                yapeConfigAlert.classList.remove('d-none');
            } else {
                yapeConfigAlert.classList.add('d-none');
            }
        }
    }

    // Escuchar cambios en localStorage (cuando el admin actualiza la empresa)
    window.addEventListener('storage', async function(e) {
        if (e.key === 'empresaActualizada') {
            console.log('Detectada actualización de empresa, recargando datos Yape...');
            try {
                const response = await fetch('/empresa/api/info');
                if (response.ok) {
                    const empresaInfo = await response.json();
                    
                    // Actualizar textos
                    const yapeNumberElement = document.querySelector('.yape-number');
                    const yapeTitularElement = document.querySelector('.yape-titular');
                    
                    if (yapeNumberElement) yapeNumberElement.textContent = empresaInfo.numeroYape || '---';
                    if (yapeTitularElement) yapeTitularElement.textContent = empresaInfo.titularYape || '---';

                    // Actualizar QR
                    const qrContainer = document.querySelector('.yape-info-card img[alt="QR Yape"]');
                    if (qrContainer) {
                        if (empresaInfo.qrYapeUrl) {
                            qrContainer.src = empresaInfo.qrYapeUrl;
                        } else {
                            qrContainer.parentElement.remove(); // Quitar el div si no hay QR
                        }
                    } else if (empresaInfo.qrYapeUrl) {
                        // Si no existía la imagen y ahora sí hay QR, añadirla
                        const colHtml = `
                            <div class="col-md-6">
                                <img src="${empresaInfo.qrYapeUrl}" alt="QR Yape" class="img-fluid rounded" style="max-height: 400px; border: 2px solid white; padding: 5px;">
                            </div>
                        `;
                        document.querySelector('.yape-info-card .row').insertAdjacentHTML('beforeend', colHtml);
                    }
                    
                    checkYapeConfig(); // Re-verificar alerta de admin
                }
            } catch (error) {
                console.error('Error al actualizar datos de empresa:', error);
            }
        }
    });

    loadCart();
    updateProgressBar(2);
    checkYapeConfig();
});

// --- FUNCIONES DE ALERTAS PROFESIONALES PARA CHECKOUT ---
const showCheckoutStockAlert = (productName, maxStock) => {
    const modalHTML = `
        <div class="modal fade" id="checkoutStockAlertModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg">
                    <div class="modal-header border-bottom-0 bg-warning text-dark">
                        <h5 class="modal-title">
                            <i class="bi bi-exclamation-triangle-fill me-2"></i>Límite de Stock Alcanzado
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body text-center py-4">
                        <div class="mb-3">
                            <i class="bi bi-box-seam" style="font-size: 4rem; color: #ffc107;"></i>
                        </div>
                        <h4 class="mb-3">Stock máximo disponible</h4>
                        <p class="text-muted mb-2">Lo sentimos, el stock máximo disponible para</p>
                        <h5 class="text-primary fw-bold mb-3">"${productName}"</h5>
                        <p class="text-muted">es de <span class="fw-bold text-warning">${maxStock} unidades</span>.</p>
                        <p class="text-muted small">Hemos ajustado la cantidad al límite permitido.</p>
                    </div>
                    <div class="modal-footer border-top-0">
                        <button type="button" class="btn btn-warning w-100" data-bs-dismiss="modal">
                            <i class="bi bi-check-circle me-2"></i>Entendido
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    const existingModal = document.getElementById('checkoutStockAlertModal');
    if (existingModal) {
        existingModal.remove();
    }

    document.body.insertAdjacentHTML('beforeend', modalHTML);
    const modal = new bootstrap.Modal(document.getElementById('checkoutStockAlertModal'));
    modal.show();

    // Limpiar el modal después de que se cierre
    document.getElementById('checkoutStockAlertModal').addEventListener('hidden.bs.modal', function () {
        this.remove();
    });
};
