/**
 * calendar.js - Time tracking calendar functionality for Employee Management System
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize FullCalendar
    initializeCalendar();

    // Initialize timer for active time tracking
    initializeTimer();

    // Setup event listeners
    setupEventListeners();
});

/**
 * Initialize the FullCalendar component
 */
function initializeCalendar() {
    const calendarEl = document.getElementById('calendar');

    if (!calendarEl) return;

    const employeeId = calendarEl.dataset.employeeId;

    // Initialize the calendar
    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
        },
        themeSystem: 'bootstrap5',
        height: 'auto',
        selectable: true,
        editable: false,
        dayMaxEvents: true,
        navLinks: true,

        // Load events from the API
        events: function(info, successCallback, failureCallback) {
            fetch(`/api/time-tracking/entries?employeeId=${employeeId}&start=${info.startStr}&end=${info.endStr}`)
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        // Transform the data to FullCalendar event format
                        const events = data.entries.map(entry => {
                            return {
                                id: entry.id,
                                title: entry.category || 'Work',
                                start: entry.startTime,
                                end: entry.endTime || null,
                                description: entry.description || '',
                                className: `fc-event-category-${(entry.category || 'other').toLowerCase().replace(/\s+/g, '-')}`,
                                extendedProps: {
                                    employeeId: entry.employeeId,
                                    employeeName: entry.employeeName,
                                    category: entry.category,
                                    durationMinutes: entry.durationMinutes,
                                    durationHours: entry.durationHours,
                                    approved: entry.approved
                                }
                            };
                        });

                        // Update total hours display if it exists
                        const totalHoursEl = document.getElementById('totalHours');
                        if (totalHoursEl) {
                            totalHoursEl.textContent = data.totalHours.toFixed(2);
                        }

                        successCallback(events);
                    } else {
                        failureCallback(new Error(data.message || 'Failed to load time entries'));
                    }
                })
                .catch(error => {
                    console.error('Error fetching time entries:', error);
                    failureCallback(error);
                });
        },

        // Event click to show details
        eventClick: function(info) {
            showTimeEntryDetails(info.event);
        },

        // Date click to add new entry
        dateClick: function(info) {
            openNewTimeEntryForm(info.date);
        }
    });

    calendar.render();

    // Store calendar instance in window object for later reference
    window.timeTrackingCalendar = calendar;
}

/**
 * Initialize the timer for active time tracking
 */
function initializeTimer() {
    const timerDisplay = document.getElementById('activeTimerDisplay');
    const employeeIdEl = document.getElementById('employeeId');

    if (!timerDisplay || !employeeIdEl) return;

    const employeeId = employeeIdEl.value;

    // Check for active time entry and update the timer
    function checkActiveTimeEntry() {
        fetch(`/api/time-tracking/active-entry?employeeId=${employeeId}`)
            .then(response => response.json())
            .then(data => {
                if (data.success && data.hasActiveEntry) {
                    updateTimerDisplay(data.activeEntry.startTime, timerDisplay);

                    // Show active time tracking section
                    document.getElementById('activeTimeTrackingSection').classList.remove('d-none');

                    // Update entry details
                    document.getElementById('activeEntryCategory').textContent = data.activeEntry.category || 'Work';
                    document.getElementById('activeEntryStartTime').textContent = formatDateTime(data.activeEntry.startTime);

                    if (data.activeEntry.description) {
                        document.getElementById('activeEntryDescription').textContent = data.activeEntry.description;
                        document.getElementById('activeEntryDescriptionSection').classList.remove('d-none');
                    } else {
                        document.getElementById('activeEntryDescriptionSection').classList.add('d-none');
                    }

                    // Disable start button, enable stop button
                    document.getElementById('startTimeTracking').disabled = true;
                    document.getElementById('stopTimeTracking').disabled = false;
                } else {
                    // Hide active time tracking section
                    document.getElementById('activeTimeTrackingSection').classList.add('d-none');

                    // Enable start button, disable stop button
                    document.getElementById('startTimeTracking').disabled = false;
                    document.getElementById('stopTimeTracking').disabled = true;
                }
            })
            .catch(error => {
                console.error('Error checking active time entry:', error);
            });
    }

    // Check active time entry initially and then every minute
    checkActiveTimeEntry();
    setInterval(checkActiveTimeEntry, 60000);
}

