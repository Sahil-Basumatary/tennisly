/** Broadcast identity keyed by tennis-data externalId (atp- and wta- prefixes). */
const BY_EXTERNAL: Record<string, { country: string; shortName: string }> = {
  "atp-001": { country: "ITA", shortName: "SIN" },
  "atp-002": { country: "DEU", shortName: "ZVE" },
  "atp-003": { country: "ESP", shortName: "ALC" },
  "atp-005": { country: "RUS", shortName: "MED" },
  "atp-006": { country: "NOR", shortName: "RUU" },
  "atp-007": { country: "SRB", shortName: "DJO" },
  "atp-009": { country: "RUS", shortName: "RUB" },
  "atp-014": { country: "GRC", shortName: "TSI" },
  "atp-031": { country: "BRA", shortName: "FON" },
  "wta-001": { country: "BLR", shortName: "SAB" },
  "wta-002": { country: "POL", shortName: "SWI" },
  "wta-003": { country: "USA", shortName: "GAU" },
  "wta-004": { country: "ITA", shortName: "PAO" },
  "wta-006": { country: "KAZ", shortName: "RYB" },
  "wta-013": { country: "USA", shortName: "KEY" },
};

/** Offline catalogue fallback UUIDs → externalId when tennis-data was unreachable at seed. */
const FALLBACK_UUID_TO_EXTERNAL: Record<string, string> = {
  "b1000000-0000-4000-8000-000000000001": "atp-003",
  "b1000000-0000-4000-8000-000000000002": "atp-001",
  "b1000000-0000-4000-8000-000000000003": "atp-007",
  "b1000000-0000-4000-8000-000000000004": "atp-005",
  "b1000000-0000-4000-8000-000000000005": "atp-002",
  "b1000000-0000-4000-8000-000000000006": "atp-006",
  "b1000000-0000-4000-8000-000000000007": "wta-002",
  "b1000000-0000-4000-8000-000000000008": "wta-003",
  "b1000000-0000-4000-8000-000000000009": "wta-001",
  "b1000000-0000-4000-8000-00000000000a": "wta-006",
  "b1000000-0000-4000-8000-00000000000b": "wta-013",
  "b1000000-0000-4000-8000-00000000000c": "atp-031",
  "b1000000-0000-4000-8000-00000000000d": "atp-009",
  "b1000000-0000-4000-8000-00000000000e": "wta-004",
  "b1000000-0000-4000-8000-00000000000f": "atp-014",
};

function resolveExternalId(playerId: string, externalId?: string): string | undefined {
  if (externalId && BY_EXTERNAL[externalId]) return externalId;
  return FALLBACK_UUID_TO_EXTERNAL[playerId];
}

export function playerCountry(playerId: string, externalId?: string): string {
  const key = resolveExternalId(playerId, externalId);
  return (key && BY_EXTERNAL[key]?.country) ?? "—";
}

export function playerShortName(
  playerId: string,
  displayName: string,
  externalId?: string,
): string {
  const key = resolveExternalId(playerId, externalId);
  const known = key ? BY_EXTERNAL[key]?.shortName : undefined;
  if (known) return known;
  const parts = displayName.trim().split(/\s+/);
  const last = parts[parts.length - 1] ?? displayName;
  return last.slice(0, 3).toUpperCase();
}
