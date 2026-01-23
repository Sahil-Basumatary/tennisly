export default function ColorsDemo() {
  const brandColors = [
    { name: "court-green", hex: "#006633" },
    { name: "grass-light", hex: "#228B22" },
    { name: "grass-dark", hex: "#004225" },
    { name: "royal-purple", hex: "#4B0082" },
    { name: "purple-accent", hex: "#6B238E" },
    { name: "championship-gold", hex: "#C9A227" },
    { name: "cream-white", hex: "#FFFEF2" },
    { name: "deep-navy", hex: "#1A1A2E" },
    { name: "charcoal", hex: "#2D2D2D" },
    { name: "soft-gray", hex: "#F5F5F5" },
  ];
  const semanticColors = [
    { name: "primary", hsl: "153 100% 20%" },
    { name: "secondary", hsl: "275 100% 25%" },
    { name: "accent", hsl: "43 69% 47%" },
    { name: "destructive", hsl: "0 84% 60%" },
    { name: "muted", hsl: "0 0% 96%" },
    { name: "card", hsl: "60 100% 97%" },
    { name: "popover", hsl: "60 100% 97%" },
  ];
  return (
    <div className="min-h-screen p-8" style={{ background: "#FFFEF2" }}>
      <h1 className="text-3xl font-bold mb-8" style={{ color: "#2D2D2D" }}>Tennisly Color Palette</h1>
      <h2 className="text-xl font-semibold mb-4" style={{ color: "#2D2D2D" }}>Brand Colors</h2>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4 mb-12">
        {brandColors.map((color) => (
          <div key={color.name} className="flex flex-col items-center">
            <div
              className="w-24 h-24 rounded-lg shadow-lg"
              style={{ backgroundColor: color.hex, border: color.hex === "#FFFEF2" || color.hex === "#F5F5F5" ? "1px solid #ccc" : "none" }}
            />
            <span className="mt-2 text-sm font-medium" style={{ color: "#2D2D2D" }}>{color.name}</span>
            <span className="text-xs" style={{ color: "#666" }}>{color.hex}</span>
          </div>
        ))}
      </div>
      <h2 className="text-xl font-semibold mb-4" style={{ color: "#2D2D2D" }}>Semantic Colors (Light Mode)</h2>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {semanticColors.map((color) => (
          <div key={color.name} className="flex flex-col items-center">
            <div
              className="w-24 h-24 rounded-lg shadow-lg"
              style={{ backgroundColor: `hsl(${color.hsl})`, border: color.name === "muted" || color.name === "card" || color.name === "popover" ? "1px solid #ccc" : "none" }}
            />
            <span className="mt-2 text-sm font-medium" style={{ color: "#2D2D2D" }}>{color.name}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
