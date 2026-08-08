import { AdminUsersPanel } from "@/components/admin/AdminUsersPanel";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminUsersPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="users" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="Users"
          description="Platform-wide profile directory. Deactivation is soft — profiles stay in the database for audit continuity."
        />
        <AdminUsersPanel />
      </main>
    </>
  );
}
