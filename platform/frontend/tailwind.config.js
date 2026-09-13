/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        paper: "#f3f4f1",
        ink: "#1b2521",
        muted: "#5b665f",
        hairline: "#d8dcd6",
        teal: "#2f6f63",
        ochre: "#b8802e",
      },
      fontFamily: {
        display: ["Space Grotesk", "sans-serif"],
        sans: ["Inter", "sans-serif"],
      },
    },
  },
  plugins: [],
}

