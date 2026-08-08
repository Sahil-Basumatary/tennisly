"use client";

import Link from "next/link";
import { SignedIn, SignedOut } from "@clerk/nextjs";
import { useCallback, useEffect, useState } from "react";
import {
  createSavedView,
  deleteSavedView,
  listSavedViews,
  setSavedViewFavorite,
} from "@/services/analytics";
import type { SavedAnalyticsView } from "@/types/analytics";

type SavedViewsPanelProps = {
  config?: Record<string, unknown>;
  compact?: boolean;
};

export function SavedViewsPanel({ config, compact = false }: SavedViewsPanelProps) {
  const [views, setViews] = useState<SavedAnalyticsView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listSavedViews();
      setViews(data);
    } catch {
      setError("Could not load saved views.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function handleSave(event: React.FormEvent) {
    event.preventDefault();
    if (!name.trim() || !config) return;
    setSaving(true);
    try {
      await createSavedView({ name: name.trim(), config });
      setName("");
      await refresh();
    } catch {
      setError("Could not save view.");
    } finally {
      setSaving(false);
    }
  }

  async function toggleFavorite(view: SavedAnalyticsView) {
    try {
      await setSavedViewFavorite(view.id, !view.favorite);
      await refresh();
    } catch {
      setError("Could not update favorite.");
    }
  }

  async function removeView(id: string) {
    try {
      await deleteSavedView(id);
      await refresh();
    } catch {
      setError("Could not delete view.");
    }
  }

  return (
    <div className="border border-hairline bg-white p-4 sm:p-5">
      <h2 className="mb-3 font-sans text-[13px] font-bold uppercase tracking-wide">
        Saved views
      </h2>
      <SignedOut>
        <p className="font-sans text-[13px] text-muted-foreground">
          Sign in to save filter presets and reopen them from any device.
        </p>
      </SignedOut>
      <SignedIn>
        {config && !compact ? (
          <form onSubmit={handleSave} className="mb-4 flex flex-wrap gap-2">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="View name"
              maxLength={100}
              className="min-w-[180px] flex-1 border border-hairline px-3 py-2 font-sans text-sm outline-none focus:border-primary"
            />
            <button
              type="submit"
              disabled={saving || !name.trim()}
              className="border border-primary bg-primary px-4 py-2 font-sans text-[11px] font-semibold uppercase tracking-wide text-primary-foreground disabled:opacity-60"
            >
              Save current
            </button>
          </form>
        ) : null}
        {loading ? (
          <p className="font-sans text-sm text-muted-foreground">Loading saved views…</p>
        ) : error ? (
          <p className="font-sans text-sm text-destructive">{error}</p>
        ) : views.length === 0 ? (
          <p className="font-sans text-[13px] text-muted-foreground">No saved views yet.</p>
        ) : (
          <ul className="divide-y divide-hairline border border-hairline">
            {views.map((view) => (
              <li key={view.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2.5">
                <div className="min-w-0">
                  <p className="truncate font-sans text-[14px] font-semibold">{view.name}</p>
                  <p className="font-data text-[11px] text-muted-foreground">
                    Updated {new Date(view.updatedAt).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => void toggleFavorite(view)}
                    className="font-sans text-[11px] font-semibold uppercase tracking-wide text-primary"
                    aria-pressed={view.favorite}
                  >
                    {view.favorite ? "Favorited" : "Favorite"}
                  </button>
                  <Link
                    href={`/analytics/views?view=${view.id}`}
                    className="font-sans text-[11px] font-semibold uppercase tracking-wide text-muted-foreground hover:text-foreground"
                  >
                    Open
                  </Link>
                  <button
                    type="button"
                    onClick={() => void removeView(view.id)}
                    className="font-sans text-[11px] font-semibold uppercase tracking-wide text-destructive"
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </SignedIn>
    </div>
  );
}
