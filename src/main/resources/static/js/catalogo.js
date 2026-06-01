document.addEventListener('DOMContentLoaded', function() {

    // --- Verificar si el usuario está logueado ---
    checkUserAuth();

    // --- Lógica para el Botón de Pedido ---
    const btnRealizarPedido = document.getElementById('btnRealizarPedido');
    if (btnRealizarPedido) {
        btnRealizarPedido.addEventListener('click', function() {
            window.location.href = "/login";
        });
    }

    function checkUserAuth() {
        // Verificar si hay usuario logueado
        fetch('/api/usuario-actual')
            .then(response => {
                if (response.ok) {
                    return response.json();
                }
                throw new Error('No autenticado');
            })
            .then(data => {
                // Usuario logueado
                const authBtn = document.getElementById('auth-btn');
                const misPedidosBtn = document.getElementById('mis-pedidos-btn');
                
                if (authBtn) {
                    authBtn.textContent = 'Cerrar Sesión';
                    authBtn.href = '/logout';
                }
                
                if (misPedidosBtn) {
                    misPedidosBtn.classList.remove('d-none');
                }
            })
            .catch(error => {
                // Usuario no logueado - mantener botón de login
                console.log('Usuario no autenticado');
            });
    }

    // --- Lógica para el Carrusel Compacto ---
    function inicializarCarrusel() {
        const track = document.querySelector('.cover-flow-carousel-track');
        if (!track) return;

        const items = Array.from(track.children);
        const nextButton = document.querySelector('.carousel-nav-btn.next');
        const prevButton = document.querySelector('.carousel-nav-btn.prev');

        const nombreEl = document.getElementById('producto-nombre');
        const descripcionEl = document.getElementById('producto-descripcion');
        const precioEl = document.getElementById('producto-precio');

        if (items.length === 0) return;

        let currentIndex = 0;
        let autoSlideInterval;

        const updateCarousel = () => {
            items.forEach((item, index) => {
                item.classList.remove('active', 'prev', 'next');

                const prevIndex = (currentIndex - 1 + items.length) % items.length;
                const nextIndex = (currentIndex + 1) % items.length;

                if (index === currentIndex) {
                    item.classList.add('active');
                    if (nombreEl && descripcionEl && precioEl) {
                        nombreEl.textContent = item.dataset.nombre;
                        descripcionEl.textContent = item.dataset.descripcion;
                        precioEl.textContent = 'S/ ' + item.dataset.precio;
                    }
                } else if (index === prevIndex) {
                    item.classList.add('prev');
                } else if (index === nextIndex) {
                    item.classList.add('next');
                }
            });
        };

        const moveToIndex = (index) => {
            currentIndex = index;
            updateCarousel();
        };

        const moveToNext = () => {
            const newIndex = (currentIndex + 1) % items.length;
            moveToIndex(newIndex);
        };

        const moveToPrev = () => {
            const newIndex = (currentIndex - 1 + items.length) % items.length;
            moveToIndex(newIndex);
        };

        const startAutoSlide = () => {
            autoSlideInterval = setInterval(moveToNext, 3000);
        };

        const stopAutoSlide = () => {
            clearInterval(autoSlideInterval);
        };

        const resetAutoSlide = () => {
            stopAutoSlide();
            startAutoSlide();
        };

        nextButton.addEventListener('click', () => {
            moveToNext();
            resetAutoSlide();
        });

        prevButton.addEventListener('click', () => {
            moveToPrev();
            resetAutoSlide();
        });

        const carouselWrapper = document.querySelector('.cover-flow-carousel-wrapper');
        carouselWrapper.addEventListener('mouseenter', stopAutoSlide);
        carouselWrapper.addEventListener('mouseleave', startAutoSlide);

        // Inicializar
        moveToIndex(0);
        startAutoSlide();
    }
    inicializarCarrusel();


    // --- Lógica de Filtros y Categorías ---
    const categoryFilterButtonsContainer = document.getElementById('categoryFilterButtons');
    const allProductsDisplay = document.getElementById('allProductsDisplay');
    const productItems = allProductsDisplay ? allProductsDisplay.querySelectorAll('.product-item') : [];
    const filterNombre = document.getElementById('filterNombre');
    const filterPrecioMin = document.getElementById('filterPrecioMin');
    const filterPrecioMax = document.getElementById('filterPrecioMax');

    function filterAndDisplayProducts() {
        const nombreFilter = filterNombre ? filterNombre.value.toLowerCase() : '';
        const precioMinFilter = filterPrecioMin ? parseFloat(filterPrecioMin.value) || 0 : 0;
        const precioMaxFilter = filterPrecioMax ? parseFloat(filterPrecioMax.value) || Infinity : Infinity;

        const activeCategoryButton = categoryFilterButtonsContainer ? categoryFilterButtonsContainer.querySelector('.active') : null;
        const activeCategoryId = activeCategoryButton ? activeCategoryButton.getAttribute('data-category-id') : 'all';

        productItems.forEach(product => {
            const productName = product.getAttribute('data-product-name').toLowerCase();
            const productPrice = parseFloat(product.getAttribute('data-product-price'));
            const productCategoryId = product.getAttribute('data-category-id');

            const matchesName = productName.includes(nombreFilter);
            const matchesPrice = productPrice >= precioMinFilter && productPrice <= precioMaxFilter;
            const matchesCategory = (activeCategoryId === 'all' || productCategoryId === activeCategoryId);

            product.style.display = (matchesName && matchesPrice && matchesCategory) ? '' : 'none';
        });
    }

    function attachRealtimeFilterEvents() {
        [filterNombre, filterPrecioMin, filterPrecioMax].forEach(input => {
            if (input) {
                input.addEventListener('input', filterAndDisplayProducts);
            }
        });
    }

    // Evento para los botones de categoría
    if (categoryFilterButtonsContainer) {
        categoryFilterButtonsContainer.addEventListener('click', function(e) {
            if (e.target.matches('.btn-outline-primary')) {
                const currentActive = categoryFilterButtonsContainer.querySelector('.active');
                if (currentActive) {
                    currentActive.classList.remove('active');
                }
                e.target.classList.add('active');
                filterAndDisplayProducts();
            }
        });
    }

    attachRealtimeFilterEvents();

    if (productItems.length > 0) {
        filterAndDisplayProducts();
    }

    // --- Lógica del Carrito de Compras ---
    const cartToggleButton = document.getElementById('cart-toggle-btn');
    const cartCloseButton = document.getElementById('cart-close-btn');
    const cartSidebar = document.getElementById('cart-sidebar');
    const mainContainer = document.querySelector('.main-container');
    const cartItemsContainer = document.getElementById('cart-items');
    const cartCount = document.getElementById('cart-count');
    const cartTotal = document.getElementById('cart-total');
    const btnFinalizarCompra = document.getElementById('btn-finalizar-compra');
    const customerDniInput = document.getElementById('customer-dni');
    const dniValidationMessage = document.getElementById('dni-validation-message');

    let cart = [];
    let discountApplied = false;

    const toggleCart = (forceOpen = false) => {
        if (forceOpen) {
            cartSidebar.classList.add('open');
            mainContainer.classList.add('cart-open');
        } else {
            cartSidebar.classList.toggle('open');
            mainContainer.classList.toggle('cart-open');
        }
    };

    const removeFromCart = (productId) => {
        cart = cart.filter(item => item.id !== productId);
        updateCartUI();
    };

    const updateQuantity = (productId, newQuantity) => {
        const quantity = parseInt(newQuantity, 10);
        const item = cart.find(item => item.id === productId);

        if (item) {
            if (isNaN(quantity) || quantity < 1) {
                removeFromCart(productId);
            } else {
                item.quantity = quantity;
                updateCartUI();
            }
        }
    };

    const updateCartUI = () => {
        cartItemsContainer.innerHTML = '';
        let subtotal = 0;
        let totalItems = 0;

        if (cart.length === 0) {
            cartItemsContainer.innerHTML = '<p class="text-center my-4">Tu carrito está vacío.</p>';
        } else {
            cart.forEach(item => {
                const itemTotal = item.price * item.quantity;
                subtotal += itemTotal;
                totalItems += item.quantity;

                const cartItemElement = document.createElement('div');
                cartItemElement.classList.add('cart-item', 'mb-3', 'p-2', 'border', 'rounded');
                cartItemElement.dataset.productId = item.id;
                cartItemElement.innerHTML = `
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="fw-bold text-truncate" style="max-width: 150px;">${item.name}</span>
                        <button class="btn btn-sm btn-outline-danger remove-from-cart">&times;</button>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mt-2">
                        <div class="input-group input-group-sm" style="width: 120px;">
                            <button class="btn btn-outline-secondary change-quantity" type="button" data-change="-1">-</button>
                            <input type="number" class="form-control text-center quantity-input" value="${item.quantity}" min="1" aria-label="Cantidad">
                            <button class="btn btn-outline-secondary change-quantity" type="button" data-change="1">+</button>
                        </div>
                        <span class="fw-bold">S/ ${itemTotal.toFixed(2)}</span>
                    </div>
                `;
                cartItemsContainer.appendChild(cartItemElement);
            });
        }

        let totalFinal = subtotal;
        if (discountApplied && totalItems >= 3) {
            totalFinal = subtotal * 0.90;
            cartTotal.innerHTML = `<span class="text-decoration-line-through text-muted">S/ ${subtotal.toFixed(2)}</span> S/ ${totalFinal.toFixed(2)} <span class="badge bg-success">10% OFF</span>`;
        } else {
            cartTotal.textContent = subtotal.toFixed(2);
        }

        cartCount.textContent = totalItems;
    };

    const addToCart = (productId, name, price) => {
        const existingItem = cart.find(item => item.id === productId);

        if (existingItem) {
            existingItem.quantity++;
        } else {
            cart.push({ id: productId, name, price, quantity: 1 });
        }

        updateCartUI();
    };

    if (cartToggleButton) {
        cartToggleButton.addEventListener('click', () => toggleCart());
    }

    if (cartCloseButton) {
        cartCloseButton.addEventListener('click', () => toggleCart());
    }

    // Event listener para los botones "Agregar al carrito" en la vista de catálogo (modal de producto)
    document.querySelectorAll('.product-item .add-to-cart-btn').forEach(button => {
        button.addEventListener('click', (e) => {
            const productId = e.target.dataset.productId;
            const productElement = e.target.closest('.product-item');
            const name = productElement.querySelector('.card-title').textContent;
            const price = parseFloat(productElement.dataset.productPrice);
            addToCart(productId, name, price);
        });
    });

    // NUEVO: Event listener para los botones "Añadir" en las tarjetas de producto
    document.querySelectorAll('.add-to-cart-card-btn').forEach(button => {
        button.addEventListener('click', (e) => {
            const productId = e.target.dataset.productId;
            const productName = e.target.dataset.productName;
            const productPrice = parseFloat(e.target.dataset.productPrice);
            addToCart(productId, productName, productPrice);
            toggleCart(true); // Abrir el carrito al añadir desde la tarjeta
        });
    });


    cartItemsContainer.addEventListener('click', (e) => {
        const target = e.target;
        const cartItem = target.closest('.cart-item');
        if (!cartItem) return;

        const productId = cartItem.dataset.productId;

        if (target.classList.contains('remove-from-cart')) {
            removeFromCart(productId);
        }

        if (target.classList.contains('change-quantity')) {
            const change = parseInt(target.dataset.change, 10);
            const item = cart.find(i => i.id === productId);
            if (item) {
                updateQuantity(productId, item.quantity + change);
            }
        }
    });

    cartItemsContainer.addEventListener('change', (e) => {
        const target = e.target;
        const cartItem = target.closest('.cart-item');
        if (!cartItem || !target.classList.contains('quantity-input')) return;

        const productId = cartItem.dataset.productId;
        updateQuantity(productId, target.value);
    });

    const validateDni = () => {
        const dni = customerDniInput.value;
        const dniRegex = /^[0-9]{8}$/;
        if (dniRegex.test(dni)) {
            customerDniInput.classList.remove('is-invalid');
            dniValidationMessage.style.display = 'none';
            return true;
        } else {
            customerDniInput.classList.add('is-invalid');
            dniValidationMessage.style.display = 'block';
            return false;
        }
    };

    if (customerDniInput) {
        customerDniInput.addEventListener('input', () => {
            // Solo permite números
            customerDniInput.value = customerDniInput.value.replace(/[^0-9]/g, '');
            validateDni();
        });
    }

    if (btnFinalizarCompra) {
        btnFinalizarCompra.addEventListener('click', () => {
            const customerName = document.getElementById('customer-name').value.trim();

            if (!validateDni()) {
                alert('Por favor, corrige los errores en el formulario.');
                return;
            }

            const customerDni = customerDniInput.value;

            if (cart.length === 0) {
                alert('Tu carrito está vacío. Agrega productos antes de continuar.');
                return;
            }

            if (!customerName) {
                alert('Por favor, ingresa tu nombre para continuar.');
                return;
            }

            // Guardar datos del cliente en localStorage para el checkout
            const clienteData = {
                nombre: customerName,
                dni: customerDni
            };
            localStorage.setItem('clienteData', JSON.stringify(clienteData));

            // Guardar el carrito en localStorage para el checkout
            localStorage.setItem('cart', JSON.stringify(cart));

            // Aplicar descuento si corresponde
            let totalItems = 0;
            cart.forEach(item => {
                totalItems += item.quantity;
            });

            let hasDiscount = false;
            if (discountApplied && totalItems >= 3) {
                hasDiscount = true;
            }
            localStorage.setItem('discountApplied', hasDiscount);

            // Redirigir al checkout
            window.location.href = '/checkout';
        });
    }

    updateCartUI();

    // --- LÓGICA DEL MODAL DE DETALLES DEL PRODUCTO ---
    const productDetailModal = document.getElementById('productDetailModal');
    if (productDetailModal) {
        const modalProductName = document.getElementById('modalProductName');
        const modalProductDescription = document.getElementById('modalProductDescription');
        const modalProductPrice = document.getElementById('modalProductPrice');
        const modalProductStock = document.getElementById('modalProductStock');
        const productCarouselInner = document.getElementById('productCarouselInner');
        const modalAddToCartBtn = document.getElementById('modalAddToCartBtn');

        productDetailModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const productId = button.getAttribute('data-product-id');
            const productName = button.getAttribute('data-product-name');
            const productDescription = button.getAttribute('data-product-description');
            const productPrice = button.getAttribute('data-product-price');
            const productStock = parseInt(button.getAttribute('data-product-stock'), 10);
            const imagesAttr = button.getAttribute('data-product-images');
            const productImages = imagesAttr ? imagesAttr.split(',') : [];

            modalProductName.textContent = productName;
            modalProductDescription.textContent = productDescription;
            modalProductPrice.textContent = productPrice;
            modalProductStock.textContent = productStock;

            productCarouselInner.innerHTML = '';

            if (productImages.length > 0 && productImages[0]) {
                productImages.forEach((imageUrl, index) => {
                    const carouselItem = document.createElement('div');
                    carouselItem.classList.add('carousel-item');
                    if (index === 0) {
                        carouselItem.classList.add('active');
                    }
                    const img = document.createElement('img');
                    img.src = imageUrl;
                    img.classList.add('d-block', 'w-100');
                    img.alt = `${productName} - Imagen ${index + 1}`;
                    img.style.maxHeight = '300px';
                    img.style.objectFit = 'contain';
                    carouselItem.appendChild(img);
                    productCarouselInner.appendChild(carouselItem);
                });
            } else {
                const carouselItem = document.createElement('div');
                carouselItem.classList.add('carousel-item', 'active');
                const img = document.createElement('img');
                img.src = '/images/placeholder.png';
                img.classList.add('d-block', 'w-100');
                img.alt = 'Imagen no disponible';
                img.style.maxHeight = '300px';
                img.style.objectFit = 'contain';
                carouselItem.appendChild(img);
                productCarouselInner.appendChild(carouselItem);
            }

            // Lógica para el botón "Agregar al carrito"
            if (productStock > 0) {
                modalAddToCartBtn.disabled = false;
                modalAddToCartBtn.innerHTML = '<i class="bi bi-cart-plus-fill me-2"></i>Agregar al carrito';
                modalAddToCartBtn.onclick = () => {
                    addToCart(productId, productName, parseFloat(productPrice));
                    const bsModal = bootstrap.Modal.getInstance(productDetailModal);
                    bsModal.hide();
                    toggleCart();
                };
            } else {
                modalAddToCartBtn.disabled = true;
                modalAddToCartBtn.innerHTML = '<i class="bi bi-x-circle-fill me-2"></i>Producto Agotado';
            }
        });
    }


    // --- LÓGICA DE OPINIONES (NUEVO FLUJO DE VALIDACIÓN) ---
    const opinionesModal = document.getElementById('opinionesModal');

    if (opinionesModal) {
        const newOpinionSection = document.getElementById('new-opinion-section');
        const opinionesList = document.getElementById('opiniones-list');
        const opinionSeparator = document.getElementById('opinion-separator');
        const API_URL = 'http://localhost:3000';

        const cargarOpiniones = async () => {
            opinionesList.innerHTML = '<p>Cargando opiniones...</p>';
            try {
                const response = await fetch(`${API_URL}/opiniones-publicadas`);
                if (!response.ok) throw new Error('No se pudo conectar al servicio de opiniones.');

                const opiniones = await response.json();
                opinionesList.innerHTML = '';

                if (opiniones.length === 0) {
                    opinionesList.innerHTML = '<p class="text-center">Aún no hay opiniones. ¡Sé el primero en dejar una!</p>';
                } else {
                    opiniones.forEach(op => {
                        const opinionCard = document.createElement('div');
                        opinionCard.className = 'opinion-card';
                        const fecha = new Date(op.fecha).toLocaleString('es-ES', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' });
                        opinionCard.innerHTML = `
                            <img src="${op.foto}" alt="Foto de ${op.nombre}" class="opinion-author-photo">
                            <div class="opinion-content">
                                <div class="opinion-header">
                                    <span class="opinion-author-name">${op.nombre}</span>
                                    <span class="opinion-date">${fecha}</span>
                                </div>
                                <p class="opinion-text">"${op.opinion}"</p>
                            </div>
                        `;
                        opinionesList.appendChild(opinionCard);
                    });
                }
            } catch (error) {
                opinionesList.innerHTML = `<p class="text-danger text-center">${error.message}</p>`;
            }
        };

        const actualizarVistaDeOpinion = async () => {
            try {
                const response = await fetch(`${API_URL}/auth/check`, { credentials: 'include' });
                const authStatus = await response.json();

                if (authStatus.isAuthenticated) {
                    // VISTA 1: Usuario completamente autenticado -> Formulario para opinar
                    newOpinionSection.innerHTML = `
                        <form id="opinion-form-loggedin">
                            <div class="user-info">
                                <img src="${authStatus.user.foto}" alt="Tu foto de perfil" class="user-photo-form">
                                <span class="user-name">Publicando como ${authStatus.user.nombre}</span>
                                <a href="${API_URL}/auth/logout" class="btn btn-sm logout-btn-styled ms-auto">Cerrar sesión</a>
                            </div>
                            <div id="opinion-message" class="mb-3"></div>
                            <div class="mb-3">
                                <label for="opinion-texto" class="form-label">Tu Opinión</label>
                                <textarea class="form-control" id="opinion-texto" rows="3" required placeholder="Escribe tu opinión aquí..."></textarea>
                            </div>
                            <button type="submit" class="btn btn-primary w-100">Publicar Opinión</button>
                        </form>
                    `;
                    opinionSeparator.style.display = 'block';
                    document.getElementById('opinion-form-loggedin').addEventListener('submit', handleOpinionSubmit);

                } else if (authStatus.isDocumentVerified) {
                    // VISTA 2: Documento verificado, pero no logueado con Google -> Botón de Google
                    newOpinionSection.innerHTML = `
                        <div class="text-center">
                            <p class="text-success">¡Documento verificado!</p>
                            <p class="text-muted">Ahora, inicia sesión con Google para continuar.</p>
                            <a href="${API_URL}/auth/google" class="google-login-btn">
                                <img src="https://upload.wikimedia.org/wikipedia/commons/c/c1/Google_%22G%22_logo.svg" alt="Google logo">
                                <span>Continuar con Google</span>
                            </a>
                        </div>
                    `;
                    opinionSeparator.style.display = 'none';

                } else {
                    // VISTA 3: Usuario no ha hecho nada -> Formulario de DNI/RUC
                    newOpinionSection.innerHTML = `
                        <form id="document-verify-form">
                            <p class="text-muted text-center">Para opinar, primero debemos verificar que eres cliente.</p>
                            <div id="document-message" class="mb-3"></div>
                            <div class="mb-3">
                                <label for="documento-cliente" class="form-label">Ingresa tu DNI o RUC</label>
                                <input type="text" class="form-control" id="documento-cliente" required placeholder="DNI o RUC">
                            </div>
                            <button type="submit" class="btn btn-secondary w-100">Verificar Documento</button>
                        </form>
                    `;
                    opinionSeparator.style.display = 'none';
                    document.getElementById('document-verify-form').addEventListener('submit', handleDocumentSubmit);
                }
            } catch (error) {
                newOpinionSection.innerHTML = `<p class="text-danger">No se pudo verificar el estado de autenticación.</p>`;
            }
        };

        const handleDocumentSubmit = async (e) => {
            e.preventDefault();
            const documento = document.getElementById('documento-cliente').value;
            const messageDiv = document.getElementById('document-message');
            const submitButton = e.target.querySelector('button[type="submit"]');

            messageDiv.innerHTML = '';
            submitButton.disabled = true;
            submitButton.textContent = 'Verificando...';

            try {
                const response = await fetch(`${API_URL}/auth/verify-document`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ documento })
                });
                const result = await response.json();

                if (response.ok) {
                    // Si la verificación es exitosa, actualizamos la vista para mostrar el botón de Google
                    actualizarVistaDeOpinion();
                } else {
                    messageDiv.innerHTML = `<div class="alert alert-danger">${result.message}</div>`;
                    submitButton.disabled = false;
                    submitButton.textContent = 'Verificar Documento';
                }
            } catch (error) {
                messageDiv.innerHTML = `<div class="alert alert-danger">Error al conectar con el servicio.</div>`;
                submitButton.disabled = false;
                submitButton.textContent = 'Verificar Documento';
            }
        };

        const handleOpinionSubmit = async (e) => {
            e.preventDefault();
            const opinionTexto = document.getElementById('opinion-texto').value;
            const messageDiv = document.getElementById('opinion-message');

            try {
                const response = await fetch(`${API_URL}/opiniones`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ opinion: opinionTexto }) // Ya no enviamos el documento
                });
                const result = await response.json();

                if (response.ok) {
                    messageDiv.innerHTML = `<div class="alert alert-success">${result.message}</div>`;
                    document.getElementById('opinion-texto').value = '';
                    setTimeout(() => {
                        cargarOpiniones();
                        messageDiv.innerHTML = '';
                    }, 2000);
                } else {
                    messageDiv.innerHTML = `<div class="alert alert-danger">${result.message}</div>`;
                }
            } catch (error) {
                messageDiv.innerHTML = `<div class="alert alert-danger">Error al enviar la opinión.</div>`;
            }
        };

        opinionesModal.addEventListener('show.bs.modal', () => {
            cargarOpiniones();
            actualizarVistaDeOpinion();
        });

        // Abrir modal si la URL contiene el hash #opinionesModal
        if (window.location.hash === '#opinionesModal') {
            const modal = new bootstrap.Modal(opinionesModal);
            modal.show();
        }
    }

    // --- LÓGICA DEL JUEGO DE MEMORIA ---
    const memoryGameModal = document.getElementById('memoryGameModal');
    if (memoryGameModal) {
        const gameBoard = memoryGameModal.querySelector('.memory-game-board');
        const gameWinMessage = memoryGameModal.querySelector('#game-win-message');
        const claimDiscountBtn = document.getElementById('claim-discount-btn');

        const cardIcons = [
            'bi-cake2-fill',
            'bi-cupcake',
            'bi-gift-fill',
            'bi-heart-fill',
            'bi-star-fill',
            'bi-gem'
        ];
        let gameCards = [];
        let firstCard, secondCard;
        let lockBoard = false;
        let matchedPairs = 0;

        const shuffle = (array) => {
            array.sort(() => Math.random() - 0.5);
        };

        const createBoard = () => {
            gameBoard.innerHTML = '';
            gameWinMessage.style.display = 'none';
            matchedPairs = 0;
            gameCards = [...cardIcons, ...cardIcons];
            shuffle(gameCards);

            gameCards.forEach(iconClass => {
                const card = document.createElement('div');
                card.classList.add('memory-card');
                card.dataset.icon = iconClass;
                card.innerHTML = `
                    <div class="front-face">
                        <i class="bi bi-question-lg"></i>
                    </div>
                    <div class="back-face">
                        <i class="bi ${iconClass}"></i>
                    </div>
                `;
                gameBoard.appendChild(card);
                card.addEventListener('click', flipCard);
            });
        };

        function flipCard() {
            if (lockBoard) return;
            if (this === firstCard) return;

            this.classList.add('flipped');

            if (!firstCard) {
                firstCard = this;
                return;
            }

            secondCard = this;
            lockBoard = true;

            checkForMatch();
        }

        function checkForMatch() {
            const isMatch = firstCard.dataset.icon === secondCard.dataset.icon;
            isMatch ? disableCards() : unflipCards();
        }



        function disableCards() {
            firstCard.removeEventListener('click', flipCard);
            secondCard.removeEventListener('click', flipCard);
            matchedPairs++;
            resetBoard();

            if (matchedPairs === cardIcons.length) {
                setTimeout(() => {
                    gameWinMessage.style.display = 'block';
                }, 500);
            }
        }

        function unflipCards() {
            setTimeout(() => {
                firstCard.classList.remove('flipped');
                secondCard.classList.remove('flipped');
                resetBoard();
            }, 1200);
        }

        function resetBoard() {
            [firstCard, secondCard, lockBoard] = [null, null, false];
        }

        claimDiscountBtn.addEventListener('click', () => {
            discountApplied = true;
            const bsModal = bootstrap.Modal.getInstance(memoryGameModal);
            bsModal.hide();
            updateCartUI();
            toggleCart(true); // Forzar apertura del carrito
        });

        memoryGameModal.addEventListener('show.bs.modal', () => {
            createBoard();
        });
    }

    // --- SINCRONIZACIÓN ENTRE PESTAÑAS ---
    window.addEventListener('storage', function(event) {
        if (event.key === 'empresaActualizada') {
            fetch('/empresa/api/info')
                .then(response => {
                    if (!response.ok) throw new Error('Network response was not ok ' + response.statusText);
                    return response.json();
                })
                .then(empresa => {
                    // Actualizar Título
                    document.title = empresa.nombre || 'Catálogo';

                    // Actualizar Header
                    const logoImg = document.querySelector('.navbar-brand img');
                    const nombreSpan = document.querySelector('.navbar-brand span');
                    if (logoImg) logoImg.src = empresa.logoUrl || '';
                    if (nombreSpan) nombreSpan.textContent = empresa.nombre || 'Catálogo';

                    // Actualizar sección "Nosotros"
                    const nosotrosP = document.querySelector('#nosotros p');
                    if (nosotrosP) nosotrosP.textContent = empresa.nosotros || 'Descripción de la empresa no disponible.';

                    // Actualizar teléfono en botón de WhatsApp
                    const btnWhatsapp = document.getElementById('btn-finalizar-compra');
                    if (btnWhatsapp) btnWhatsapp.dataset.telefono = empresa.telefono || '';

                    // Lógica para reconstruir el carrusel
                    const carruselContainer = document.getElementById('productos-destacados');
                    if (carruselContainer) {
                        const track = carruselContainer.querySelector('.cover-flow-carousel-track');
                        if(track) {
                            track.innerHTML = ''; // Limpiar carrusel antiguo
                            if (empresa.productosDestacados && empresa.productosDestacados.length > 0) {
                                empresa.productosDestacados.forEach(producto => {
                                    const item = document.createElement('div');
                                    item.className = 'cover-flow-item';
                                    item.dataset.nombre = producto.nombre;
                                    item.dataset.precio = producto.precio.toFixed(2);
                                    item.dataset.descripcion = producto.descripcion || 'Sin descripción disponible.';

                                    const img = document.createElement('img');
                                    img.src = producto.foto || '/images/placeholder.png';
                                    img.alt = producto.nombre;

                                    item.appendChild(img);
                                    track.appendChild(item);
                                });
                                carruselContainer.style.display = 'block';
                                inicializarCarrusel(); // Re-inicializar la lógica del carrusel
                            } else {
                                carruselContainer.style.display = 'none'; // Ocultar si no hay productos
                            }
                        }
                    }
                })
                .catch(error => console.error('Error al refrescar datos de la empresa:', error));
        } else if (event.key === 'productosActualizados') {
            console.log('Productos actualizados en otra pestaña. No se recarga automáticamente para no interrumpir la sesión.');
            // Aquí se podría implementar una actualización parcial de productos si se desea,
            // pero por ahora evitamos una recarga completa que genera sobrecarga al cargar la página.
        }
    });
});