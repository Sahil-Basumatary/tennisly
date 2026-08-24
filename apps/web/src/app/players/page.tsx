import { PageHero } from "@/components/layout/PageHero";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { PlayersSkeleton } from "@/components/scaffolds/PlayersSkeleton";
import { withPlayersBoardHeadshots } from "@/lib/player-photos";
import { getPlayersBoard } from "@/services/scaffolds";

type PageProps = {
  searchParams: Promise<{
    tour?: string;
    q?: string;
    view?: string;
    page?: string;
    size?: string;
  }>;
};

const DEFAULT_SIZE = 50;
const MAX_SIZE = 100;

function parsePositiveInt(raw: string | undefined, fallback: number): number {
  const n = Number.parseInt(raw ?? "", 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

function rankingsHref(opts: {
  tour: string;
  view?: string;
  q?: string;
  page: number;
  size: number;
}): string {
  const params = new URLSearchParams();
  params.set("tour", opts.tour);
  if (opts.view === "rankings" || opts.view === "search") {
    params.set("view", opts.view);
  }
  if (opts.q) params.set("q", opts.q);
  if (opts.page > 1) params.set("page", String(opts.page));
  if (opts.size !== DEFAULT_SIZE) params.set("size", String(opts.size));
  return `/players?${params.toString()}`;
}

export default async function PlayersPage({ searchParams }: PageProps) {
  const { tour: tourParam, q, view, page: pageParam, size: sizeParam } = await searchParams;
  const tour = tourParam === "wta" ? "wta" : "atp";
  const query = q?.trim() ?? "";
  const searching = view === "search" || query.length > 0;
  const rankingsView = view === "rankings" || !searching;
  const size = Math.min(MAX_SIZE, parsePositiveInt(sizeParam, DEFAULT_SIZE));
  const board = await getPlayersBoard(tour);
  const filtered = query
    ? board.rows.filter((row) => row.name.toLowerCase().includes(query.toLowerCase()))
    : searching
      ? []
      : board.rows;
  const total = filtered.length;
  const pageCount = Math.max(1, Math.ceil(total / size) || 1);
  const page = Math.min(parsePositiveInt(pageParam, 1), pageCount);
  const pagedRows = filtered.slice((page - 1) * size, page * size);
  const pagedBoard = await withPlayersBoardHeadshots({
    ...board,
    rows: pagedRows,
    total,
    page,
    size,
  });
  const viewKey = searching ? "search" : rankingsView && view === "rankings" ? "rankings" : undefined;
  const rankJumps =
    searching || total === 0
      ? []
      : Array.from({ length: pageCount }, (_, index) => {
          const start = index * size + 1;
          const end = Math.min((index + 1) * size, total);
          return {
            label: `${start}–${end}`,
            href: rankingsHref({ tour, view: viewKey, q: query || undefined, page: index + 1, size }),
            current: index + 1 === page,
          };
        });
  const subnav = [
    {
      id: "atp",
      label: "ATP Singles",
      href: rankingsHref({ tour: "atp", view: viewKey, page: 1, size }),
    },
    {
      id: "wta",
      label: "WTA Singles",
      href: rankingsHref({ tour: "wta", view: viewKey, page: 1, size }),
    },
    { id: "search", label: "Search", href: `/players?view=search&tour=${tour}` },
  ];
  return (
    <>
      <SectionSubnav items={subnav} activeId={searching ? "search" : tour} />
      {searching ? (
        <PageHero
          eyebrow="Players"
          title="Search"
          description="Filter the live rankings table by name. Open a row for the player board."
        />
      ) : null}
      {searching ? (
        <div className="border-b border-hairline bg-white">
          <form
            action="/players"
            method="get"
            className="mx-auto flex max-w-[1400px] flex-wrap items-end gap-3 px-4 py-4 sm:px-6"
          >
            <input type="hidden" name="tour" value={tour} />
            <input type="hidden" name="view" value="search" />
            <label className="min-w-[220px] flex-1">
              <span className="mb-1 block font-data text-[11px] font-bold uppercase tracking-[0.14em] text-muted-foreground">
                Player name
              </span>
              <input
                name="q"
                defaultValue={query}
                placeholder="Alcaraz"
                className="h-10 w-full border border-hairline px-3 font-sans text-sm outline-none focus:border-chrome"
              />
            </label>
            <button
              type="submit"
              className="h-10 border border-chrome bg-chrome px-4 font-sans text-[12px] font-bold uppercase tracking-wide text-chrome-foreground"
            >
              Search
            </button>
          </form>
        </div>
      ) : null}
      <PlayersSkeleton
        board={pagedBoard}
        hideHero={searching}
        emptyLabel={
          board.rows.length === 0
            ? undefined
            : searching && !query
              ? "Type a surname to search the rankings table."
              : searching
                ? "No ranked players match that name."
                : undefined
        }
        prevHref={
          pagedRows.length > 0 && page > 1
            ? rankingsHref({ tour, view: viewKey, q: query || undefined, page: page - 1, size })
            : null
        }
        nextHref={
          pagedRows.length > 0 && page < pageCount
            ? rankingsHref({ tour, view: viewKey, q: query || undefined, page: page + 1, size })
            : null
        }
        rankJumps={rankJumps}
      />
    </>
  );
}
