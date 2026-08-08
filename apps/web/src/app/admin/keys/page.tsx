import { AdminComingSoon } from "@/components/admin/AdminComingSoon";
import { SectionSubnav } from "@/components/layout/SectionSubnav";
import { adminSubnav } from "@/config/admin-subnav";

export default function AdminKeysPage() {
  return (
    <>
      <SectionSubnav items={adminSubnav} activeId="keys" />
      <main id="main-content" className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <AdminComingSoon
          title="API keys"
          detail="Issue and rotate partner keys with scoped permissions. Planned for the next admin milestone."
        />
      </main>
    </>
  );
}
