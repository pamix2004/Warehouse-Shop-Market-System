

document.addEventListener('DOMContentLoaded', () => {

    // 1. Toggle New Product Section
    const productSelect = document.getElementById('productSelect');
    const newProductSection = document.getElementById('newProductSection');
    if (productSelect && newProductSection) {
        const toggle = () => newProductSection.style.display = (productSelect.value === "") ? 'block' : 'none';
        productSelect.addEventListener('change', toggle);
        toggle();
    }

    // 2. AJAX Submission (Wholesaler)
    const form = document.getElementById('addOfferForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            const formData = new FormData(this);
            const errorDiv = document.getElementById('ajax-error-container');
            const errorText = document.getElementById('ajax-error-text');

            fetch(this.action, {
                method: 'POST',
                body: formData
                // Note: X-User-Id is omitted; injected by API Gateway
            })
            .then(async response => {
                if (response.ok) {
                    window.location.reload();
                } else {
                    const msg = await response.text();
                    if (errorText) errorText.textContent = msg;
                    if (errorDiv) errorDiv.style.display = 'block';
                }
            })
            .catch(() => {
                if (errorText) errorText.textContent = "Connection error.";
                if (errorDiv) errorDiv.style.display = 'block';
            });
        });
    }

    // 3. Pagination Logic
    function setupPagination(tableId, prevId, nextId, infoId, sizeId) {
        const table = document.getElementById(tableId);
        if (!table) return;

        const rows = Array.from(table.querySelectorAll("tbody tr"));
        let page = 1, pageSize = 25;

        const prevBtn = document.getElementById(prevId);
        const nextBtn = document.getElementById(nextId);
        const info = document.getElementById(infoId);
        const sizeSel = document.getElementById(sizeId);

        function render() {
            const total = Math.ceil(rows.length / pageSize) || 1;
            page = Math.min(Math.max(page, 1), total);
            rows.forEach((r, i) => r.style.display = (i >= (page-1)*pageSize && i < page*pageSize) ? "" : "none");

            if (info) info.textContent = `Page ${page} / ${total}`;
            if (prevBtn) prevBtn.disabled = (page === 1);
            if (nextBtn) nextBtn.disabled = (page === total);
        }

        if (prevBtn) prevBtn.onclick = () => { page--; render(); };
        if (nextBtn) nextBtn.onclick = () => { page++; render(); };
        if (sizeSel) sizeSel.onchange = (e) => { pageSize = parseInt(e.target.value); page = 1; render(); };

        render();
    }

    // Initialize Paginations
    setupPagination('wholesalerTable', 'wPrevBtn', 'wNextBtn', 'wPageInfo', 'wPageSize');
    setupPagination('storeTable', 'prevBtn', 'nextBtn', 'pageInfo', 'pageSize');
});