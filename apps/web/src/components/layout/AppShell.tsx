import type { ReactNode } from "react";

type AppShellProps = {
  ticker?: ReactNode;
  nav?: ReactNode;
  subnav?: ReactNode;
  footer?: ReactNode;
  children: ReactNode;
};

export function AppShell({
  ticker,
  nav,
  subnav,
  footer,
  children,
}: AppShellProps) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      {ticker}
      {nav}
      {subnav}
      <div className="flex-1">{children}</div>
      {footer}
    </div>
  );
}
