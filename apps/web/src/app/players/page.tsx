import { PageHero } from "@/components/layout/PageHero";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { PlayersSkeleton } from "@/components/scaffolds/PlayersSkeleton";
import { getPlayersBoard } from "@/services/scaffolds";

type PageProps = {
  searchParams: Promise<{ tour?: string; q?: string; view?: string }>;
};

export default async function PlayersPage({ searchParams }: PageProps) {
  const { tour: tourParam, q, view } = await searchParams;
  const tour = tourParam === "wta" ? "wta" : "atp";
  const query = q?.trim() ?? "";
  const searching = view === "search" || query.length > 0;
  const board = await getPlayersBoard(tour);
  const rows = query
    ? board.rows.filter((row) => row.name.toLowerCase().includes(query.toLowerCase()))
    : searching
      ? []
      : board.rows;
  const subnav = [
    { id: "atp", label: "ATP Singles", href: "/players?tour=atp" },
    { id: "wta", label: "WTA Singles", href: "/players?tour=wta" },
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
        board={{ ...board, rows }}
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
      />
    </>
  );
}
