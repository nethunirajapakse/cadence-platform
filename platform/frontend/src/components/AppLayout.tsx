import { Layout, Menu, Button, Typography } from "antd";
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
  { key: "/projects", label: "Projects" },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const items = user?.role === "MANAGER" ? MANAGER_ITEMS : TEAM_MEMBER_ITEMS;

  async function handleLogout() {
    await logout();
    navigate("/login");
  }

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider theme="light" width={220} style={{ borderRight: "1px solid #D8DCD6" }}>
        <div style={{ padding: "24px 20px 4px" }}>
          <CadenceMark />
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={items}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: "none" }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: "#fff",
            borderBottom: "1px solid #D8DCD6",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 24px",
          }}
        >
          <Typography.Text type="secondary">
            {user?.name} · {user?.role === "MANAGER" ? "Manager" : "Team member"}
          </Typography.Text>
          <Button onClick={handleLogout}>Log out</Button>
        </Header>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
