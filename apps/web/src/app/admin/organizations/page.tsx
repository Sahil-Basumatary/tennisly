import { AdminOrganizationsPanel } from "@/components/admin/AdminOrganizationsPanel";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminOrganizationsPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="organizations" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="Organizations"
          description="Clerk still owns creation. Admin can search, inspect inactive orgs, adjust plan caps, and deactivate."
        />
        <AdminOrganizationsPanel />
      </main>
    </>
  );
}
