import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/reports": "http://localhost:8000",
      "/output": "http://localhost:8000",
      "/evaluate": "http://localhost:8000",
    },
  },
});
