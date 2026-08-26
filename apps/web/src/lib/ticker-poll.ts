export const TICKER_VISIBLE_MS = 10_000;
export const TICKER_HIDDEN_MS = 30_000;

export function tickerIntervalMs(hidden: boolean): number {
  return hidden ? TICKER_HIDDEN_MS : TICKER_VISIBLE_MS;
}

export function shouldReplaceTickerBody(status: number): boolean {
  return status === 200;
}
