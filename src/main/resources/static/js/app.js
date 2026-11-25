(function () {
    'use strict';

    function initMenu() {
        const toggle = document.getElementById('menuToggle');
        const sidebar = document.getElementById('sidebar');
        if (toggle && sidebar) {
            toggle.addEventListener('click', function () {
                sidebar.classList.toggle('open');
            });
        }

        document.querySelectorAll('[data-nav]').forEach(function (link) {
            const ruta = new URL(link.href).pathname;
            if (window.location.pathname === ruta ||
                (ruta !== '/' && window.location.pathname.startsWith(ruta))) {
                link.classList.add('active');
            }
        });
    }

    /** Las peticiones AJAX mismo origen reutilizan el token CSRF del layout. */
    function initCsrfFetch() {
        const metaToken = document.querySelector('meta[name="_csrf"]');
        const metaHeader = document.querySelector('meta[name="_csrf_header"]');
        if (!metaToken || !metaToken.content || typeof window.fetch !== 'function') {
            return;
        }
        const cabecera = metaHeader && metaHeader.content ? metaHeader.content : 'X-CSRF-TOKEN';
        const fetchOriginal = window.fetch;
        window.fetch = function (entrada, opciones) {
            const config = opciones || {};
            const metodo = String(config.method || (entrada && entrada.method) || 'GET').toUpperCase();
            if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(metodo)) {
                return fetchOriginal(entrada, config);
            }
            const headers = new Headers(config.headers || (entrada && entrada.headers) || {});
            headers.set(cabecera, metaToken.content);
            return fetchOriginal(entrada, Object.assign({}, config, {headers: headers}));
        };
    }

    function initConfirmaciones() {
        document.querySelectorAll('[data-confirm]').forEach(function (boton) {
            boton.addEventListener('click', function (evento) {
                if (!window.confirm(boton.getAttribute('data-confirm'))) {
                    evento.preventDefault();
                }
            });
        });
    }

    function initPublicaciones() {
        const tipo = document.getElementById('tipo');
        if (!tipo) {
            return;
        }
        const fieldsets = Array.from(document.querySelectorAll('fieldset[data-tipos]'));

        function actualizar() {
            fieldsets.forEach(function (fieldset) {
                const permitidos = fieldset.getAttribute('data-tipos').split('|');
                const activo = permitidos.indexOf(tipo.value) !== -1;
                fieldset.hidden = !activo;
                fieldset.disabled = !activo;
            });
        }

        tipo.addEventListener('change', actualizar);
        document.getElementById('formPublicacion').addEventListener('submit', function (evento) {
            if (!tipo.value) {
                evento.preventDefault();
                window.alert('Seleccione el tipo de publicación.');
            }
        });
        actualizar();
    }

    document.addEventListener('DOMContentLoaded', function () {
        initMenu();
        initCsrfFetch();
        initConfirmaciones();
        initPublicaciones();
    });
})();
