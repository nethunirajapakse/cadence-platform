import { useEffect, useState } from "react";

// Delays reflecting a fast-changing value (e.g. text input) until it's been
// stable for delayMs - used to avoid firing a backend request on every
// keystroke of a search filter.
export function useDebouncedValue<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timeout = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timeout);
  }, [value, delayMs]);

  return debounced;
}
