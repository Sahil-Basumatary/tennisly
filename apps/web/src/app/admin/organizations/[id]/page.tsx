import { AdminOrganizationDetail } from "@/components/admin/AdminOrganizationDetail";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

type PageProps = {
  params: Promise<{ id: string }>;
};

export default async function AdminOrganizationDetailPage({ params }: PageProps) {
  const { id } = await params;
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="organizations" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader title="Organization detail" />
        <AdminOrganizationDetail orgId={id} />
      </main>
    </>
  );
}
