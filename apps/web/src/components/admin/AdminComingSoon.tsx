import { AdminPageHeader } from "@/components/admin/AdminPageHeader";

type AdminComingSoonProps = {
  title: string;
  detail: string;
};

export function AdminComingSoon({ title, detail }: AdminComingSoonProps) {
  return (
    <div className="border border-hairline bg-white p-5 sm:p-6">
      <AdminPageHeader title={title} description={detail} />
      <p className="font-sans text-[13px] text-muted-foreground">
        This surface is planned for a later admin milestone. Navigation stays visible so the full
        console layout is already wired.
      </p>
    </div>
  );
}
