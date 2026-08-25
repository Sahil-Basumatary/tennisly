/** Display helpers from tennis-data fields — no hardcoded player map. */
export function playerCountry(nationality?: string | null): string {
  const value = nationality?.trim();
  return value && value.length > 0 ? value.toUpperCase() : "—";
}

/** Live Tennis sometimes percent-encodes spaces inside a first name (`Aditya%20Vishal`). */
function decodeDisplayName(value: string): string {
  let current = value;
  for (let i = 0; i < 3; i++) {
    if (!/%[0-9A-Fa-f]{2}/.test(current)) break;
    try {
      const next = decodeURIComponent(current);
      if (next === current) break;
      current = next;
    } catch {
      break;
    }
  }
  return current.replace(/\s+/g, " ").trim();
}

/** Drop identity placeholders so Davis Cup nations are not shown as "Unknown Burundi". */
export function publicPlayerName(displayName: string): string {
  const trimmed = decodeDisplayName(displayName.trim());
  if (!trimmed || /^unknown$/i.test(trimmed)) return "TBD";
  const stripped = trimmed.replace(/^unknown\s+/i, "").trim();
  return stripped || "TBD";
}

export function playerShortName(displayName: string): string {
  const parts = publicPlayerName(displayName).split(/\s+/).filter(Boolean);
  const last = parts[parts.length - 1] ?? publicPlayerName(displayName);
  return last.slice(0, 3).toUpperCase();
}

export function playerInitials(displayName: string): string {
  const parts = publicPlayerName(displayName).split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] ?? ""}${parts[parts.length - 1][0] ?? ""}`.toUpperCase();
}
