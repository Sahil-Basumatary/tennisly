import { ClerkProvider as BaseClerkProvider } from "@clerk/nextjs";
import { SessionPersistence } from "@/components/auth/SessionPersistence";

export function ClerkProvider({ children }: { children: React.ReactNode }) {
  return (
    <BaseClerkProvider>
      <SessionPersistence />
      {children}
    </BaseClerkProvider>
  );
}
