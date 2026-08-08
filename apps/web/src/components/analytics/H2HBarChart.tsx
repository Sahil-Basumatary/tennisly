"use client";

import { AxisBottom, AxisLeft } from "@visx/axis";
import { GridRows } from "@visx/grid";
import { Group } from "@visx/group";
import { ParentSize } from "@visx/responsive";
import { scaleBand, scaleLinear } from "@visx/scale";
import { motion } from "framer-motion";
import type { TapeSideMetrics } from "@/types/analytics";

const margin = { top: 16, right: 16, bottom: 48, left: 48 };

const METRICS = [
  { key: "pointsWon" as const, label: "Points" },
  { key: "servicePointsWon" as const, label: "Service" },
  { key: "breakPointsWon" as const, label: "Break" },
];

type H2HBarChartProps = {
  playerA: TapeSideMetrics;
  playerB: TapeSideMetrics;
  labelA?: string;
  labelB?: string;
};

function H2HChartInner({
  width,
  height,
  playerA,
  playerB,
  labelA,
  labelB,
}: H2HBarChartProps & { width: number; height: number }) {
  const innerWidth = Math.max(0, width - margin.left - margin.right);
  const innerHeight = Math.max(0, height - margin.top - margin.bottom);
  const groups = METRICS.map((m) => m.key);
  const x0 = scaleBand<string>({
    domain: groups,
    range: [0, innerWidth],
    padding: 0.25,
  });
  const x1 = scaleBand<string>({
    domain: ["A", "B"],
    range: [0, x0.bandwidth()],
    padding: 0.15,
  });
  const maxVal = Math.max(
    1,
    ...groups.flatMap((g) => [playerA[g], playerB[g]]),
  );
  const yScale = scaleLinear<number>({
    domain: [0, maxVal * 1.15],
    range: [innerHeight, 0],
    nice: true,
  });
  const colorA = "hsl(var(--primary))";
  const colorB = "hsl(var(--muted-foreground))";

  return (
    <svg
      width={width}
      height={height}
      role="img"
      aria-label={`Head-to-head metrics comparison between ${labelA ?? "Player A"} and ${labelB ?? "Player B"}`}
    >
      <title>Head-to-head tape metrics</title>
      <Group left={margin.left} top={margin.top}>
        <GridRows
          scale={yScale}
          width={innerWidth}
          stroke="hsl(var(--hairline))"
          strokeOpacity={0.8}
        />
        {groups.map((group) => {
          const gx = x0(group) ?? 0;
          const aVal = playerA[group];
          const bVal = playerB[group];
          const ax = gx + (x1("A") ?? 0);
          const bx = gx + (x1("B") ?? 0);
          const barW = x1.bandwidth();
          return (
            <Group key={group}>
              <rect
                x={ax}
                y={yScale(aVal)}
                width={barW}
                height={innerHeight - yScale(aVal)}
                fill={colorA}
              />
              <rect
                x={bx}
                y={yScale(bVal)}
                width={barW}
                height={innerHeight - yScale(bVal)}
                fill={colorB}
              />
            </Group>
          );
        })}
        <AxisLeft
          scale={yScale}
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
          scale={x0}
          tickFormat={(g) => METRICS.find((m) => m.key === g)?.label ?? g}
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
      <foreignObject x={margin.left} y={height - 28} width={innerWidth} height={24}>
        <div className="flex justify-center gap-4 font-sans text-[10px] uppercase tracking-wide text-muted-foreground">
          <span>
            <span className="mr-1.5 inline-block h-2 w-2 bg-primary align-middle" aria-hidden />
            {labelA ?? "Player A"}
          </span>
          <span>
            <span
              className="mr-1.5 inline-block h-2 w-2 bg-muted-foreground align-middle"
              aria-hidden
            />
            {labelB ?? "Player B"}
          </span>
        </div>
      </foreignObject>
    </svg>
  );
}

export function H2HBarChart({ playerA, playerB, labelA, labelB }: H2HBarChartProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
      className="border border-hairline bg-white p-4"
    >
      <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
        Head-to-head metrics
      </h2>
      <div className="h-[280px] w-full">
        <ParentSize>
          {({ width, height }) =>
            width > 0 && height > 0 ? (
              <H2HChartInner
                width={width}
                height={height}
                playerA={playerA}
                playerB={playerB}
                labelA={labelA}
                labelB={labelB}
              />
            ) : null
          }
        </ParentSize>
      </div>
    </motion.div>
  );
}
