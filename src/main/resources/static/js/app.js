let stompClient = null;
let availableServicesData = []; // Store the full service data (id, name, capacity)
let myChoicesSortableInstance = null;
let availableServicesSortableInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    connectWebSocket();
    initializeSortableLists();
    // Optionally, load initial data if a volunteer ID is pre-filled
    // if (document.getElementById('volunteerId').value) {
    //     fetchInitialData();
    // }
});

function connectWebSocket() {
    const socket = new SockJS('/ws-assign'); // Ensure this path is correct for your Spring Boot SockJS endpoint
    stompClient = Stomp.over(socket);
    stompClient.connect({}, (frame) => {
        console.log('Connected to WebSocket: ' + frame);
        stompClient.subscribe('/topic/assignments', (message) => {
            showAssignmentResults(JSON.parse(message.body));
        });
        // If using STOMP, send CONNECT frame here if not handled by library automatically
        // This depends on your specific STOMP client and server setup.
        // The Tyrus client from JavaFX example needed manual STOMP frames.
        // Stomp.js usually handles this.
    }, (error) => {
        console.error('STOMP error: ' + error);
        setTimeout(connectWebSocket, 5000); // Try to reconnect
    });
}

function initializeSortableLists() {
    const availableListEl = document.getElementById('available-services-list');
    const choicesListEl = document.getElementById('my-choices-list');

    availableServicesSortableInstance = new Sortable(availableListEl, {
        group: {
            name: 'servicesGroup',
            pull: 'clone', // Clone items from available to choices
            put: false     // Don't allow dropping items back into available (or handle it if you want)
        },
        sort: false, // Don't allow sorting within the available services list
        animation: 150,
        onEnd: function (/**Event*/evt) {
            // If an item was cloned and moved to the choices list, it might appear there.
            // We'll handle adding to choices in the 'onAdd' of the choicesListEl.
            // This ensures the original in available-services-list is not removed.
            if (evt.to === choicesListEl && evt.from === availableListEl) {
                // The cloned item will be handled by choicesListEl's onAdd
            }
        }
    });

    myChoicesSortableInstance = new Sortable(choicesListEl, {
        group: 'servicesGroup', // Same group name to allow moving between them
        animation: 150,
        filter: '.remove-btn', // Clicks on .remove-btn won't start a drag
        onAdd: function (/**Event*/evt) {
            const itemEl = evt.item; // The dragged DOM Element
            const serviceId = itemEl.dataset.serviceId;

            if (choicesListEl.children.length > 5) {
                alert('You can select a maximum of 5 services.');
                itemEl.remove(); // Remove the item if it exceeds the limit
                // If cloned, we might need to remove from source if not handled by 'clone'
                // But since we're cloning, we don't want to remove from source.
            } else {
                // Check for duplicates (SortableJS might add it even if it's a duplicate by ID)
                let count = 0;
                for (let i = 0; i < choicesListEl.children.length; i++) {
                    if (choicesListEl.children[i].dataset.serviceId === serviceId) {
                        count++;
                    }
                }
                if (count > 1) { // If this add resulted in a duplicate
                    itemEl.remove(); // Remove the duplicate
                    alert("This service is already in your choices.");
                } else {
                    // Add remove button to the newly added item in choices
                    addRemoveButtonToChoiceItem(itemEl);
                    updateMyChoicesListOrder(); // Update internal array if needed
                }
            }
        },
        onUpdate: function (/**Event*/evt) {
            // Called when sorting within the same list
            updateMyChoicesListOrder();
        },
        onRemove: function(/**Event*/evt) {
            // Called when an item is removed from this list (e.g., dragged to another list)
            // If you drag out of choices list, it should be removed.
            updateMyChoicesListOrder();
        }
    });
}

function addRemoveButtonToChoiceItem(itemEl) {
    // Remove existing button if any (e.g. if item moved back and forth)
    const existingBtn = itemEl.querySelector('.remove-btn');
    if (existingBtn) existingBtn.remove();

    const removeBtn = document.createElement('button');
    removeBtn.textContent = 'X';
    removeBtn.classList.add('remove-btn');
    removeBtn.onclick = () => {
        itemEl.remove(); // Remove the <li> element from the DOM
        updateMyChoicesListOrder(); // Update the underlying data
    };
    itemEl.appendChild(removeBtn);
}


