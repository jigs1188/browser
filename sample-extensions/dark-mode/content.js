// Dark Mode extension for MER
// Stores user preference and adds a toggle via the MER storage API
(function() {
    'use strict';

    // Check if dark mode was previously enabled
    var enabled = mer.storage.get('dark_mode_enabled');
    if (enabled === null) {
        enabled = true; // Default to enabled
        mer.storage.set('dark_mode_enabled', true);
    }

    if (enabled) {
        document.documentElement.classList.add('mer-dark-mode');
    }

    mer.log.info('Dark Mode extension loaded, enabled: ' + enabled);
})();
