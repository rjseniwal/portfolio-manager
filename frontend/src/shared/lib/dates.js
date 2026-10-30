/** Format a Date using the user's local calendar rather than UTC. */
export function localToday(date = new Date()) {
  const year = String(date.getFullYear()).padStart(4, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function monthToDate(date = new Date()) {
  const first = new Date(date.getTime());
  first.setDate(1);
  return { from: localToday(first), to: localToday(date) };
}

/** Inclusive local calendar range for the supported reporting presets. */
export function dateRange(preset, date = new Date()) {
  if (preset === 'month') return monthToDate(date);
  const first = new Date(date.getTime());
  if (preset === '30days') first.setDate(first.getDate() - 29);
  else if (preset === 'year') first.setMonth(0, 1);
  else throw new RangeError(`Unknown date range preset: ${preset}`);
  return { from: localToday(first), to: localToday(date) };
}

/** Twelve calendar months including the current partial month. */
export function trendRange(date = new Date()) {
  const first = new Date(date.getTime());
  first.setDate(1);
  first.setMonth(first.getMonth() - 11);
  return { from: localToday(first), to: localToday(date) };
}

export function validDate(value) {
  if (
    typeof value !== 'string' ||
    value.length !== 10 ||
    !/^\d{4}-\d{2}-\d{2}$/.test(value)
  ) {
    return false;
  }
  const [year, month, day] = value.split('-').map(Number);
  if (year < 1 || month < 1 || month > 12 || day < 1) return false;
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const daysInMonth = [
    31,
    leap ? 29 : 28,
    31,
    30,
    31,
    30,
    31,
    31,
    30,
    31,
    30,
    31,
  ];
  return day <= daysInMonth[month - 1];
}
