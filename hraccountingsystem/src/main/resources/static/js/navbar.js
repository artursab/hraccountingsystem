/**
 * navbar.js - JavaScript for the modern navbar
 * Created: May 2025
 */

document.addEventListener('DOMContentLoaded', function() {
    // Mobile menu toggle
    initMobileMenu();

    // Dropdown functionality
    initDropdowns();

    // Add active class to current nav item
    highlightCurrentPage();

    // Initialize notifications
    initNotifications();

    // Sticky navbar effect
    initStickyNavbar();
});

/**
 * Initialize the mobile menu toggle
 */
function initMobileMenu() {
    const menuToggle = document.getElementById('menuToggle');
    const navbarMenu = document.getElementById('navbarMenu');

    if (menuToggle && navbarMenu) {
        menuToggle.addEventListener('click', function() {
            navbarMenu.classList.toggle('active');

            // Change icon based on menu state
            const icon = menuToggle.querySelector('i');
            if (navbarMenu.classList.contains('active')) {
                icon.classList.remove('fa-bars');
                icon.classList.add('fa-times');
            } else {
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            }
        });

        // Close menu when clicking outside
        document.addEventListener('click', function(event) {
            if (!menuToggle.contains(event.target) && !navbarMenu.contains(event.target)) {
                navbarMenu.classList.remove('active');
                const icon = menuToggle.querySelector('i');
                icon.classList.remove('fa-times');
                icon.classList.add('fa-bars');
            }
        });
    }
}

/**
 * Initialize dropdown menus
 */
function initDropdowns() {
    // For desktop devices, we're using CSS hover
    // For mobile devices, we'll use click events
    if (window.innerWidth <= 992) {
        const dropdownTriggers = document.querySelectorAll('.dropdown-trigger');

        dropdownTriggers.forEach(trigger => {
            trigger.addEventListener('click', function(e) {
                e.preventDefault();
                const dropdown = this.nextElementSibling;

                // Close all other dropdowns
                document.querySelectorAll('.dropdown-content.active').forEach(drop => {
                    if (drop !== dropdown) {
                        drop.classList.remove('active');
                    }
                });

                // Toggle current dropdown
                dropdown.classList.toggle('active');
            });
        });
    }
}

/**
 * Highlight the current page in the navigation
 */
function highlightCurrentPage() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-link');

    navLinks.forEach(link => {
        const href = link.getAttribute('href');

        // Skip empty or hash links
        if (!href || href === '#') return;

        // Check if the current path matches the link's href
        if (currentPath === href ||
            (href !== '/' && currentPath.startsWith(href))) {
            link.classList.add('active');
        }
    });
}

/**
 * Initialize notifications functionality
 */
function initNotifications() {
    const notificationButton = document.querySelector('.notification-indicator .navbar-link');

    if (notificationButton) {
        notificationButton.addEventListener('click', function(e) {
            e.preventDefault();

            // In a real application, you'd show a notification panel
            // For now, just remove the badge
            const badge = this.querySelector('.notification-badge');
            if (badge) {
                badge.style.display = 'none';
            }

            // You could show a dropdown with notifications here
            alert('You have 3 new notifications!');
        });
    }
}

/**
 * Add sticky behavior to navbar
 */
function initStickyNavbar() {
    const navbar = document.querySelector('.modern-navbar');
    const navbarHeight = navbar.offsetHeight;
    let lastScrollTop = 0;

    window.addEventListener('scroll', function() {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;

        // Add shadow when scrolling down
        if (scrollTop > 10) {
            navbar.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.1)';
        } else {
            navbar.style.boxShadow = 'none';
        }

        // Hide navbar when scrolling down, show when scrolling up
        if (scrollTop > navbarHeight && scrollTop > lastScrollTop) {
            // Scrolling down
            navbar.style.transform = 'translateY(-100%)';
        } else {
            // Scrolling up
            navbar.style.transform = 'translateY(0)';
        }

        lastScrollTop = scrollTop;
    });
}

/**
 * Close alert banners
 */
function closeAlert(element) {
    if (element && element.parentElement) {
        element.parentElement.style.display = 'none';
    }
}