function fetchInitialData() {
    const volunteerId = document.getElementById('volunteerId').value;
    if (!volunteerId) {
        alert('Please enter a Volunteer ID.');
        return;
    }
    document.getElementById('pref-status').textContent = 'Loading...';

    fetch(`/api/preferences/initial-data?volunteerId=${volunteerId}`)
        .then(response => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            availableServicesData = data.services || [];
            const volunteerPrefs = data.volunteer?.preferredServicesRanks || {};

            const availableListEl = document.getElementById('available-services-list');
            availableListEl.innerHTML = ''; // Clear previous
            availableServicesData.forEach(service => {
                const li = document.createElement('li');
                li.textContent = `${service.name} (Cap: ${service.maxVolunteers})`;
                li.dataset.serviceId = service.id; // Crucial for SortableJS
                availableListEl.appendChild(li);
            });

            const choicesListEl = document.getElementById('my-choices-list');
            choicesListEl.innerHTML = ''; // Clear previous

            // Sort preferences by rank (key of the map) and populate choices
            const sortedPrefEntries = Object.entries(volunteerPrefs)
                .sort((a, b) => parseInt(a[0]) - parseInt(b[0])); // Sort by rank (key)

            sortedPrefEntries.forEach(([rank, serviceId]) => {
                const service = availableServicesData.find(s => s.id === serviceId);
                if (service) {
                    const li = document.createElement('li');
                    li.textContent = service.name; // Display only name for simplicity in choices
                    li.dataset.serviceId = service.id;
                    addRemoveButtonToChoiceItem(li); // Add remove button
                    choicesListEl.appendChild(li);
                }
            });
            document.getElementById('pref-status').textContent = 'Data loaded.';
        })
        .catch(error => {
            console.error('Error fetching initial data:', error);
            document.getElementById('pref-status').textContent = `Error: ${error.message}`;
        });
}

// This function is less critical if SortableJS directly manipulates the DOM elements
// that we read during submission. However, it can be useful for validation or other logic.
function updateMyChoicesListOrder() {
    const choicesListEl = document.getElementById('my-choices-list');
    const currentChoiceIds = Array.from(choicesListEl.children).map(li => li.dataset.serviceId);
    console.log("Current ranked choices:", currentChoiceIds);
    // You could store this `currentChoiceIds` in a global variable if needed elsewhere,
    // but for submission, we'll read directly from the DOM.
}


function submitPreferences() {
    const volunteerId = document.getElementById('volunteerId').value;
    if (!volunteerId) {
        alert('Please enter a Volunteer ID.');
        return;
    }

    const choicesListEl = document.getElementById('my-choices-list');
    const rankedServiceIds = Array.from(choicesListEl.children).map(li => li.dataset.serviceId);

    if (rankedServiceIds.length === 0) {
        // alert('Please select at least one service preference.');
        // return; // Allow submitting empty if desired, backend can validate
    }
    if (rankedServiceIds.length > 5) {
        alert('You have selected more than 5 services. Please remove some.');
        return;
    }


    const preferenceData = {
        // volunteerId: volunteerId, // Backend gets it from path variable
        rankedServiceIds: rankedServiceIds
    };

    document.getElementById('pref-status').textContent = 'Submitting...';
    fetch(`/api/preferences/${volunteerId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(preferenceData)
    })
        .then(response => {
            if (response.ok) return response.text();
            return response.text().then(text => { throw new Error(text || 'Submission failed') });
        })
        .then(data => {
            document.getElementById('pref-status').textContent = 'Preferences submitted successfully!';
            console.log('Preferences submitted:', data);
        })
        .catch(error => {
            document.getElementById('pref-status').textContent = 'Error: ' + error.message;
            console.error('Error submitting preferences:', error);
        });
}

function triggerOptimization() {
    document.getElementById('opt-status').textContent = 'Triggering optimization...';
    fetch('/api/assignment/optimize', { method: 'POST' })
        .then(response => response.text())
        .then(data => {
            document.getElementById('opt-status').textContent = data;
            console.log('Optimization triggered:', data);
        })
        .catch(error => {
            document.getElementById('opt-status').textContent = 'Error triggering optimization.';
            console.error('Error triggering optimization:', error);
        });
}

function showAssignmentResults(result) {
    console.log('Received assignment result:', result);
    const resultsTableBody = document.getElementById('assignment-table-body');
    resultsTableBody.innerHTML = ''; // Clear previous results

    if (result.message) {
        document.getElementById('opt-status').textContent = result.message;
    }

    if (result.assignments && result.assignments.length > 0) {
        result.assignments.forEach(assignment => {
            const row = resultsTableBody.insertRow();
            row.insertCell().textContent = assignment.volunteerId;
            const service = availableServicesData.find(s => s.id === assignment.serviceId);
            row.insertCell().textContent = service ? service.name : assignment.serviceId;
            row.insertCell().textContent = assignment.cost.toFixed(2);
            row.insertCell().textContent = assignment.preferenceRank === 0 ? 'Not Preferred' : assignment.preferenceRank;
        });
        document.getElementById('total-cost').textContent = result.totalCost.toFixed(2);
    } else {
        document.getElementById('total-cost').textContent = "N/A";
        const row = resultsTableBody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 4;
        cell.textContent = result.message || "No assignments available or error occurred.";
    }
}