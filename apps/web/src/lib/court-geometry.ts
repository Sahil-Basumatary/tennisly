/**
 * Regulation court dimensions mirrored from
 * services/replay-service/.../physics/CourtGeometry.java (SI metres).
 * Net sits at y = 0; positive y runs towards the receiver baseline.
 */

export const HALF_LENGTH_METRES = 11.885;
export const FULL_LENGTH_METRES = HALF_LENGTH_METRES * 2;
export const SINGLES_HALF_WIDTH_METRES = 4.115;
export const DOUBLES_HALF_WIDTH_METRES = 5.485;
export const SERVICE_LINE_FROM_NET_METRES = 6.4;
export const NET_HEIGHT_CENTRE_METRES = 0.914;
export const NET_HEIGHT_POST_METRES = 1.07;

export const BALL_MASS_KG = 0.057;
export const BALL_RADIUS_METRES = 0.0335;
export const GRAVITY_METRES_PER_SECOND_SQUARED = 9.81;

export const GROUNDSTROKE_CONTACT_HEIGHT_METRES = 0.95;
export const VOLLEY_CONTACT_HEIGHT_METRES = 1.15;
export const SERVE_CONTACT_HEIGHT_METRES = 2.65;

export const FRAME_RATE = 60;

export function netHeightAt(lateralX: number): number {
  const clamped = Math.min(Math.abs(lateralX), DOUBLES_HALF_WIDTH_METRES);
  const ratio = clamped / DOUBLES_HALF_WIDTH_METRES;
  return (
    NET_HEIGHT_CENTRE_METRES +
    (NET_HEIGHT_POST_METRES - NET_HEIGHT_CENTRE_METRES) * ratio
  );
}
