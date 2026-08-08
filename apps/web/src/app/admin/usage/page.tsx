import { AdminUsagePanel } from "@/components/admin/AdminUsagePanel";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminUsagePage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="usage" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="Usage metering"
          description="Daily org-level counters for admin actions and API consumption."
        />
        <AdminUsagePanel />
      </main>
    </>
  );
}
