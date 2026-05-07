// Word Counter extension for MER
// Shows a floating badge with the word count of the page
(function() {
    'use strict';

    function countWords() {
        var text = document.body.innerText || '';
        var words = text.trim().split(/\s+/).filter(function(w) { return w.length > 0; });
        return words.length;
    }

    function createBadge() {
        var badge = document.createElement('div');
        badge.id = 'mer-word-counter';
        badge.style.cssText = [
            'position: fixed',
            'bottom: 20px',
            'right: 20px',
            'background: rgba(99, 102, 241, 0.9)',
            'color: white',
            'padding: 8px 14px',
            'border-radius: 20px',
            'font-family: -apple-system, BlinkMacSystemFont, sans-serif',
            'font-size: 13px',
            'font-weight: 500',
            'z-index: 99999',
            'box-shadow: 0 4px 12px rgba(0,0,0,0.3)',
            'backdrop-filter: blur(10px)',
            'cursor: pointer',
            'transition: opacity 0.3s ease',
            'user-select: none'
        ].join(';');

        badge.addEventListener('click', function() {
            badge.style.opacity = badge.style.opacity === '0.3' ? '1' : '0.3';
        });

        document.body.appendChild(badge);
        return badge;
    }

    var badge = createBadge();
    var count = countWords();
    badge.textContent = count.toLocaleString() + ' words';

    mer.log.info('Word count: ' + count);
})();
