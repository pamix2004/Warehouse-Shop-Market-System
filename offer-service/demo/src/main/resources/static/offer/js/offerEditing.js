document.addEventListener('DOMContentLoaded', () => {
    /**
     * EVENT DELEGATION:
     * We attach listeners to the document (or a static parent) so that
     * when pagination replaces table rows, the functionality remains.
     */

    // 1. Handle Button Clicks (Edit, Save, Cancel)
    document.addEventListener('click', (e) => {
        const editBtn = e.target.closest('.offer-edit-btn');
        const saveBtn = e.target.closest('.offer-save-btn');
        const cancelBtn = e.target.closest('.offer-cancel-btn');

        if (editBtn) startEdit(editBtn);
        if (saveBtn) saveRow(saveBtn);
        if (cancelBtn) cancelEdit(cancelBtn);
    });

    // 2. Handle State Dropdown Changes
    document.addEventListener('change', (e) => {
        if (e.target.classList.contains('offer-state-select')) {
            updateOfferState(e.target);
        }
    });
});

/**
 * FUNCTIONS FOR ROW EDITING (Price, Quantities)
 */

function startEdit(button) {
    const row = button.closest('tr');
    if (!row || row.dataset.editing === '1') return;

    row.dataset.editing = '1';
    const cells = row.querySelectorAll('td');

    // Store original values for "Cancel"
    row.dataset.origPrice = cells[1].textContent.trim();
    row.dataset.origAvail = cells[2].textContent.trim();
    row.dataset.origMin = cells[3].textContent.trim();

    // Transform cells to inputs
    cells[1].innerHTML = `<input type="number" step="0.01" class="form-control form-control-sm" value="${escapeAttr(row.dataset.origPrice)}">`;
    cells[2].innerHTML = `<input type="number" class="form-control form-control-sm" value="${escapeAttr(row.dataset.origAvail)}">`;
    cells[3].innerHTML = `<input type="number" class="form-control form-control-sm" value="${escapeAttr(row.dataset.origMin)}">`;

    // Swap buttons
    cells[4].innerHTML = `
        <button type="button" class="btn btn-success btn-sm offer-save-btn">Save</button>
        <button type="button" class="btn btn-secondary btn-sm offer-cancel-btn" style="margin-left:6px;">Cancel</button>
    `;
}

function cancelEdit(button) {
    const row = button.closest('tr');
    if (!row) return;

    const cells = row.querySelectorAll('td');
    cells[1].textContent = row.dataset.origPrice ?? '';
    cells[2].textContent = row.dataset.origAvail ?? '';
    cells[3].textContent = row.dataset.origMin ?? '';
    cells[4].innerHTML = `<button type="button" class="btn btn-primary btn-sm offer-edit-btn">Edit</button>`;

    row.dataset.editing = '0';
}

function saveRow(button) {
    const row = button.closest('tr');
    if (!row) return;

    const offerId = row.dataset.offerId;
    const cells = row.querySelectorAll('td');
    const price = cells[1].querySelector('input')?.value;
    const available = cells[2].querySelector('input')?.value;
    const minimal = cells[3].querySelector('input')?.value;

    if (price === '' || available === '' || minimal === '') {
        alert('Please fill all fields.');
        return;
    }

    const params = new URLSearchParams();
    params.append('offerId', offerId);
    params.append('price', price);
    params.append('availableQuantity', available);
    params.append('minimalQuantity', minimal);

    fetch('/offer/changeOffer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(response => {
        if (response.ok) {
            cells[1].textContent = price;
            cells[2].textContent = available;
            cells[3].textContent = minimal;
            cells[4].innerHTML = `<button type="button" class="btn btn-primary btn-sm offer-edit-btn">Edit</button>`;
            row.dataset.editing = '0';
        } else {
            return response.text().then(text => { throw new Error(text) });
        }
    })
    .catch(error => {
        alert('Failed to update offer: ' + error.message);
    });
}

/**
 * FUNCTION FOR STATE UPDATES
 */

function updateOfferState(selectElement) {
    const row = selectElement.closest('tr');
    const offerId = row.dataset.offerId;
    const newState = selectElement.value;

    // Visual feedback
    selectElement.classList.add('is-updating');
    selectElement.disabled = true;

    const params = new URLSearchParams();
    params.append('offerId', offerId);
    params.append('desiredState', newState);

    fetch('/offer/updateState', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(response => {
        if (!response.ok) throw new Error('Server returned error');
        console.log(`State updated to ${newState} for offer ${offerId}`);
    })
    .catch(error => {
        console.error('Update failed:', error);
        alert('Could not update status. Please try again.');
    })
    .finally(() => {
        selectElement.classList.remove('is-updating');
        selectElement.disabled = false;
    });
}

/**
 * HELPER
 */

function escapeAttr(str) {
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('"', '&quot;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
}