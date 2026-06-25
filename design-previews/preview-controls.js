(function () {
  const root = document.documentElement;
  const screens = [...document.querySelectorAll(".screen")];

  const presets = {
    fog: {
      light: {
        "--bg1": "#f5f7fa",
        "--bg2": "#eff3f8",
        "--bg3": "#e8edf4",
        "--surface": "#ffffff",
        "--card": "#ffffff",
        "--line": "rgba(130, 150, 180, 0.18)",
        "--text": "#2c3344",
        "--muted": "#5a6577",
        "--faint": "#8a96a8",
        "--accent": "#6b8db5",
        "--accent2": "#b5926b",
        "--accent3": "#9b8eba",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#0b0d12",
        "--bg2": "#0e1018",
        "--bg3": "#11131c",
        "--surface": "#161822",
        "--card": "#181a26",
        "--line": "rgba(130, 150, 180, 0.22)",
        "--text": "#d8e0f0",
        "--muted": "#9aa8c0",
        "--faint": "#6a7890",
        "--accent": "#88b0d4",
        "--accent2": "#d4a878",
        "--accent3": "#a89ac8",
        "--noise": "0.14"
      }
    },
    moss: {
      light: {
        "--bg1": "#f6f7f4",
        "--bg2": "#eff2eb",
        "--bg3": "#e5eae0",
        "--surface": "#fcfdfb",
        "--card": "#fcfdfb",
        "--line": "rgba(100, 122, 90, 0.18)",
        "--text": "#2e3328",
        "--muted": "#5a6450",
        "--faint": "#8a9480",
        "--accent": "#7ba06e",
        "--accent2": "#c4a06b",
        "--accent3": "#a088b0",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#0b0d0a",
        "--bg2": "#0e110d",
        "--bg3": "#111410",
        "--surface": "#161a14",
        "--card": "#181c16",
        "--line": "rgba(160, 184, 144, 0.18)",
        "--text": "#d8e4d0",
        "--muted": "#9aaa90",
        "--faint": "#6a7a60",
        "--accent": "#8bc07a",
        "--accent2": "#d4b078",
        "--accent3": "#b098c4",
        "--noise": "0.14"
      }
    },
    ocean: {
      light: {
        "--bg1": "#f2fbfc",
        "--bg2": "#e8f6fa",
        "--bg3": "#e2f0f6",
        "--surface": "#ffffff",
        "--card": "#ffffff",
        "--line": "rgba(123, 184, 199, 0.18)",
        "--text": "#173d46",
        "--muted": "#4f6e79",
        "--faint": "#7f99a3",
        "--accent": "#0f8b8d",
        "--accent2": "#2f74d0",
        "--accent3": "#6b87b8",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#091318",
        "--bg2": "#0c1820",
        "--bg3": "#101e29",
        "--surface": "#121b24",
        "--card": "#15212b",
        "--line": "rgba(125, 183, 197, 0.20)",
        "--text": "#d9eef0",
        "--muted": "#9ab7c0",
        "--faint": "#6d8992",
        "--accent": "#39b8c0",
        "--accent2": "#5a8ddb",
        "--accent3": "#7d8fc7",
        "--noise": "0.14"
      }
    },
    petal: {
      light: {
        "--bg1": "#fff8f7",
        "--bg2": "#ffefed",
        "--bg3": "#f8e6e3",
        "--surface": "#ffffff",
        "--card": "#ffffff",
        "--line": "rgba(211, 122, 110, 0.18)",
        "--text": "#422a2a",
        "--muted": "#72585a",
        "--faint": "#9b8384",
        "--accent": "#d36a5e",
        "--accent2": "#b56aa8",
        "--accent3": "#dd9478",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#181013",
        "--bg2": "#1e1216",
        "--bg3": "#27181b",
        "--surface": "#221619",
        "--card": "#281a1d",
        "--line": "rgba(224, 141, 132, 0.18)",
        "--text": "#f2dcdc",
        "--muted": "#c3a7a7",
        "--faint": "#9d7f7f",
        "--accent": "#e38579",
        "--accent2": "#c57bc7",
        "--accent3": "#e2a67e",
        "--noise": "0.14"
      }
    },
    sand: {
      light: {
        "--bg1": "#fcfaf3",
        "--bg2": "#f7f1e6",
        "--bg3": "#f0e5d3",
        "--surface": "#ffffff",
        "--card": "#ffffff",
        "--line": "rgba(201, 163, 94, 0.18)",
        "--text": "#42361d",
        "--muted": "#6c5c40",
        "--faint": "#94805f",
        "--accent": "#c58a32",
        "--accent2": "#cc6b4f",
        "--accent3": "#9b8360",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#17140f",
        "--bg2": "#1d1812",
        "--bg3": "#251f17",
        "--surface": "#231d16",
        "--card": "#2a221a",
        "--line": "rgba(207, 165, 91, 0.18)",
        "--text": "#f0e5d4",
        "--muted": "#c0ad8a",
        "--faint": "#a08b6c",
        "--accent": "#d9a24a",
        "--accent2": "#d98367",
        "--accent3": "#ad9271",
        "--noise": "0.14"
      }
    },
    clay: {
      light: {
        "--bg1": "#fbf5f0",
        "--bg2": "#f4e7dd",
        "--bg3": "#ead7cb",
        "--surface": "#fffcfa",
        "--card": "#fffcfa",
        "--line": "rgba(196, 119, 95, 0.18)",
        "--text": "#40302a",
        "--muted": "#6c574d",
        "--faint": "#9a8579",
        "--accent": "#c4775f",
        "--accent2": "#779b87",
        "--accent3": "#9b84ad",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#17120f",
        "--bg2": "#1c1511",
        "--bg3": "#241b16",
        "--surface": "#211713",
        "--card": "#281d18",
        "--line": "rgba(208, 139, 115, 0.18)",
        "--text": "#f2e3da",
        "--muted": "#c3aea0",
        "--faint": "#8d7769",
        "--accent": "#d08b73",
        "--accent2": "#87a992",
        "--accent3": "#ab94c0",
        "--noise": "0.14"
      }
    },
    ink: {
      light: {
        "--bg1": "#f3f5fa",
        "--bg2": "#e7ebf5",
        "--bg3": "#dce3f0",
        "--surface": "#ffffff",
        "--card": "#ffffff",
        "--line": "rgba(77, 106, 168, 0.18)",
        "--text": "#1f2735",
        "--muted": "#506176",
        "--faint": "#7a8698",
        "--accent": "#4d6aa8",
        "--accent2": "#6e83c4",
        "--accent3": "#9a7faf",
        "--noise": "0.10"
      },
      dark: {
        "--bg1": "#0a0d14",
        "--bg2": "#0e121b",
        "--bg3": "#121824",
        "--surface": "#161b27",
        "--card": "#1a2030",
        "--line": "rgba(138, 168, 224, 0.18)",
        "--text": "#d9e1ef",
        "--muted": "#9aa8c1",
        "--faint": "#6d7893",
        "--accent": "#8aa8e0",
        "--accent2": "#a48bd4",
        "--accent3": "#6fb0c2",
        "--noise": "0.14"
      }
    }
  };

  function applyPresetToElement(element, preset) {
    Object.entries(preset).forEach(([key, value]) => element.style.setProperty(key, value));
  }

  function applyTheme(theme, mode) {
    const normalizedTheme = theme === "lake" ? "ocean" : theme;
    const preset = presets[normalizedTheme]?.[mode] || presets.fog.light;
    root.dataset.theme = normalizedTheme;
    root.dataset.mode = mode;
    applyPresetToElement(root, preset);
    screens.forEach((screen) => applyPresetToElement(screen, preset));
  }

  function syncButtons(buttons, attr, value) {
    buttons.forEach((button) => {
      button.classList.toggle("active", button.getAttribute(attr) === value);
      button.setAttribute("aria-pressed", button.classList.contains("active") ? "true" : "false");
    });
  }

  const modeButtons = [...document.querySelectorAll("[data-mode]")];
  const themeButtons = [...document.querySelectorAll("[data-theme]")];
  const initialMode = modeButtons.find((button) => button.classList.contains("active"))?.getAttribute("data-mode") || "light";
  const initialTheme = themeButtons.find((button) => button.classList.contains("active"))?.getAttribute("data-theme") || "fog";

  applyTheme(initialTheme, initialMode);
  syncButtons(modeButtons, "data-mode", initialMode);
  syncButtons(themeButtons, "data-theme", initialTheme);

  modeButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const mode = button.getAttribute("data-mode") || "light";
      const theme = root.dataset.theme || initialTheme;
      applyTheme(theme, mode);
      syncButtons(modeButtons, "data-mode", mode);
    });
  });

  themeButtons.forEach((button) => {
    button.addEventListener("click", () => {
      const theme = button.getAttribute("data-theme") || initialTheme;
      const mode = root.dataset.mode || initialMode;
      applyTheme(theme, mode);
      syncButtons(themeButtons, "data-theme", theme);
    });
  });
})();
