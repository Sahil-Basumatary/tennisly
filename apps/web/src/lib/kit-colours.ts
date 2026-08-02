/** Shared home/away kit tints — same RGB across 3D, heatmaps, and stats bars. */
export type KitSide = "home" | "away";

export const KIT_RGB: Record<KitSide, readonly [number, number, number]> = {
  home: [214, 86, 24],
  away: [34, 106, 200],
};

export function kitCss(side: KitSide, alpha = 1): string {
  const [r, g, b] = KIT_RGB[side];
  return alpha >= 1 ? `rgb(${r}, ${g}, ${b})` : `rgba(${r}, ${g}, ${b}, ${alpha})`;
}
