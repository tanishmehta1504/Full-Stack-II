import React from "react";
import { createRoot } from "react-dom/client";
import { Provider } from "react-redux";
import { store } from "./store";
import App from "./main";
createRoot(document.getElementById("root")).render(
  <Provider store={store}>
    <App />
  </Provider>,
);
