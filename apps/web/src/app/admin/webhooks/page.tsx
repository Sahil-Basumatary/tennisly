import { AdminPageHeader } from "@/components/admin/AdminPageHeader";
import { AdminWebhookDeliveriesPanel } from "@/components/admin/AdminWebhookDeliveriesPanel";
import { AdminWebhooksPanel } from "@/components/admin/AdminWebhooksPanel";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminWebhooksPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="webhooks" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminPageHeader
          title="Webhooks"
          description="Register signed HTTP endpoints for match and security events. Secrets are shown once."
        />
        <div className="space-y-10">
          <AdminWebhooksPanel />
          <AdminWebhookDeliveriesPanel />
        </div>
      </main>
    </>
  );
}
