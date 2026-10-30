import { defineConfig } from "vite";
export default defineConfig({
  build: { outDir: "../src/main/resources/static", emptyOutDir: true },
  server: { proxy: { "/api": "http://localhost:8083" } },
  test: {
    environment: "jsdom",
    coverage: {
      provider: "v8",
      include: ["src/store.js"],
      thresholds: { lines: 80, functions: 80, statements: 80, branches: 80 },
    },
  },
});
