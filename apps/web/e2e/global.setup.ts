import { clerkSetup } from "@clerk/testing/playwright";
import { loadEnvConfig } from "@next/env";

export default async function globalSetup() {
  loadEnvConfig(process.cwd());
  if (!process.env.CLERK_PUBLISHABLE_KEY && process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY) {
    process.env.CLERK_PUBLISHABLE_KEY = process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY;
  }
  await clerkSetup();
}
