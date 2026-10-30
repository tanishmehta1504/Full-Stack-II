import React from "react";
import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen, cleanup, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import App from "./main";
import { store, clear, setToken } from "./store";
afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  store.dispatch(clear());
  setToken("");
});
function setup(role = "ADMIN") {
  const posts = [];
  const f = vi
    .spyOn(globalThis, "fetch")
    .mockImplementation(async (url, options) => {
      let data = {};
      if (url.endsWith("/login"))
        data = { token: "signed", user: { id: "u", name: "Demo User", role } };
      else if (url.endsWith("/workspace"))
        data = { posts: [...posts], counts: [], daily: [], activity: [] };
      else if (url.endsWith("/posts")) {
        const body = JSON.parse(options.body);
        posts.push({
          ...body,
          id: "p",
          owner_id: "u",
          author: "Demo User",
          status: "DRAFT",
          created_at: Date.now(),
        });
        data = { id: "p" };
      } else if (url.endsWith("/members")) data = [];
      return { ok: true, json: async () => data };
    });
  render(
    <Provider store={store}>
      <App />
    </Provider>,
  );
  return f;
}
async function login() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Password"), "Admin@12345");
  await user.click(screen.getByRole("button", { name: /Sign in/ }));
  await screen.findByText("Your ideas, in motion.");
  return user;
}
describe("Dashboard workflows", () => {
  it("signs in, saves a draft and displays it in the workspace", async () => {
    const f = setup();
    const user = await login();
    await user.click(screen.getByRole("button", { name: /Create post/ }));
    await user.type(
      screen.getByLabelText("Internal title"),
      "Community launch",
    );
    await user.type(
      screen.getByLabelText("Your content"),
      "Hello from our team",
    );
    await user.click(screen.getByRole("button", { name: "Save draft" }));
    await screen.findByText("Community launch");
    expect(
      f.mock.calls.some(
        ([url, options]) => url === "/api/posts" && options.method === "POST",
      ),
    ).toBe(true);
  });
  it("renders calendar, analytics and team navigation", async () => {
    setup();
    const user = await login();
    await user.click(screen.getByRole("button", { name: "Calendar" }));
    expect(screen.getByText("Sun")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Next month" }));
    await user.click(screen.getByRole("button", { name: "Analytics" }));
    expect(screen.getByText("Publishing rhythm")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Team" }));
    expect(screen.getByRole("button", { name: "Add member" })).toBeTruthy();
  });
  it("restricts viewer composition and returns to login on logout", async () => {
    setup("VIEWER");
    const user = await login();
    expect(
      screen.getByRole("button", { name: "Content studio" }).disabled,
    ).toBe(true);
    expect(screen.queryByRole("button", { name: /Create post/ })).toBeNull();
    await user.click(screen.getByRole("button", { name: /Demo User viewer/ }));
    await screen.findByText("Welcome back.");
  });
});
