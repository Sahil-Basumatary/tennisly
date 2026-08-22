export function newRequestId(): string {
  return crypto.randomUUID();
}

export function upstreamHeaders(extra?: HeadersInit): HeadersInit {
  return {
    Accept: "application/json",
    "X-Request-Id": newRequestId(),
    ...extra,
  };
}
