import { AdminComingSoon } from "@/components/admin/AdminComingSoon";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminUsagePage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="usage" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminComingSoon
          title="Usage metering"
          detail="Org-level API consumption, quota enforcement, and billing handoff hooks."
        />
      </main>
    </>
  );
}
