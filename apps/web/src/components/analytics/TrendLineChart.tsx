"use client";

import { AxisBottom, AxisLeft } from "@visx/axis";
import { curveMonotoneX } from "@visx/curve";
import { GridRows } from "@visx/grid";
import { Group } from "@visx/group";
import { ParentSize } from "@visx/responsive";
import { scaleLinear, scalePoint } from "@visx/scale";
import { LinePath } from "@visx/shape";
import { motion } from "framer-motion";
import type { PlayerTrendPoint } from "@/types/analytics";

const margin = { top: 16, right: 16, bottom: 36, left: 44 };

type TrendLineChartProps = {
  points: PlayerTrendPoint[];
  title?: string;
};

function TrendChartInner({ width, height, points, title }: TrendLineChartProps & { width: number; height: number }) {
  const innerWidth = Math.max(0, width - margin.left - margin.right);
  const innerHeight = Math.max(0, height - margin.top - margin.bottom);
  const data = points.map((point, index) => ({
    index,
    label: point.matchId.slice(0, 8),
    value: point.metrics.pointsWon,
  }));
  const xScale = scalePoint<string>({
    domain: data.map((d) => String(d.index)),
    range: [0, innerWidth],
    padding: 0.4,
  });
  const maxY = Math.max(1, ...data.map((d) => d.value));
  const yScale = scaleLinear<number>({
    domain: [0, maxY * 1.1],
    range: [innerHeight, 0],
    nice: true,
  });
  const primary = "hsl(var(--primary))";

  return (
    <svg
      width={width}
      height={height}
      role="img"
      aria-label={title ?? "Points won trend over recent matches"}
    >
      <title>{title ?? "Points won trend over recent matches"}</title>
      <Group left={margin.left} top={margin.top}>
        <GridRows
          scale={yScale}
          width={innerWidth}
          stroke="hsl(var(--hairline))"
          strokeOpacity={0.8}
        />
        <LinePath
          data={data}
          x={(d) => xScale(String(d.index)) ?? 0}
          y={(d) => yScale(d.value)}
          stroke={primary}
          strokeWidth={2}
          curve={curveMonotoneX}
        />
        {data.map((d) => (
          <circle
            key={d.index}
            cx={xScale(String(d.index)) ?? 0}
            cy={yScale(d.value)}
            r={3}
            fill={primary}
          />
        ))}
        <AxisLeft
          scale={yScale}
          tickFormat={(v) => String(v)}
          stroke="hsl(var(--hairline))"
          tickStroke="hsl(var(--hairline))"
          tickLabelProps={() => ({
            fill: "hsl(var(--muted-foreground))",
            fontSize: 10,
            fontFamily: "var(--font-data)",
            textAnchor: "end",
            dx: -4,
          })}
        />
        <AxisBottom
          top={innerHeight}
          scale={xScale}
          tickFormat={(v) => {
            const idx = Number(v);
            return data[idx]?.label ?? "";
          }}
          stroke="hsl(var(--hairline))"
          tickStroke="hsl(var(--hairline))"
          tickLabelProps={() => ({
            fill: "hsl(var(--muted-foreground))",
            fontSize: 10,
            fontFamily: "var(--font-data)",
            textAnchor: "middle",
          })}
        />
      </Group>
    </svg>
  );
}

export function TrendLineChart({ points, title }: TrendLineChartProps) {
  if (points.length === 0) return null;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
      className="border border-hairline bg-white p-4"
    >
      <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
        Points won trend
      </h2>
      <div className="h-[240px] w-full">
        <ParentSize>
          {({ width, height }) =>
            width > 0 && height > 0 ? (
              <TrendChartInner width={width} height={height} points={points} title={title} />
            ) : null
          }
        </ParentSize>
      </div>
    </motion.div>
  );
}
