"use client";

import Link from "next/link";
import { useEffect } from "react";
import { CloseIcon } from "@/components/ui/brandIcons";
import { SignInForm } from "@/components/auth/SignInForm";
import { SignUpForm } from "@/components/auth/SignUpForm";

type AuthModalProps = {
  mode: "sign-in" | "sign-up";
};

export function AuthModal({ mode }: AuthModalProps) {
  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, []);

  return (
    <div
      className="fixed inset-0 z-[80] flex items-start justify-center overflow-y-auto bg-[#121619]/55 px-4 py-10 sm:items-center sm:py-12"
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-modal-brand"
    >
      <div className="relative w-full max-w-[520px] bg-white shadow-[0_12px_40px_rgba(18,22,25,0.28)]">
        <div className="flex items-start justify-between border-b border-[#dde1e6] px-6 py-5 sm:px-8">
          <h1
            id="auth-modal-brand"
            className="font-display text-[28px] font-bold leading-none tracking-tight text-[#121619] sm:text-[32px]"
          >
            Tennisly
          </h1>
          <Link
            href="/"
            aria-label="Close authentication modal"
            className="inline-flex h-9 w-9 items-center justify-center text-[#121619] transition-colors hover:bg-[#f2f4f8]"
          >
            <CloseIcon className="h-5 w-5" />
          </Link>
        </div>
        <div className="max-h-[min(78vh,720px)] overflow-y-auto px-6 py-6 sm:px-8 sm:py-7">
          {mode === "sign-in" ? <SignInForm /> : <SignUpForm />}
        </div>
      </div>
    </div>
  );
}
