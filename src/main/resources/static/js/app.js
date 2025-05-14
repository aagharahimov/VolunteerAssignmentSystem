let stompClient = null;
let availableServices = [];
let myChoicesList = []; // Array of service IDs

document.addEventListener('DOMContentLoaded', (event) => {
    connectWebSocket();
    // Make lists sortable/draggable (basic example, consider a library like SortableJS)
    makeServicesDraggable();
    makeChoicesDroppableAndSortable();
});

function connectWebSocket() {
    const socket = new SockJS('/ws-assign');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected to WebSocket: ' + frame);
        stompClient.subscribe('/topic/assignments', function (message) {
            showAssignmentResults(JSON.parse(message.body));
        });
    }, function(error) {
        console.error('STOMP error: ' + error);
        // You might want to implement reconnection logic here
        setTimeout(connectWebSocket, 5000); // Try to reconnect after 5 seconds
    });
}

function fetchInitialData() {
    const volunteerId = document.getElementById('volunteerId').value;
    if (!volunteerId) {
        alert('Please enter a Volunteer ID.');
        return;
    }
    fetch(`/api/preferences/initial-data?volunteerId=${volunteerId}`)
        .then(response => response.json())
        .then(data => {
            availableServices = data.services || [];
            const volunteerPrefs = data.volunteer?.preferredServicesRanks || {};

            const servicesListDiv = document.getElementById('services-list');
            servicesListDiv.innerHTML = '<h3>Available Services (Drag to \'My Choices\' below)</h3><ul></ul>'; // Clear previous
            const ul = servicesListDiv.querySelector('ul');
            availableServices.forEach(service => {
                const li = document.createElement('li');
                li.textContent = `${service.name} (Capacity: ${service.maxVolunteers})`;
                li.dataset.serviceId = service.id;
                li.draggable = true;
                li.id = `service-item-${service.id}`;
                ul.appendChild(li);
            });

            // Populate my choices if they exist
            myChoicesList = [];
            const choicesUl = document.getElementById('my-choices-list');
            choicesUl.innerHTML = ''; // Clear previous

            // Sort preferences by rank
            const sortedPrefs = Object.entries(volunteerPrefs)
                .sort(([,aRank],[ ,bRank]) => aRank - bRank) // Sort by rank (value in map)
                .map(([rank, serviceId]) => ({ rank: parseInt(rank), serviceId: serviceId }));


            // Correctly extract ranked service IDs from the volunteer's data
            const rankedServiceIds = [];
            if (data.volunteer && data.volunteer.preferredServicesRanks) {
                const prefEntries = Object.entries(data.volunteer.preferredServicesRanks);
                prefEntries.sort((a, b) => a[0] - b[0]); // Sort by rank (key)
                prefEntries.forEach(([rank, serviceId]) => rankedServiceIds.push(serviceId));
            }


            myChoicesList = [...rankedServiceIds]; // Update myChoicesList
            updateMyChoicesDisplay();

            makeServicesDraggable(); // Re-apply draggable to new items
        })
        .catch(error => console.error('Error fetching initial data:', error));
}


function makeServicesDraggable() {
    document.querySelectorAll('#services-list li').forEach(item => {
        item.addEventListener('dragstart', handleDragStart);
    });
}

function makeChoicesDroppableAndSortable() {
    const choicesUl = document.getElementById('my-choices-list');
    choicesUl.addEventListener('dragover', handleDragOver);
    choicesUl.addEventListener('drop', handleDropOnChoices);

    // For re-ordering within choices (simplified)
    choicesUl.addEventListener('dragstart', (event) => {
        if (event.target.tagName === 'LI') {
            event.dataTransfer.setData('text/plain', event.target.dataset.serviceId);
            event.dataTransfer.effectAllowed = 'move';
        }
    });
}

function handleDragStart(event) {
    event.dataTransfer.setData('text/plain', event.target.dataset.serviceId);
    event.dataTransfer.effectAllowed = 'copy';
}

function handleDragOver(event) {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
}

function handleDropOnChoices(event) {
    event.preventDefault();
    const serviceId = event.dataTransfer.getData('text/plain');
    const serviceName = availableServices.find(s => s.id === serviceId)?.name;

    if (serviceId && serviceName && !myChoicesList.includes(serviceId) && myChoicesList.length < 5) {
        myChoicesList.push(serviceId);
        updateMyChoicesDisplay();
    } else if (myChoicesList.length >= 5) {
        alert('You can select a maximum of 5 services.');
    }
}

function updateMyChoicesDisplay() {
    const choicesUl = document.getElementById('my-choices-list');
    choicesUl.innerHTML = ''; // Clear and redraw
    myChoicesList.forEach((serviceId, index) => {
        const service = availableServices.find(s => s.id === serviceId);
        if (service) {
            const li = document.createElement('li');
            li.textContent = `${index + 1}. ${service.name}`;
            li.dataset.serviceId = serviceId;
            li.draggable = true; // For re-ordering
            // Add a remove button
            const removeBtn = document.createElement('button');
            removeBtn.textContent = 'X';
            removeBtn.style.marginLeft = '10px';
            removeBtn.onclick = () => removeChoice(serviceId);
            li.appendChild(removeBtn);
            choicesUl.appendChild(li);
        }
    });
    makeChoicesDroppableAndSortable(); // Re-apply draggable for items in choices list
}

function removeChoice(serviceIdToRemove) {
    myChoicesList = myChoicesList.filter(id => id !== serviceIdToRemove);
    updateMyChoicesDisplay();
}


function submitPreferences() {
    const volunteerId = document.getElementById('volunteerId').value;
    if (!volunteerId) {
        alert('Please enter a Volunteer ID.');
        return;
    }
    if (myChoicesList.length === 0) {
        alert('Please select at least one service preference.');
        return;
    }

    const preferenceData = {
        volunteerId: volunteerId,
        rankedServiceIds: myChoicesList
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
            document.getElementById('pref-status').textContent = 'Preferences submitted: ' + data;
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
            const serviceName = availableServices.find(s => s.id === assignment.serviceId)?.name || assignment.serviceId;
            row.insertCell().textContent = serviceName;
            row.insertCell().textContent = assignment.cost.toFixed(2);
            row.insertCell().textContent = assignment.preferenceRank === 0 ? 'Not Preferred' : assignment.preferenceRank;
        });
        document.getElementById('total-cost').textContent = result.totalCost.toFixed(2);
    } else {
        document.getElementById('total-cost').textContent = "N/A";
        const row = resultsTableBody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 4;
        cell.textContent = result.message || "No assignments available.";
    }
}