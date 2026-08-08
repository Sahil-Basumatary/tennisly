import { AdminAuditPanel } from "@/components/admin/AdminAuditPanel";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminAuditPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="audit" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="Audit log"
          description="Immutable admin action history with actor, target, and metadata."
        />
        <AdminAuditPanel />
      </main>
    </>
  );
}
