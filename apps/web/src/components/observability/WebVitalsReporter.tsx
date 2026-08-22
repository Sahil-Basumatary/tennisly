"use client";

import { useEffect } from "react";

type MetricName = "LCP" | "INP" | "CLS" | "TTFB";

function send(name: MetricName, value: number) {
  const body = JSON.stringify({ name, value, path: window.location.pathname });
  if (navigator.sendBeacon) {
    navigator.sendBeacon("/api/vitals", body);
    return;
  }
  void fetch("/api/vitals", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
    keepalive: true,
  });
}

export function WebVitalsReporter() {
  useEffect(() => {
    if (typeof PerformanceObserver === "undefined") {
      return;
    }
    const lcp = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const last = entries[entries.length - 1];
      if (last) send("LCP", last.startTime);
    });
    lcp.observe({ type: "largest-contentful-paint", buffered: true });

    const inp = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        const event = entry as PerformanceEventTiming;
        if (event.duration) send("INP", event.duration);
      }
    });
    try {
      inp.observe({ type: "event", buffered: true, durationThreshold: 16 } as PerformanceObserverInit);
    } catch {
      inp.disconnect();
    }

    const cls = new PerformanceObserver((list) => {
      let score = 0;
      for (const entry of list.getEntries()) {
        const layout = entry as PerformanceEntry & { value?: number; hadRecentInput?: boolean };
        if (!layout.hadRecentInput) score += layout.value ?? 0;
      }
      send("CLS", score);
    });
    try {
      cls.observe({ type: "layout-shift", buffered: true });
    } catch {
      cls.disconnect();
    }

    const nav = performance.getEntriesByType("navigation")[0] as PerformanceNavigationTiming | undefined;
    if (nav) send("TTFB", nav.responseStart);

    return () => {
      lcp.disconnect();
      inp.disconnect();
      cls.disconnect();
    };
  }, []);

  return null;
}
