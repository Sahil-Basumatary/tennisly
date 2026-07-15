"use client";

import { useAuth, useClerk } from "@clerk/nextjs";
import { useEffect } from "react";
import {
  claimBrowserSession,
  consumeRememberPending,
  shouldPersistSession,
} from "@/lib/auth-session";

/**
 * Clerk cookies survive browser restarts. When "Remember me" was off at sign-in,
 * drop the session on the first load of a new browser process so the checkbox
 * matches expected session-cookie behavior.
 */
export function SessionPersistence() {
  const { isLoaded, isSignedIn } = useAuth();
  const { signOut } = useClerk();

  useEffect(() => {
    if (!isLoaded) return;
    consumeRememberPending();
    const continuingBrowserSession = claimBrowserSession();
    if (continuingBrowserSession) return;
    if (!isSignedIn) return;
    if (shouldPersistSession()) return;
    void signOut({ redirectUrl: "/" });
  }, [isLoaded, isSignedIn, signOut]);

  return null;
}
