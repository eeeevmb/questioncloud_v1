declare global {
  interface Window {
    MathJax?: any;
  }
}

let loaderPromise: Promise<void> | null = null;

export function loadMathJax(): Promise<void> {
  if (window.MathJax?.typesetPromise) {
    return window.MathJax.startup?.promise ?? Promise.resolve();
  }
  if (loaderPromise) {
    return loaderPromise;
  }

  window.MathJax = {
    loader: { load: ['[tex]/ams'] },
    tex: {
      inlineMath: [['$', '$'], ['\\(', '\\)']],
      displayMath: [['$$', '$$'], ['\\[', '\\]']],
      processEnvironments: true,
      processEscapes: true,
      packages: { '[+]': ['ams'] }
    },
    options: {
      skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code']
    },
    startup: { typeset: false }
  };

  loaderPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-chtml.js';
    script.async = true;
    script.onload = () =>
      (window.MathJax.startup?.promise ?? Promise.resolve()).then(resolve).catch(reject);
    script.onerror = () => reject(new Error('MathJax 加载失败'));
    document.head.appendChild(script);
  });

  return loaderPromise;
}
