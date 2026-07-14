import type { Metadata } from "next";
import { montserrat, montserratDisplay, robotoCondensed } from "@/lib/fonts";
import { ClerkProvider } from "@/components/providers/clerk-provider";
import { ThemeProvider } from "@/components/providers/theme-provider";
import { SiteChrome } from "@/components/layout/SiteChrome";
import "./globals.css";

export const metadata: Metadata = {
  title: "Tennisly",
  description: "Your premium tennis companion",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <ClerkProvider>
      <html
        lang="en"
        suppressHydrationWarning
        className={`${montserrat.variable} ${montserratDisplay.variable} ${robotoCondensed.variable}`}
      >
        <body className="font-sans antialiased">
          <ThemeProvider>
            <SiteChrome>{children}</SiteChrome>
          </ThemeProvider>
        </body>
      </html>
    </ClerkProvider>
  );
}
