import { Layout, Menu, Button, Drawer, Grid, Typography } from "antd";
import { MenuOutlined } from "@ant-design/icons";
import { useState } from "react";
import { useNavigate, useLocation, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { CadenceMark } from "@/components/CadenceMark";

const { Sider, Header, Content } = Layout;

const TEAM_MEMBER_ITEMS = [
  { key: "/reports", label: "My reports" },
  { key: "/reports/new", label: "New report" },
];

const MANAGER_ITEMS = [
  { key: "/dashboard", label: "Team dashboard" },
  { key: "/reports/all", label: "All reports" },
  { key: "/team-members", label: "Team members" },
  { key: "/projects", label: "Projects" },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const isMobile = !screens.md;
  const [isNavigationOpen, setNavigationOpen] = useState(false);

  const items = user?.role === "MANAGER" ? MANAGER_ITEMS : TEAM_MEMBER_ITEMS;

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  function handleNavigation(key: string) {
    navigate(key);
    setNavigationOpen(false);
  }

  const navigation = (
    <>
      <div style={{ padding: "24px 20px 4px" }}>
        <CadenceMark />
      </div>
      <Menu
        mode="inline"
        selectedKeys={[location.pathname]}
        items={items}
        onClick={({ key }) => handleNavigation(key)}
        style={{ borderRight: "none" }}
      />
    </>
  );

  return (
    <Layout style={{ minHeight: "100vh" }}>
      {!isMobile && (
        <Sider theme="light" width={220} style={{ borderRight: "1px solid #D8DCD6" }}>
          {navigation}
        </Sider>
      )}
      <Drawer
        title="Navigation"
        placement="left"
        width={260}
        open={isNavigationOpen}
        onClose={() => setNavigationOpen(false)}
        styles={{ body: { padding: 0 } }}
      >
        {navigation}
      </Drawer>
      <Layout style={{ minWidth: 0 }}>
        <Header
          style={{
            background: "#fff",
            borderBottom: "1px solid #D8DCD6",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: isMobile ? "0 16px" : "0 24px",
            gap: 12,
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8, minWidth: 0 }}>
            {isMobile && (
              <Button
                type="text"
                icon={<MenuOutlined />}
                aria-label="Open navigation"
                onClick={() => setNavigationOpen(true)}
              />
            )}
          <Typography.Text type="secondary" ellipsis style={{ minWidth: 0 }}>
            {user?.name} · {user?.role === "MANAGER" ? "Manager" : "Team member"}
          </Typography.Text>
          </div>
          <Button onClick={handleLogout}>Log out</Button>
        </Header>
        <Content style={{ padding: isMobile ? 16 : 24, minWidth: 0 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
