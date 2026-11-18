const storageKey = 'matriz.theme';
const preferences = ['light', 'dark', 'system'];

// Only a non-sensitive display preference is persisted, never account or API data.
export function createTheme(browser) {
  const media = browser.matchMedia('(prefers-color-scheme: dark)');
  let preference = 'system';
  try {
    const stored = browser.localStorage.getItem(storageKey);
    if (preferences.includes(stored)) preference = stored;
  } catch { /* Storage can be unavailable in private/restricted browser contexts. */ }
  function apply() {
    browser.document.documentElement.dataset.theme = preference === 'system'
      ? (media.matches ? 'dark' : 'light') : preference;
  }
  apply();
  return {
    get preference() { return preference; },
    select(value) {
      if (!preferences.includes(value)) return;
      preference = value;
      try { browser.localStorage.setItem(storageKey, value); } catch { /* Keep in-memory theme usable. */ }
      apply();
    },
    connect() {
      media.addEventListener('change', apply);
      return () => media.removeEventListener('change', apply);
    },
  };
}
