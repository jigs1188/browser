// AI Page Assistant — MER Sample Extension
// Demonstrates: mer.ai.*, mer.overlay.*, and mer.runtime.hasPermission()
(function() {
    'use strict';

    // Check AI availability before creating UI
    if (!mer.ai.isAvailable()) {
        mer.log.warn('AI not available — API key may not be configured');
        return;
    }

    // Create a floating action button
    var fab = document.createElement('div');
    fab.id = 'mer-ai-fab';
    fab.style.cssText = [
        'position: fixed',
        'bottom: 24px',
        'left: 16px',
        'width: 48px',
        'height: 48px',
        'background: linear-gradient(135deg, #6366f1, #14b8a6)',
        'border-radius: 50%',
        'display: flex',
        'align-items: center',
        'justify-content: center',
        'cursor: pointer',
        'z-index: 999998',
        'box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4)',
        'transition: transform 0.2s ease, box-shadow 0.2s ease',
        'user-select: none',
        'font-size: 22px'
    ].join(';');
    fab.textContent = '✨';
    fab.title = 'AI Assistant';

    fab.addEventListener('mouseenter', function() {
        fab.style.transform = 'scale(1.1)';
        fab.style.boxShadow = '0 6px 24px rgba(99, 102, 241, 0.5)';
    });
    fab.addEventListener('mouseleave', function() {
        fab.style.transform = 'scale(1)';
        fab.style.boxShadow = '0 4px 16px rgba(99, 102, 241, 0.4)';
    });

    var menuVisible = false;
    var currentOverlayId = null;

    fab.addEventListener('click', function() {
        if (menuVisible) {
            var menu = document.getElementById('mer-ai-menu');
            if (menu) menu.remove();
            menuVisible = false;
            return;
        }

        // Show action menu
        var menu = document.createElement('div');
        menu.id = 'mer-ai-menu';
        menu.style.cssText = [
            'position: fixed',
            'bottom: 80px',
            'left: 16px',
            'background: rgba(15, 23, 42, 0.95)',
            'border-radius: 14px',
            'box-shadow: 0 8px 32px rgba(0,0,0,0.4)',
            'backdrop-filter: blur(20px)',
            'overflow: hidden',
            'z-index: 999998',
            'min-width: 180px',
            'animation: merOverlayIn 0.2s ease-out'
        ].join(';');

        var actions = [
            { label: '📝 Summarize page', action: 'summarize' },
            { label: '❓ Ask about page', action: 'ask' }
        ];

        actions.forEach(function(item) {
            var btn = document.createElement('div');
            btn.style.cssText = 'padding:12px 16px;color:#f1f5f9;font-size:14px;font-family:-apple-system,sans-serif;cursor:pointer;transition:background 0.15s;';
            btn.textContent = item.label;
            btn.addEventListener('mouseenter', function() { btn.style.background = 'rgba(255,255,255,0.06)'; });
            btn.addEventListener('mouseleave', function() { btn.style.background = 'transparent'; });
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                menu.remove();
                menuVisible = false;
                handleAction(item.action);
            });
            menu.appendChild(btn);
        });

        document.body.appendChild(menu);
        menuVisible = true;
    });

    document.body.appendChild(fab);

    function handleAction(action) {
        // Show loading overlay
        if (currentOverlayId) mer.overlay.dismiss(currentOverlayId);
        currentOverlayId = mer.overlay.show({
            title: action === 'summarize' ? '📝 Summarizing...' : '🤖 Thinking...',
            content: 'Processing with AI...',
            bottom: '80px',
            right: '16px'
        });

        if (action === 'summarize') {
            mer.ai.summarize().then(function(result) {
                if (currentOverlayId) {
                    mer.overlay.update(currentOverlayId, {
                        title: '📝 Summary',
                        content: result
                    });
                }
            }).catch(function(err) {
                if (currentOverlayId) {
                    mer.overlay.update(currentOverlayId, {
                        title: '❌ Error',
                        content: err.message
                    });
                }
            });
        } else if (action === 'ask') {
            // Show input prompt
            if (currentOverlayId) mer.overlay.dismiss(currentOverlayId);
            var question = prompt('What do you want to know about this page?');
            if (!question) return;

            currentOverlayId = mer.overlay.show({
                title: '🤖 Thinking...',
                content: 'Processing: "' + question + '"',
                bottom: '80px'
            });

            mer.ai.ask(question + '\n\nPage content:\n' + document.body.innerText.substring(0, 5000))
                .then(function(result) {
                    if (currentOverlayId) {
                        mer.overlay.update(currentOverlayId, {
                            title: '💬 Answer',
                            content: result
                        });
                    }
                }).catch(function(err) {
                    if (currentOverlayId) {
                        mer.overlay.update(currentOverlayId, {
                            title: '❌ Error',
                            content: err.message
                        });
                    }
                });
        }
    }

    mer.log.info('AI Page Assistant loaded');
})();
