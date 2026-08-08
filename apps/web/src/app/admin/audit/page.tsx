import { AdminComingSoon } from "@/components/admin/AdminComingSoon";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminAuditPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="audit" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminComingSoon
          title="Audit log"
          detail="Immutable admin action history with actor, target, and before/after snapshots."
        />
      </main>
    </>
  );
}
