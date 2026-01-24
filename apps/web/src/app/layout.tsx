import type { Metadata } from "next";
import { playfair, cormorant, lato } from "@/lib/fonts";
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
    <html lang="en" className={`${playfair.variable} ${cormorant.variable} ${lato.variable}`}>
      <body className="font-sans antialiased">
        {children}
      </body>
    </html>
  );
}
