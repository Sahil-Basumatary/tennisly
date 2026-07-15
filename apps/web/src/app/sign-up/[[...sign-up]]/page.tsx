import { AuthModal } from "@/components/auth/AuthModal";
import HomePage from "@/app/page";

export default function SignUpPage() {
  return (
    <>
      <div className="pointer-events-none select-none" aria-hidden>
        <HomePage />
      </div>
      <AuthModal mode="sign-up" />
    </>
  );
}
