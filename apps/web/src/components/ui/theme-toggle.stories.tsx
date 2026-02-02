import type { Meta, StoryObj } from "@storybook/nextjs";
import { ThemeToggle } from "./theme-toggle";

const meta = {
  title: "Components/ThemeToggle",
  component: ThemeToggle,
  parameters: {
    layout: "centered",
  },
  tags: ["autodocs"],
} satisfies Meta<typeof ThemeToggle>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const WithLabel: Story = {
  decorators: [
    (Story) => (
      <div className="flex items-center gap-3">
        <span className="text-sm text-muted-foreground">Toggle theme:</span>
        <Story />
      </div>
    ),
  ],
};



