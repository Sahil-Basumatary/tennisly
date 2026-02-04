import type { Metadata } from "next";
import { playfair, cormorant, lato } from "@/lib/fonts";
import { ClerkProvider } from "@/components/providers/clerk-provider";
import { ThemeProvider } from "@/components/providers/theme-provider";
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
        className={`${playfair.variable} ${cormorant.variable} ${lato.variable}`}
      >
        <body className="font-sans antialiased">
          <ThemeProvider>
            {children}
          </ThemeProvider>
        </body>
      </html>
    </ClerkProvider>
  );
}
