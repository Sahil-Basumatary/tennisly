import Link from "next/link";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminOverviewPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="overview" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="Platform console"
          description="Operate organizations, users, keys, webhooks, audit, usage, and service health from this shell."
        />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[
            { label: "Organizations", href: "/admin/organizations", hint: "Search, filter, edit plans" },
            { label: "Users", href: "/admin/users", hint: "Search profiles, deactivate access" },
            { label: "Health", href: "/admin/health", hint: "Best-effort service status strips" },
            { label: "Keys", href: "/admin/keys", hint: "Issue and revoke API keys" },
            { label: "Webhooks", href: "/admin/webhooks", hint: "Partner endpoints and deliveries" },
            { label: "Audit", href: "/admin/audit", hint: "Operator actions on this console" },
            { label: "Usage", href: "/admin/usage", hint: "Metered request volume" },
          ].map((card) => (
            <Link
              key={card.label}
              href={card.href}
              className="min-h-[140px] border border-hairline bg-white p-5 transition-colors hover:border-primary"
            >
              <h2 className="mb-2 font-sans text-[14px] font-bold uppercase tracking-wide">
                {card.label}
              </h2>
              <p className="font-sans text-[13px] text-muted-foreground">{card.hint}</p>
            </Link>
          ))}
        </div>
      </main>
    </>
  );
}
