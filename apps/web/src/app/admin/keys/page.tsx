import { AdminKeysPanel } from "@/components/admin/AdminKeysPanel";
import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminKeysPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="keys" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="API keys"
          description="Issue scoped partner keys. Plaintext secrets are shown once at creation."
        />
        <AdminKeysPanel />
      </main>
    </>
  );
}
