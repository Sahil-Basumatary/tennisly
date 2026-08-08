"use client";

import { AxisBottom, AxisLeft } from "@visx/axis";
import { GridColumns } from "@visx/grid";
import { Group } from "@visx/group";
import { ParentSize } from "@visx/responsive";
import { scaleBand, scaleLinear } from "@visx/scale";
import { motion } from "framer-motion";

const margin = { top: 16, right: 24, bottom: 36, left: 72 };

type SurfaceBreakdownChartProps = {
  data: Record<string, number>;
  title?: string;
  valueLabel?: string;
};

function SurfaceChartInner({
  width,
  height,
  data,
  title,
  valueLabel = "Count",
}: SurfaceBreakdownChartProps & { width: number; height: number }) {
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return null;
  const innerWidth = Math.max(0, width - margin.left - margin.right);
  const innerHeight = Math.max(0, height - margin.top - margin.bottom);
  const yScale = scaleBand<string>({
    domain: entries.map(([surface]) => surface),
    range: [0, innerHeight],
    padding: 0.25,
  });
  const maxVal = Math.max(1, ...entries.map(([, v]) => v));
  const xScale = scaleLinear<number>({
    domain: [0, maxVal * 1.1],
    range: [0, innerWidth],
    nice: true,
  });
  const barColor = "hsl(var(--primary))";

  return (
    <svg
      width={width}
      height={height}
      role="img"
      aria-label={title ?? "Surface breakdown chart"}
    >
      <title>{title ?? "Surface breakdown"}</title>
      <Group left={margin.left} top={margin.top}>
        <GridColumns
          scale={xScale}
          height={innerHeight}
          stroke="hsl(var(--hairline))"
          strokeOpacity={0.8}
        />
        {entries.map(([surface, value]) => {
          const y = yScale(surface) ?? 0;
          const barH = yScale.bandwidth();
          const barW = xScale(value);
          return (
            <rect
              key={surface}
              x={0}
              y={y}
              width={barW}
              height={barH}
              fill={barColor}
            />
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
          scale={xScale}
          label={valueLabel}
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

export function SurfaceBreakdownChart({ data, title, valueLabel }: SurfaceBreakdownChartProps) {
  if (Object.keys(data).length === 0) return null;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: "easeOut" }}
      className="border border-hairline bg-white p-4"
    >
      <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
        {title ?? "Surface breakdown"}
      </h2>
      <div className="h-[220px] w-full">
        <ParentSize>
          {({ width, height }) =>
            width > 0 && height > 0 ? (
              <SurfaceChartInner
                width={width}
                height={height}
                data={data}
                title={title}
                valueLabel={valueLabel}
              />
            ) : null
          }
        </ParentSize>
      </div>
    </motion.div>
  );
}
