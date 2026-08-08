import { NotificationSettingsPanel } from "@/components/settings/NotificationSettingsPanel";

export default function NotificationSettingsPage() {
  return (
    <main id="main-content" className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <p className="mb-1 font-sans text-xs font-semibold uppercase tracking-[0.16em] text-primary">
        Account
      </p>
      <h1 className="mb-2 font-display text-2xl font-semibold text-foreground sm:text-3xl">
        Notification settings
      </h1>
      <p className="mb-8 max-w-2xl font-sans text-sm text-muted-foreground">
        Control which emails Tennisly sends. Master switches override category toggles.
      </p>
      <NotificationSettingsPanel />
    </main>
  );
}