/**
 * Update the timer display with elapsed time
 */
function updateTimerDisplay(startTime, timerDisplay) {
    // Update the timer display every second
    function updateTimer() {
        const start = new Date(startTime);
        const now = new Date();
        const elapsedMilliseconds = now - start;

        // Calculate hours, minutes, seconds
        const hours = Math.floor(elapsedMilliseconds / (1000 * 60 * 60));
        const minutes = Math.floor((elapsedMilliseconds % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((elapsedMilliseconds % (1000 * 60)) / 1000);

        // Format as HH:MM:SS
        const formattedTime =
            String(hours).padStart(2, '0') + ':' +
            String(minutes).padStart(2, '0') + ':' +
            String(seconds).padStart(2, '0');

        timerDisplay.textContent = formattedTime;
    }

    // Update immediately and then every second
    updateTimer();

    // Clear any existing interval
    if (window.timerInterval) {
        clearInterval(window.timerInterval);
    }

    // Set new interval
    window.timerInterval = setInterval(updateTimer, 1000);
}

/**
 * Setup event listeners for time tracking actions
 */
function setupEventListeners() {
    // Start time tracking button
    const startButton = document.getElementById('startTimeTracking');
    if (startButton) {
        startButton.addEventListener('click', function() {
            const employeeId = document.getElementById('employeeId').value;
            const category = document.getElementById('timeEntryCategory').value;
            const description = document.getElementById('timeEntryDescription').value;

            startTimeTracking(employeeId, category, description);
        });
    }

    // Stop time tracking button
    const stopButton = document.getElementById('stopTimeTracking');
    if (stopButton) {
        stopButton.addEventListener('click', function() {
            const employeeId = document.getElementById('employeeId').value;
            stopTimeTracking(employeeId);
        });
    }

    // New time entry form
    const newTimeEntryForm = document.getElementById('newTimeEntryForm');
    if (newTimeEntryForm) {
        newTimeEntryForm.addEventListener('submit', function(event) {
            event.preventDefault();
            submitNewTimeEntry();
        });
    }
}

/**
 * Start time tracking
 */
function startTimeTracking(employeeId, category, description) {
    // Create form data
    const formData = new URLSearchParams();
    formData.append('employeeId', employeeId);
    formData.append('category', category);
    if (description) formData.append('description', description);

    // Send request to start time tracking
    fetch('/api/time-tracking/start', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Show success message
            showNotification('Time tracking started successfully!', 'success');

            // Refresh calendar and timer
            if (window.timeTrackingCalendar) {
                window.timeTrackingCalendar.refetchEvents();
            }

            // Check active time entry
            initializeTimer();

            // Clear form
            document.getElementById('timeEntryDescription').value = '';
        } else {
            showNotification(data.message || 'Failed to start time tracking', 'error');
        }
    })
    .catch(error => {
        console.error('Error starting time tracking:', error);
        showNotification('An error occurred while starting time tracking', 'error');
    });
}

/**
 * Stop time tracking
 */
function stopTimeTracking(employeeId) {
    // Create form data
    const formData = new URLSearchParams();
    formData.append('employeeId', employeeId);

    // Send request to stop time tracking
    fetch('/api/time-tracking/stop', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Show success message
            showNotification('Time tracking stopped successfully!', 'success');

            // Refresh calendar
            if (window.timeTrackingCalendar) {
                window.timeTrackingCalendar.refetchEvents();
            }

            // Check active time entry
            initializeTimer();

            // Stop timer
            if (window.timerInterval) {
                clearInterval(window.timerInterval);
                window.timerInterval = null;
            }
        } else {
            showNotification(data.message || 'Failed to stop time tracking', 'error');
        }
    })
    .catch(error => {
        console.error('Error stopping time tracking:', error);
        showNotification('An error occurred while stopping time tracking', 'error');
    });
}

