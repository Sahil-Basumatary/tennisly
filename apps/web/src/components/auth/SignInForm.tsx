"use client";

import { useSignIn } from "@clerk/nextjs";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { EyeIcon, EyeOffIcon } from "@/components/ui/brandIcons";
import {
  AuthAlert,
  AuthField,
  AuthInput,
  AuthPrimaryButton,
} from "@/components/auth/fields";
import { SocialButtons } from "@/components/auth/SocialButtons";
import {
  setRememberPreference,
  stashRememberBeforeRedirect,
} from "@/lib/auth-session";

function clerkMessage(err: unknown) {
  if (
    err &&
    typeof err === "object" &&
    "errors" in err &&
    Array.isArray((err as { errors: { message?: string }[] }).errors)
  ) {
    return (
      (err as { errors: { message?: string }[] }).errors[0]?.message ??
      "Something went wrong. Please try again."
    );
  }
  return "Something went wrong. Please try again.";
}

export function SignInForm() {
  const router = useRouter();
  const { isLoaded, signIn, setActive } = useSignIn();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState("");
  const [pending, setPending] = useState(false);
  const [resetMode, setResetMode] = useState(false);
  const [resetCode, setResetCode] = useState("");
  const [newPassword, setNewPassword] = useState("");

  function validateLogin() {
    const next: Record<string, string> = {};
    if (!email.trim()) next.email = "This field is required";
    if (!password) next.password = "This field is required";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isLoaded || !signIn) {
      setFormError("Sign-in is not ready yet. Refresh the page and try again.");
      return;
    }
    if (!validateLogin()) return;
    setPending(true);
    setFormError("");
    try {
      const result = await signIn.create({
        identifier: email.trim(),
        password,
      });
      if (result.status === "complete" && result.createdSessionId) {
        setRememberPreference(remember);
        await setActive({ session: result.createdSessionId });
        router.push("/dashboard");
        return;
      }
      setFormError("Additional verification is required. Check your email.");
    } catch (err) {
      setFormError(clerkMessage(err));
    } finally {
      setPending(false);
    }
  }

  async function onSocial(
    strategy: "oauth_google" | "oauth_apple" | "oauth_facebook",
  ) {
    if (!isLoaded || !signIn) {
      setFormError("Sign-in is not ready yet. Refresh the page and try again.");
      return;
    }
    setPending(true);
    setFormError("");
    try {
      stashRememberBeforeRedirect(remember);
      await signIn.authenticateWithRedirect({
        strategy,
        redirectUrl: "/sso-callback",
        redirectUrlComplete: "/dashboard",
      });
    } catch (err) {
      setFormError(clerkMessage(err));
      setPending(false);
    }
  }

  async function startReset(e: React.FormEvent) {
    e.preventDefault();
    if (!isLoaded || !signIn) return;
    if (!email.trim()) {
      setErrors({ email: "This field is required" });
      return;
    }
    setPending(true);
    setFormError("");
    try {
      await signIn.create({
        strategy: "reset_password_email_code",
        identifier: email.trim(),
      });
      setResetMode(true);
    } catch (err) {
      setFormError(clerkMessage(err));
    } finally {
      setPending(false);
    }
  }

  async function completeReset(e: React.FormEvent) {
    e.preventDefault();
    if (!isLoaded || !signIn) return;
    const next: Record<string, string> = {};
    if (!resetCode.trim()) next.resetCode = "This field is required";
    if (!newPassword) next.newPassword = "This field is required";
    setErrors(next);
    if (Object.keys(next).length) return;
    setPending(true);
    setFormError("");
    try {
      const result = await signIn.attemptFirstFactor({
        strategy: "reset_password_email_code",
        code: resetCode.trim(),
        password: newPassword,
      });
      if (result.status === "complete" && result.createdSessionId) {
        setRememberPreference(remember);
        await setActive({ session: result.createdSessionId });
        router.push("/dashboard");
        return;
      }
      setFormError("Could not reset password. Try again.");
    } catch (err) {
      setFormError(clerkMessage(err));
    } finally {
      setPending(false);
    }
  }

  if (resetMode) {
    return (
      <form className="space-y-4" onSubmit={completeReset} noValidate>
        <h2 className="text-center font-sans text-[16px] font-medium text-[#006633]">
          Reset your password
        </h2>
        <AuthAlert>{formError}</AuthAlert>
        <AuthField id="reset-code" label="Verification code" required error={errors.resetCode}>
          <AuthInput
            id="reset-code"
            value={resetCode}
            hasError={Boolean(errors.resetCode)}
            onChange={(e) => setResetCode(e.target.value)}
            autoComplete="one-time-code"
          />
        </AuthField>
        <AuthField id="new-password" label="New password" required error={errors.newPassword}>
          <AuthInput
            id="new-password"
            type="password"
            value={newPassword}
            hasError={Boolean(errors.newPassword)}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
          />
        </AuthField>
        <AuthPrimaryButton disabled={pending}>
          {pending ? "Updating…" : "Update password"}
        </AuthPrimaryButton>
        <button
          type="button"
          className="w-full font-sans text-[14px] text-[#006633] underline underline-offset-2"
          onClick={() => setResetMode(false)}
        >
          Back to sign in
        </button>
      </form>
    );
  }

  return (
    <form className="space-y-4" onSubmit={onSubmit} noValidate>
      <h2 className="text-center font-sans text-[16px] font-medium text-[#006633]">
        Sign in to Tennisly
      </h2>
      <AuthAlert>{formError}</AuthAlert>
      <AuthField id="email" label="Email" required error={errors.email}>
        <AuthInput
          id="email"
          type="email"
          value={email}
          hasError={Boolean(errors.email)}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
        />
      </AuthField>
      <AuthField id="password" label="Password" required error={errors.password}>
        <div className="relative">
          <AuthInput
            id="password"
            type={showPassword ? "text" : "password"}
            value={password}
            hasError={Boolean(errors.password)}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            className="pr-11"
          />
          <button
            type="button"
            aria-label={showPassword ? "Hide password" : "Show password"}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-[#697077] hover:text-[#121619]"
            onClick={() => setShowPassword((v) => !v)}
          >
            {showPassword ? (
              <EyeOffIcon className="h-5 w-5" />
            ) : (
              <EyeIcon className="h-5 w-5" />
            )}
          </button>
        </div>
      </AuthField>
      <div className="flex items-center justify-between gap-3">
        <label className="inline-flex items-center gap-2 font-sans text-[14px] text-[#121619]">
          <input
            type="checkbox"
            checked={remember}
            onChange={(e) => setRemember(e.target.checked)}
            className="h-4 w-4 accent-[#006633]"
          />
          Remember me
        </label>
        <button
          type="button"
          className="font-sans text-[14px] font-medium text-[#006633] underline underline-offset-2 hover:text-[#004225]"
          onClick={(e) => {
            void startReset(e);
          }}
        >
          Forgot password?
        </button>
      </div>
      <AuthPrimaryButton disabled={pending}>
        {pending ? "Signing in…" : "Log in"}
      </AuthPrimaryButton>
      <p className="text-center font-sans text-[12px] text-[#697077]">
        Sign in with
      </p>
      <SocialButtons onSelect={onSocial} disabled={pending} />
      <p className="pt-1 text-center font-sans text-[14px] text-[#697077]">
        Don&apos;t have an account?{" "}
        <Link
          href="/sign-up"
          className="font-medium text-[#006633] underline underline-offset-2 hover:text-[#004225]"
        >
          Sign up
        </Link>
      </p>
    </form>
  );
}
