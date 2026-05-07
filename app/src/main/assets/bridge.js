// MER Runtime Bridge v0.2
// Injected into pages BEFORE extension scripts.
// Provides the `mer` API namespace for extensions.
//
// ARCHITECTURE: The bridge is a single Kotlin object ("MerNative").
// Before each extension's scripts run, we call MerNative.setActiveExtension(id)
// so that storage/log calls route to the correct extension namespace.
// The `mer.__setContext(id)` function is called by the injection engine.
//
// AI CALLS: Are async. We use a callback registry pattern:
// 1. JS generates a callback ID and stores the resolve/reject fns
// 2. Kotlin receives the callback ID with the request
// 3. Kotlin calls __merAiCallback(id, result, error) when done
// 4. The callback resolves the Promise
(function() {
    'use strict';

    // Guard against double-injection
    if (window.__mer_bridge_initialized) return;
    window.__mer_bridge_initialized = true;

    // Internal: current extension context
    var _currentExtId = '';

    // AI callback registry
    var _aiCallbacks = {};
    var _callbackCounter = 0;

    function _registerCallback(resolve, reject) {
        var id = 'cb_' + (++_callbackCounter) + '_' + Date.now();
        _aiCallbacks[id] = { resolve: resolve, reject: reject };
        return id;
    }

    // Global callback handler — called from Kotlin via evaluateJavascript
    window.__merAiCallback = function(callbackId, result, error) {
        var cb = _aiCallbacks[callbackId];
        if (!cb) return;
        delete _aiCallbacks[callbackId];
        if (error) {
            cb.reject(new Error(error));
        } else {
            cb.resolve(result);
        }
    };

    var mer = {
        // Internal context switcher — called by injection engine, not extensions
        __setContext: function(extensionId) {
            _currentExtId = extensionId;
            MerNative.setActiveExtension(extensionId);
            // Sync AI bridge context too
            if (typeof MerAiNative !== 'undefined') {
                try { MerAiNative.setActiveExtension(extensionId); } catch(e) {}
            }
        },

        storage: {
            get: function(key) {
                try {
                    var result = MerNative.storageGet(key);
                    if (result === 'null' || result === null) return null;
                    try { return JSON.parse(result); } catch(e) { return result; }
                } catch(e) {
                    return null;
                }
            },
            set: function(key, value) {
                try {
                    MerNative.storageSet(key, JSON.stringify(value));
                } catch(e) {
                    console.error('[MER] storage.set failed:', e);
                }
            },
            remove: function(key) {
                try {
                    MerNative.storageRemove(key);
                } catch(e) {
                    console.error('[MER] storage.remove failed:', e);
                }
            }
        },

        runtime: {
            getId: function() {
                return MerNative.getExtensionId();
            },
            hasPermission: function(perm) {
                try {
                    return MerNative.hasPermission(perm);
                } catch(e) {
                    return false;
                }
            }
        },

        log: {
            debug: function(msg) { MerNative.log('debug', String(msg)); },
            info: function(msg) { MerNative.log('info', String(msg)); },
            warn: function(msg) { MerNative.log('warn', String(msg)); },
            error: function(msg) { MerNative.log('error', String(msg)); }
        },

        overlay: {
            /**
             * Show a floating overlay panel on the page.
             * @param {Object} options - { title, content, html?, bottom?, right?, maxWidth? }
             * @returns {string} overlay element ID
             */
            show: function(options) {
                options = options || {};
                var id = 'mer-overlay-' + _currentExtId + '-' + Date.now();

                // Remove existing overlay from this extension
                var existing = document.querySelectorAll('[data-mer-overlay="' + _currentExtId + '"]');
                existing.forEach(function(el) { el.remove(); });

                var container = document.createElement('div');
                container.id = id;
                container.setAttribute('data-mer-overlay', _currentExtId);
                container.style.cssText = [
                    'position: fixed',
                    'bottom: ' + (options.bottom || '70px'),
                    'right: ' + (options.right || '16px'),
                    'max-width: ' + (options.maxWidth || '320px'),
                    'min-width: 200px',
                    'background: rgba(15, 23, 42, 0.95)',
                    'color: #f1f5f9',
                    'border-radius: 16px',
                    'box-shadow: 0 8px 32px rgba(0,0,0,0.4)',
                    'backdrop-filter: blur(20px)',
                    'font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
                    'font-size: 14px',
                    'z-index: 999999',
                    'overflow: hidden',
                    'animation: merOverlayIn 0.25s ease-out'
                ].join(';');

                // Inject animation keyframes if not already present
                if (!document.getElementById('mer-overlay-styles')) {
                    var styleEl = document.createElement('style');
                    styleEl.id = 'mer-overlay-styles';
                    styleEl.textContent = '@keyframes merOverlayIn { from { opacity: 0; transform: translateY(12px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }';
                    document.head.appendChild(styleEl);
                }

                // Header
                var header = document.createElement('div');
                header.style.cssText = 'display:flex;align-items:center;justify-content:space-between;padding:12px 14px 8px;border-bottom:1px solid rgba(255,255,255,0.08);';

                var title = document.createElement('span');
                title.style.cssText = 'font-weight:600;font-size:13px;color:#94a3b8;';
                title.textContent = options.title || 'MER';

                var closeBtn = document.createElement('span');
                closeBtn.style.cssText = 'cursor:pointer;font-size:18px;color:#64748b;padding:0 4px;line-height:1;';
                closeBtn.textContent = '\u00d7';
                closeBtn.onclick = function() { container.remove(); };

                header.appendChild(title);
                header.appendChild(closeBtn);
                container.appendChild(header);

                // Body
                var body = document.createElement('div');
                body.style.cssText = 'padding:10px 14px 14px;line-height:1.5;max-height:300px;overflow-y:auto;';

                if (typeof options.content === 'string') {
                    body.textContent = options.content;
                } else if (options.html) {
                    body.innerHTML = options.html;
                }

                container.appendChild(body);
                document.body.appendChild(container);

                return id;
            },

            /**
             * Update an existing overlay's content.
             * @param {string} overlayId - ID returned by show()
             * @param {Object} options - { content?, html?, title? }
             */
            update: function(overlayId, options) {
                var el = document.getElementById(overlayId);
                if (!el) return false;
                options = options || {};
                var body = el.querySelector('div:last-child');
                if (body) {
                    if (options.content) body.textContent = options.content;
                    else if (options.html) body.innerHTML = options.html;
                }
                if (options.title) {
                    var titleEl = el.querySelector('span:first-child');
                    if (titleEl) titleEl.textContent = options.title;
                }
                return true;
            },

            /**
             * Dismiss an overlay.
             */
            dismiss: function(overlayId) {
                if (overlayId) {
                    var el = document.getElementById(overlayId);
                    if (el) el.remove();
                } else {
                    var overlays = document.querySelectorAll('[data-mer-overlay="' + _currentExtId + '"]');
                    overlays.forEach(function(el) { el.remove(); });
                }
            }
        },

        ai: {
            /**
             * Ask a general AI question. Returns a Promise.
             * Requires "ai" permission.
             * @param {string} prompt
             * @returns {Promise<string>}
             */
            ask: function(prompt) {
                return new Promise(function(resolve, reject) {
                    try {
                        if (typeof MerAiNative === 'undefined') {
                            reject(new Error('AI service not available'));
                            return;
                        }
                        var cbId = _registerCallback(resolve, reject);
                        MerAiNative.ask(prompt, cbId);
                    } catch(e) {
                        reject(e);
                    }
                });
            },

            /**
             * Summarize the current page. Returns a Promise.
             * Extracts page text automatically.
             * @returns {Promise<string>}
             */
            summarize: function() {
                return new Promise(function(resolve, reject) {
                    try {
                        if (typeof MerAiNative === 'undefined') {
                            reject(new Error('AI service not available'));
                            return;
                        }
                        var pageText = document.body.innerText || '';
                        var url = window.location.href;
                        var cbId = _registerCallback(resolve, reject);
                        MerAiNative.summarize(pageText.substring(0, 10000), url, cbId);
                    } catch(e) {
                        reject(e);
                    }
                });
            },

            /**
             * Explain highlighted/selected text. Returns a Promise.
             * @param {string} text - The text to explain
             * @returns {Promise<string>}
             */
            explain: function(text) {
                return new Promise(function(resolve, reject) {
                    try {
                        if (typeof MerAiNative === 'undefined') {
                            reject(new Error('AI service not available'));
                            return;
                        }
                        var url = window.location.href;
                        var cbId = _registerCallback(resolve, reject);
                        MerAiNative.explain(text, url, cbId);
                    } catch(e) {
                        reject(e);
                    }
                });
            },

            /**
             * Extract structured data from page content. Returns a Promise.
             * @param {string} instruction - What to extract
             * @returns {Promise<string>}
             */
            extract: function(instruction) {
                return new Promise(function(resolve, reject) {
                    try {
                        if (typeof MerAiNative === 'undefined') {
                            reject(new Error('AI service not available'));
                            return;
                        }
                        var pageText = document.body.innerText || '';
                        var cbId = _registerCallback(resolve, reject);
                        MerAiNative.extract(pageText.substring(0, 10000), instruction, cbId);
                    } catch(e) {
                        reject(e);
                    }
                });
            },

            /**
             * Check if AI is configured and available.
             * @returns {boolean}
             */
            isAvailable: function() {
                try {
                    return typeof MerAiNative !== 'undefined' && MerAiNative.isConfigured();
                } catch(e) {
                    return false;
                }
            }
        }
    };

    // Freeze the API to prevent extension tampering
    Object.freeze(mer.storage);
    Object.freeze(mer.runtime);
    Object.freeze(mer.log);
    Object.freeze(mer.overlay);
    Object.freeze(mer.ai);
    Object.freeze(mer);

    window.mer = mer;
})();