/**
 * Show time entry details in a modal
 */
function showTimeEntryDetails(event) {
    // Get the modal
    const modal = new bootstrap.Modal(document.getElementById('timeEntryDetailsModal'));

    // Set the title
    document.getElementById('timeEntryDetailsTitle').textContent = event.title;

    // Set the details
    const startTime = formatDateTime(event.start);
    const endTime = event.end ? formatDateTime(event.end) : 'In progress';
    const duration = event.extendedProps.durationHours
        ? `${event.extendedProps.durationHours.toFixed(2)} hours`
        : 'In progress';

    document.getElementById('timeEntryDetailsStartTime').textContent = startTime;
    document.getElementById('timeEntryDetailsEndTime').textContent = endTime;
    document.getElementById('timeEntryDetailsDuration').textContent = duration;

    // Set the description if available
    if (event.description) {
        document.getElementById('timeEntryDetailsDescription').textContent = event.description;
        document.getElementById('timeEntryDetailsDescriptionSection').classList.remove('d-none');
    } else {
        document.getElementById('timeEntryDetailsDescriptionSection').classList.add('d-none');
    }

    // Set approval status
    const approvalBadge = document.getElementById('timeEntryDetailsApproval');
    if (event.extendedProps.approved) {
        approvalBadge.textContent = 'Approved';
        approvalBadge.classList.remove('bg-warning');
        approvalBadge.classList.add('bg-success');
    } else {
        approvalBadge.textContent = 'Pending Approval';
        approvalBadge.classList.remove('bg-success');
        approvalBadge.classList.add('bg-warning');
    }

    // Set delete button action
    const deleteButton = document.getElementById('deleteTimeEntryButton');
    if (deleteButton) {
        deleteButton.onclick = function() {
            if (confirm('Are you sure you want to delete this time entry?')) {
                window.location.href = `/user/time-tracking/delete/${event.id}`;
            }
        };
    }

    // Show the modal
    modal.show();
}

/**
 * Open form to add a new time entry
 */
function openNewTimeEntryForm(date) {
    // Set the date and time in the form
    const startDateTimeField = document.getElementById('startTime');
    const endDateTimeField = document.getElementById('endTime');

    if (startDateTimeField && endDateTimeField) {
        // Format date for datetime-local input (YYYY-MM-DDTHH:MM)
        const formattedDate = date.toISOString().substr(0, 16);

        startDateTimeField.value = formattedDate;

        // Set end time to 1 hour after start time by default
        const endDate = new Date(date);
        endDate.setHours(endDate.getHours() + 1);
        endDateTimeField.value = endDate.toISOString().substr(0, 16);

        // Open the modal
        const modal = new bootstrap.Modal(document.getElementById('newTimeEntryModal'));
        modal.show();
    }
}

/**
 * Submit a new time entry form
 */
function submitNewTimeEntry() {
    const form = document.getElementById('newTimeEntryForm');

    // Submit the form
    form.submit();
}

/**
 * Format date and time for display
 */
function formatDateTime(dateTimeStr) {
    const date = new Date(dateTimeStr);

    return date.toLocaleString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Show notification message
 */
function showNotification(message, type = 'info') {
    // Create the toast container if it doesn't exist
    let toastContainer = document.querySelector('.toast-container');

    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.classList.add('toast-container', 'position-fixed', 'bottom-0', 'end-0', 'p-3');
        document.body.appendChild(toastContainer);
    }

    // Create toast element
    const toastEl = document.createElement('div');
    toastEl.classList.add('toast', 'align-items-center', 'text-white');

    // Set background color based on type
    if (type === 'success') {
        toastEl.classList.add('bg-success');
    } else if (type === 'error') {
        toastEl.classList.add('bg-danger');
    } else {
        toastEl.classList.add('bg-primary');
    }

    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    // Create toast content
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;

    // Add to container
    toastContainer.appendChild(toastEl);

    // Initialize the toast
    const toast = new bootstrap.Toast(toastEl, {
        autohide: true,
        delay: 5000
    });

    // Show the toast
    toast.show();

    // Remove from DOM after it's hidden
    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}