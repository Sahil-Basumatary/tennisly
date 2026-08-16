"use client";

import { useSignUp } from "@clerk/nextjs";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { EyeIcon, EyeOffIcon } from "@/components/ui/brandIcons";
import {
  ACCOUNT_TITLES,
  COUNTRIES,
  MONTHS,
  ageGateHint,
  birthYearOptions,
  isOldEnough,
} from "@/components/auth/constants";
import {
  AuthAlert,
  AuthField,
  AuthInput,
  AuthPrimaryButton,
  AuthSelect,
  TermsBox,
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

type FormState = {
  email: string;
  password: string;
  confirm: string;
  title: string;
  firstName: string;
  lastName: string;
  country: string;
  postcode: string;
  birthDay: string;
  birthMonth: string;
  birthYear: string;
  terms: boolean;
};

const empty: FormState = {
  email: "",
  password: "",
  confirm: "",
  title: "",
  firstName: "",
  lastName: "",
  country: "",
  postcode: "",
  birthDay: "",
  birthMonth: "",
  birthYear: "",
  terms: false,
};

export function SignUpForm() {
  const router = useRouter();
  const { isLoaded, signUp, setActive } = useSignUp();
  const [form, setForm] = useState<FormState>(empty);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState("");
  const [pending, setPending] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [code, setCode] = useState("");
  const years = useMemo(() => birthYearOptions(), []);
  const ageHint = useMemo(() => ageGateHint(), []);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function validate() {
    const next: Record<string, string> = {};
    if (!form.email.trim()) next.email = "This field is required";
    if (!form.password) next.password = "This field is required";
    if (!form.confirm) next.confirm = "This field is required";
    else if (form.confirm !== form.password)
      next.confirm = "Passwords do not match";
    if (!form.title) next.title = "This field is required";
    if (!form.firstName.trim()) next.firstName = "This field is required";
    if (!form.lastName.trim()) next.lastName = "This field is required";
    if (!form.country) next.country = "This field is required";
    if (!form.postcode.trim()) next.postcode = "This field is required";
    if (!form.birthDay) next.birthDay = "This field is required";
    if (!form.birthMonth) next.birthMonth = "This field is required";
    if (!form.birthYear) next.birthYear = "This field is required";
    else if (!isOldEnough(form.birthDay, form.birthMonth, form.birthYear))
      next.birthYear = ageHint;
    if (!form.terms) next.terms = "This field is required";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isLoaded || !signUp) {
      setFormError("Sign-up is not ready yet. Refresh the page and try again.");
      return;
    }
    if (!validate()) return;
    setPending(true);
    setFormError("");
    try {
      await signUp.create({
        emailAddress: form.email.trim(),
        password: form.password,
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        unsafeMetadata: {
          title: form.title,
          country: form.country,
          postcode: form.postcode.trim(),
          birthDay: form.birthDay,
          birthMonth: form.birthMonth,
          birthYear: form.birthYear,
          termsAcceptedAt: new Date().toISOString(),
        },
      });
      await signUp.prepareEmailAddressVerification({ strategy: "email_code" });
      setVerifying(true);
    } catch (err) {
      setFormError(clerkMessage(err));
    } finally {
      setPending(false);
    }
  }

  async function onVerify(e: React.FormEvent) {
    e.preventDefault();
    if (!isLoaded || !signUp) return;
    if (!code.trim()) {
      setErrors({ code: "This field is required" });
      return;
    }
    setPending(true);
    setFormError("");
    try {
      const result = await signUp.attemptEmailAddressVerification({
        code: code.trim(),
      });
      if (result.status === "complete" && result.createdSessionId) {
        setRememberPreference(true);
        await setActive({ session: result.createdSessionId });
        router.push("/dashboard");
        return;
      }
      setFormError("Verification incomplete. Try again.");
    } catch (err) {
      setFormError(clerkMessage(err));
    } finally {
      setPending(false);
    }
  }

  async function onSocial(
    strategy: "oauth_google" | "oauth_apple" | "oauth_facebook",
  ) {
    if (!isLoaded || !signUp) {
      setFormError("Sign-up is not ready yet. Refresh the page and try again.");
      return;
    }
    setPending(true);
    setFormError("");
    try {
      stashRememberBeforeRedirect(true);
      await signUp.authenticateWithRedirect({
        strategy,
        redirectUrl: "/sso-callback",
        redirectUrlComplete: "/dashboard",
      });
    } catch (err) {
      setFormError(clerkMessage(err));
      setPending(false);
    }
  }

  if (verifying) {
    return (
      <form className="space-y-4" onSubmit={onVerify} noValidate>
        <h2 className="text-center font-sans text-[16px] font-medium text-[#006633]">
          Verify your email
        </h2>
        <p className="font-sans text-[14px] text-[#697077]">
          Enter the code we sent to {form.email.trim()}.
        </p>
        <AuthAlert>{formError}</AuthAlert>
        <AuthField id="code" label="Verification code" required error={errors.code}>
          <AuthInput
            id="code"
            value={code}
            hasError={Boolean(errors.code)}
            onChange={(e) => setCode(e.target.value)}
            autoComplete="one-time-code"
          />
        </AuthField>
        <AuthPrimaryButton disabled={pending}>
          {pending ? "Verifying…" : "Continue"}
        </AuthPrimaryButton>
      </form>
    );
  }

  return (
    <form className="space-y-4" onSubmit={onSubmit} noValidate>
      <h2 className="text-center font-sans text-[16px] font-medium text-[#006633]">
        Join Tennisly
      </h2>
      <AuthAlert>{formError}</AuthAlert>
      <AuthField id="email" label="Email" required error={errors.email}>
        <AuthInput
          id="email"
          type="email"
          value={form.email}
          hasError={Boolean(errors.email)}
          onChange={(e) => set("email", e.target.value)}
          autoComplete="email"
        />
      </AuthField>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AuthField id="password" label="Password" required error={errors.password}>
          <div className="relative">
            <AuthInput
              id="password"
              type={showPassword ? "text" : "password"}
              value={form.password}
              hasError={Boolean(errors.password)}
              onChange={(e) => set("password", e.target.value)}
              autoComplete="new-password"
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
        <AuthField
          id="confirm"
          label="Confirm Password"
          required
          error={errors.confirm}
        >
          <div className="relative">
            <AuthInput
              id="confirm"
              type={showConfirm ? "text" : "password"}
              value={form.confirm}
              hasError={Boolean(errors.confirm)}
              onChange={(e) => set("confirm", e.target.value)}
              autoComplete="new-password"
              className="pr-11"
            />
            <button
              type="button"
              aria-label={showConfirm ? "Hide password" : "Show password"}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#697077] hover:text-[#121619]"
              onClick={() => setShowConfirm((v) => !v)}
            >
              {showConfirm ? (
                <EyeOffIcon className="h-5 w-5" />
              ) : (
                <EyeIcon className="h-5 w-5" />
              )}
            </button>
          </div>
        </AuthField>
      </div>
      <h3 className="pt-2 text-center font-sans text-[16px] font-medium text-[#006633]">
        Complete Registration
      </h3>
      <AuthField id="title" label="Title" required error={errors.title}>
        <AuthSelect
          id="title"
          value={form.title}
          hasError={Boolean(errors.title)}
          onChange={(e) => set("title", e.target.value)}
        >
          <option value="">-- Select Title --</option>
          {ACCOUNT_TITLES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </AuthSelect>
      </AuthField>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AuthField id="firstName" label="First Name" required error={errors.firstName}>
          <AuthInput
            id="firstName"
            value={form.firstName}
            hasError={Boolean(errors.firstName)}
            onChange={(e) => set("firstName", e.target.value)}
            autoComplete="given-name"
          />
        </AuthField>
        <AuthField id="lastName" label="Last Name" required error={errors.lastName}>
          <AuthInput
            id="lastName"
            value={form.lastName}
            hasError={Boolean(errors.lastName)}
            onChange={(e) => set("lastName", e.target.value)}
            autoComplete="family-name"
          />
        </AuthField>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <AuthField
          id="country"
          label="Country of residence"
          required
          error={errors.country}
        >
          <AuthSelect
            id="country"
            value={form.country}
            hasError={Boolean(errors.country)}
            onChange={(e) => set("country", e.target.value)}
          >
            <option value="">-- Select a Country --</option>
            {COUNTRIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </AuthSelect>
        </AuthField>
        <AuthField id="postcode" label="Postcode" required error={errors.postcode}>
          <AuthInput
            id="postcode"
            value={form.postcode}
            hasError={Boolean(errors.postcode)}
            onChange={(e) => set("postcode", e.target.value)}
            autoComplete="postal-code"
          />
        </AuthField>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <AuthField id="birthDay" label="Birth Day" required error={errors.birthDay}>
          <AuthSelect
            id="birthDay"
            value={form.birthDay}
            hasError={Boolean(errors.birthDay)}
            onChange={(e) => set("birthDay", e.target.value)}
          >
            <option value="">Day</option>
            {Array.from({ length: 31 }, (_, i) => i + 1).map((d) => (
              <option key={d} value={String(d)}>
                {d}
              </option>
            ))}
          </AuthSelect>
        </AuthField>
        <AuthField
          id="birthMonth"
          label="Birth Month"
          required
          error={errors.birthMonth}
        >
          <AuthSelect
            id="birthMonth"
            value={form.birthMonth}
            hasError={Boolean(errors.birthMonth)}
            onChange={(e) => set("birthMonth", e.target.value)}
          >
            <option value="">Month</option>
            {MONTHS.map((m) => (
              <option key={m.value} value={m.value}>
                {m.label}
              </option>
            ))}
          </AuthSelect>
        </AuthField>
        <AuthField id="birthYear" label="Birth Year" required error={errors.birthYear}>
          <AuthSelect
            id="birthYear"
            value={form.birthYear}
            hasError={Boolean(errors.birthYear)}
            onChange={(e) => set("birthYear", e.target.value)}
          >
            <option value="">Year</option>
            {years.map((y) => (
              <option key={y} value={String(y)}>
                {y}
              </option>
            ))}
          </AuthSelect>
        </AuthField>
      </div>
      <p className="font-sans text-[12px] text-[#697077]">{ageHint}</p>
      <TermsBox
        id="terms"
        checked={form.terms}
        onChange={(v) => set("terms", v)}
        error={errors.terms}
      />
      <AuthPrimaryButton disabled={pending}>
        {pending ? "Creating account…" : "Create account"}
      </AuthPrimaryButton>
      <p className="text-center font-sans text-[12px] text-[#697077]">
        Or continue with
      </p>
      <SocialButtons onSelect={onSocial} disabled={pending} />
      <p className="pt-1 text-center font-sans text-[14px] text-[#697077]">
        Already have an account?{" "}
        <Link
          href="/sign-in"
          className="font-medium text-[#006633] underline underline-offset-2 hover:text-[#004225]"
        >
          Sign in
        </Link>
      </p>
    </form>
  );
}
