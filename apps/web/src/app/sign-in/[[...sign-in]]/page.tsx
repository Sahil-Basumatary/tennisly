import { SignIn } from "@clerk/nextjs";

export default function SignInPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="absolute inset-0 bg-gradient-to-br from-court-green/10 via-transparent to-championship-gold/10 dark:from-court-green/5 dark:to-championship-gold/5" />
      <div className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-championship-gold/10 blur-3xl" />
      <div className="absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-court-green/10 blur-3xl" />
      <div className="relative z-10">
        <SignIn
          appearance={{
            elements: {
              rootBox: "mx-auto",
              card: "bg-card border border-border shadow-lg",
              headerTitle: "font-display text-foreground",
              headerSubtitle: "text-muted-foreground",
              formButtonPrimary:
                "bg-primary hover:bg-primary/90 text-primary-foreground",
              formFieldInput:
                "bg-background border-input text-foreground focus:ring-ring",
              footerActionLink: "text-primary hover:text-primary/80",
              identityPreviewEditButton: "text-primary",
              formFieldLabel: "text-foreground",
              dividerLine: "bg-border",
              dividerText: "text-muted-foreground",
              socialButtonsBlockButton:
                "border-border text-foreground hover:bg-muted",
              socialButtonsBlockButtonText: "text-foreground",
            },
          }}
        />
      </div>
    </div>
  );
}
