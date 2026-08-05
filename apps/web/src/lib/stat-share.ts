export function parseStatMagnitude(raw: string): number | null {
  const trimmed = raw.trim();
  if (!trimmed || trimmed === "—") return null;
  if (trimmed.endsWith("%")) {
    const n = Number.parseFloat(trimmed.slice(0, -1));
    return Number.isFinite(n) ? n : null;
  }
  if (trimmed.includes("/")) {
    const [a, b] = trimmed.split("/").map((p) => Number.parseFloat(p.trim()));
    if (!Number.isFinite(a) || !Number.isFinite(b) || b === 0) {
      return Number.isFinite(a) ? a : null;
    }
    return (a / b) * 100;
  }
  const n = Number.parseFloat(trimmed);
  return Number.isFinite(n) ? n : null;
}

/** Home share of a dual bar in [0, 1]. Equal split when unknown. */
export function homeShare(homeRaw: string, awayRaw: string): number {
  const home = parseStatMagnitude(homeRaw);
  const away = parseStatMagnitude(awayRaw);
  if (home === null && away === null) return 0.5;
  if (home === null) return 0;
  if (away === null) return 1;
  const sum = home + away;
  if (sum <= 0) return 0.5;
  return home / sum;
}
