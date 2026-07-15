export const ACCOUNT_TITLES = [
  "Mr",
  "Mrs",
  "Miss",
  "Ms",
  "Dr",
  "Mx",
] as const;

export const COUNTRIES = [
  "United Kingdom",
  "United States",
  "Ireland",
  "Australia",
  "Canada",
  "France",
  "Germany",
  "Spain",
  "Italy",
  "Netherlands",
  "India",
  "Japan",
  "Brazil",
  "South Africa",
  "New Zealand",
  "Other",
] as const;

export const MONTHS = [
  { value: "1", label: "January" },
  { value: "2", label: "February" },
  { value: "3", label: "March" },
  { value: "4", label: "April" },
  { value: "5", label: "May" },
  { value: "6", label: "June" },
  { value: "7", label: "July" },
  { value: "8", label: "August" },
  { value: "9", label: "September" },
  { value: "10", label: "October" },
  { value: "11", label: "November" },
  { value: "12", label: "December" },
] as const;

export const MIN_AGE_YEARS = 13;

export function birthYearOptions(now = new Date()) {
  const newest = now.getFullYear() - MIN_AGE_YEARS;
  const oldest = newest - 100;
  const years: number[] = [];
  for (let y = newest; y >= oldest; y -= 1) years.push(y);
  return years;
}

export function isOldEnough(
  day: string,
  month: string,
  year: string,
  now = new Date(),
) {
  const d = Number(day);
  const m = Number(month);
  const y = Number(year);
  if (!d || !m || !y) return false;
  const cutoff = new Date(now);
  cutoff.setFullYear(cutoff.getFullYear() - MIN_AGE_YEARS);
  const birth = new Date(y, m - 1, d);
  return birth <= cutoff;
}

export function ageGateHint(now = new Date()) {
  const cutoff = new Date(now);
  cutoff.setFullYear(cutoff.getFullYear() - MIN_AGE_YEARS);
  const dd = String(cutoff.getDate()).padStart(2, "0");
  const mm = String(cutoff.getMonth() + 1).padStart(2, "0");
  return `You must have been born before ${dd}/${mm}/${cutoff.getFullYear()} to register.`;
}
