"use client";

import dynamic from "next/dynamic";
import type { PlayerTrends } from "@/types/analytics";

const TrendLineChart = dynamic(
  () => import("@/components/analytics/TrendLineChart").then((mod) => mod.TrendLineChart),
  { ssr: false, loading: () => <div className="h-56 border border-hairline bg-white" /> },
);

const SurfaceBreakdownChart = dynamic(
  () =>
    import("@/components/analytics/SurfaceBreakdownChart").then(
      (mod) => mod.SurfaceBreakdownChart,
    ),
  { ssr: false, loading: () => <div className="h-56 border border-hairline bg-white" /> },
);

type PlayerAnalyticsChartsProps = {
  trends: PlayerTrends["trends"];
  trendTitle: string;
  surfaceData: Record<string, number>;
  surfaceTitle: string;
};

export function PlayerAnalyticsCharts({
  trends,
  trendTitle,
  surfaceData,
  surfaceTitle,
}: PlayerAnalyticsChartsProps) {
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <TrendLineChart points={trends} title={trendTitle} />
      <SurfaceBreakdownChart
        data={surfaceData}
        title={surfaceTitle}
        valueLabel="Points"
      />
    </div>
  );
}
