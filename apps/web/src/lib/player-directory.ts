/** Display helpers from tennis-data fields — no hardcoded player map. */
export function playerCountry(nationality?: string | null): string {
  const value = nationality?.trim();
  return value && value.length > 0 ? value.toUpperCase() : "—";
}

export function playerShortName(displayName: string): string {
  const parts = displayName.trim().split(/\s+/).filter(Boolean);
  const last = parts[parts.length - 1] ?? displayName;
  return last.slice(0, 3).toUpperCase();
}
