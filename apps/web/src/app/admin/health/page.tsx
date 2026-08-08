import { AdminHealthPanel } from "@/components/admin/AdminHealthPanel";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminHealthPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="health" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="System health"
          description="Best-effort actuator checks across core services. One failure never collapses the whole board."
        />
        <AdminHealthPanel />
      </main>
    </>
  );
}
