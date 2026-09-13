import type { ThemeConfig } from "antd";

// Maps antd's tokens onto the palette already established for the auth
// screens (paper/ink/teal/hairline) so the app doesn't suddenly switch to
// default antd blue once real pages start using its components.
export const cadenceTheme: ThemeConfig = {
  token: {
    colorPrimary: "#2F6F63",
    colorLink: "#2F6F63",
    colorBgLayout: "#F3F4F1",
    colorBorder: "#D8DCD6",
    colorText: "#1B2521",
    colorTextSecondary: "#5B665F",
    colorWarning: "#B8802E",
    fontFamily: "Inter, sans-serif",
    borderRadius: 2, // flat, matches the hairline/no-shadow look used on auth pages
  },
  components: {
    Layout: {
      siderBg: "#FFFFFF",
      headerBg: "#FFFFFF",
    },
    Menu: {
      itemSelectedBg: "#2F6F6318",
      itemSelectedColor: "#2F6F63",
    },
  },
};
