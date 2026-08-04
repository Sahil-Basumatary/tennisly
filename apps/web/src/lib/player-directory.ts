/** Broadcast-facing identity for seeded catalogue players (country + ticker). */
const DIRECTORY: Record<string, { country: string; shortName: string }> = {
  "b1000000-0000-4000-8000-000000000001": { country: "ESP", shortName: "ALC" },
  "b1000000-0000-4000-8000-000000000002": { country: "ITA", shortName: "SIN" },
  "b1000000-0000-4000-8000-000000000003": { country: "SRB", shortName: "DJO" },
  "b1000000-0000-4000-8000-000000000004": { country: "RUS", shortName: "MED" },
  "b1000000-0000-4000-8000-000000000005": { country: "GER", shortName: "ZVE" },
  "b1000000-0000-4000-8000-000000000006": { country: "NOR", shortName: "RUU" },
  "b1000000-0000-4000-8000-000000000007": { country: "POL", shortName: "SWI" },
  "b1000000-0000-4000-8000-000000000008": { country: "USA", shortName: "GAU" },
  "b1000000-0000-4000-8000-000000000009": { country: "BLR", shortName: "SAB" },
  "b1000000-0000-4000-8000-00000000000a": { country: "KAZ", shortName: "RYB" },
  "b1000000-0000-4000-8000-00000000000b": { country: "USA", shortName: "KEY" },
  "b1000000-0000-4000-8000-00000000000c": { country: "BRA", shortName: "FON" },
  "b1000000-0000-4000-8000-00000000000d": { country: "RUS", shortName: "RUB" },
  "b1000000-0000-4000-8000-00000000000e": { country: "ITA", shortName: "PAO" },
  "b1000000-0000-4000-8000-00000000000f": { country: "GRE", shortName: "TSI" },
};

export function playerCountry(playerId: string): string {
  return DIRECTORY[playerId]?.country ?? "—";
}

export function playerShortName(playerId: string, displayName: string): string {
  const known = DIRECTORY[playerId]?.shortName;
  if (known) return known;
  const parts = displayName.trim().split(/\s+/);
  const last = parts[parts.length - 1] ?? displayName;
  return last.slice(0, 3).toUpperCase();
}
