import { Button, Space } from "antd";
import type { ReactNode } from "react";

// Shared shape for antd Table column filterDropdowns - renders whatever
// filter control is passed in, plus a consistent Reset/Done pair, so every
// filterable column across the app behaves the same way regardless of
// whether it wraps a Select, a text Input, a RangePicker, etc.
export function makeFilterDropdown(
  content: (close: () => void) => ReactNode,
  onReset: () => void
) {
  return ({ close }: { close: () => void }) => (
    <div style={{ padding: 12, width: 260 }} onKeyDown={(e) => e.stopPropagation()}>
      {content(close)}
      <Space style={{ marginTop: 8, display: "flex", justifyContent: "flex-end" }}>
        <Button
          size="small"
          onClick={() => {
            onReset();
            close();
          }}
        >
          Reset
        </Button>
        <Button size="small" type="primary" onClick={close}>
          Done
        </Button>
      </Space>
    </div>
  );
}
