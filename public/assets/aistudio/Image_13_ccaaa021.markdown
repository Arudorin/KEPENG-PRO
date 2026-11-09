---
name: KEPENG
colors:
  surface: '#131316'
  surface-dim: '#131316'
  surface-bright: '#39393c'
  surface-container-lowest: '#0e0e11'
  surface-container-low: '#1b1b1e'
  surface-container: '#1f1f22'
  surface-container-high: '#2a2a2d'
  surface-container-highest: '#353438'
  on-surface: '#e4e1e6'
  on-surface-variant: '#e8bcb9'
  inverse-surface: '#e4e1e6'
  inverse-on-surface: '#303033'
  outline: '#ae8785'
  outline-variant: '#5e3f3d'
  surface-tint: '#ffb3af'
  primary: '#ffb3af'
  on-primary: '#68000d'
  primary-container: '#ff5355'
  on-primary-container: '#5c000a'
  inverse-primary: '#bf0021'
  secondary: '#ffb3ae'
  on-secondary: '#68000c'
  secondary-container: '#92121c'
  on-secondary-container: '#ff9e98'
  tertiary: '#00e297'
  on-tertiary: '#003822'
  tertiary-container: '#00a56d'
  on-tertiary-container: '#00311d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad7'
  primary-fixed-dim: '#ffb3af'
  on-primary-fixed: '#410005'
  on-primary-fixed-variant: '#930017'
  secondary-fixed: '#ffdad7'
  secondary-fixed-dim: '#ffb3ae'
  on-secondary-fixed: '#410004'
  on-secondary-fixed-variant: '#8f0f1a'
  tertiary-fixed: '#4dffb2'
  tertiary-fixed-dim: '#00e297'
  on-tertiary-fixed: '#002112'
  on-tertiary-fixed-variant: '#005234'
  background: '#131316'
  on-background: '#e4e1e6'
  surface-variant: '#353438'
typography:
  display:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 38px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.005em
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.08em
  mono-metric:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: -0.01em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

The design system embodies a tactical, sovereign, and razor-sharp personal finance ecosystem. Built specifically for an offline-first architecture, the interface communicates complete data ownership, discipline, and uncompromising privacy. It avoids playful, rounded consumer-fintech cliches in favor of an authoritative, high-density cockpit aesthetic that feels purposeful, masculine, and sophisticated.

Visual direction pairs hyper-clean technical minimalism with high-contrast tactical styling:
- **Surface Depth:** Pitch black and deep obsidian obsidian charcoal tones form an immersive background that maximizes OLED contrast and minimizes battery draw.
- **Accents:** High-voltage crimson acts as a calculated surgical tool rather than decorative filler—used exclusively for primary execution pathways, deficit alerts, and acute financial signals.
- **Atmosphere:** Controlled structural hierarchy, fine hairlines, and information density tailored for immediate legibility and rapid offline expense logging.

## Colors

The palette leverages pure value contrast across deep neutral tones, reserving saturated chromatic hues strictly for operational states and financial metrics:

- **Primary (`#FF2A3B`):** Blood crimson neon. Used for the primary call-to-action, active indicators, debt/expense callouts, and key focus indicators.
- **Secondary (`#8E0E19`):** Deep carmine. Serves as interactive hover/pressed states, subtle container fills for primary elements, and muted borders.
- **Tertiary (`#00E599`):** Tactical mint green. Used selectively to signify cash inflows, positive cash flow, and asset growth without clashing with the dominant red theme.
- **Neutral Core:**
  - `bg-root`: `#0D0D0F` (Base screen substrate)
  - `surface-base`: `#121215` (Cards, drawers, panels)
  - `surface-elevated`: `#1A1A1E` (Dropdowns, modals, floating elements)
  - `surface-highlight`: `#26262C` (Subtle hover fills, input fields)
- **Text & Contrast:**
  - `text-primary`: `#FFFFFF` (100% white, highest tier hierarchy)
  - `text-secondary`: `#A0A0AA` (Muted labels, metadata, inactive items)
  - `text-tertiary`: `#63636E` (Placeholders, disabled states, grid lines)
- **Structural Outlines:** `#27272F` (1px clean separation lines across surfaces)

## Typography

The type system balances the dynamic authority of **Plus Jakarta Sans** for dominant values and titles with the disciplined utility of **Inter** for dense transactional records.

- **Financial Numerals:** All numeric financial amounts (balances, entries, analytics) mandate tabular lining figures (`font-variant-numeric: tabular-nums`) to guarantee strict vertical alignment across ledgers.
- **Hierarchy Rules:** `label-caps` is always paired with uppercase transformations for categorizations, account types, and offline sync status badges.
- **Display Scale:** On viewports below 480px width, `display-mobile` takes precedence over `display` to preserve line wraps on multi-digit currency balances.

## Layout & Spacing

The layout is built upon an 8pt architectural rhythm, optimized for rapid single-handed thumb operation on mobile and multi-column telemetry on desktop:

- **Mobile (<768px):** Single-column layout with fixed bottom navigation or thumb-accessible quick-add FAB. Screen margins stay tight at `1rem` to optimize usable data density.
- **Tablet (768px - 1024px):** 6-column fluid structure with side-by-side ledger history and category analytics.
- **Desktop (>1024px):** 12-column fixed grid (max canvas `1280px`), featuring persistent vertical rail navigation, main transaction ledger, and summary detail panel.
- **Density Profile:** Component padding favors vertical compactness (`space-sm` / `space-md`) to ensure high visible volume of transaction rows without scrolling.

## Elevation & Depth

Visual depth avoids realistic diffuse shadows, relying on layered luminosity and microscopic hairline borders:

- **Layer 0 (Substrate):** `#0D0D0F` — The root viewport background.
- **Layer 1 (Card & Groupings):** `#121215` with a 1px border of `#27272F`.
- **Layer 2 (Overlays & Menus):** `#1A1A1E` with a 1px border of `#353540` and an ambient perimeter shadow (`0px 12px 32px rgba(0, 0, 0, 0.65)`).
- **Active Focus & Glow:** Critical active elements and key cards utilize a subtle crimson edge bleed (`0px 0px 16px rgba(255, 42, 59, 0.18)`), producing an instrument-like radiance.

## Shapes

The design system deploys a controlled, technical corner geometry:

- **Base Radius (`0.25rem` / 4px):** Tags, numeric chips, input fields, checkboxes, and inline code badges.
- **Medium Radius (`0.5rem` / 8px):** Action buttons, transaction line containers, and card modules.
- **Large Radius (`0.75rem` / 12px):** Modal sheets, primary dashboard widgets, and contextual dialogue flyouts.
- **Circular (`9999px`):** Reserved exclusively for status indicator pips and quick-add icon trigger buttons.

## Components

### Buttons
- **Primary:** Background `#FF2A3B`, text `#FFFFFF`, font `label-lg`, radius `0.5rem`. Hover: `#D91E2D`. Active: `#B31422`. Focused: 2px offset ring `#FF2A3B`.
- **Secondary / Outline:** Background `transparent`, border 1px `#27272F`, text `#FFFFFF`. Hover: border `#FF2A3B`, background `rgba(255, 42, 59, 0.06)`.
- **Ghost:** Text `#A0A0AA`, background `transparent`. Hover: text `#FFFFFF`, background `#1A1A1E`.

### Cards & Metrics
- **Metric Card:** Base surface `#121215`, 1px outline `#27272F`. Padding `space-lg`. Contains label in `label-caps` (`#A0A0AA`), monetary figures in `headline-md` (`#FFFFFF`, tabular numerals), and subtle bottom indicator line (crimson for net spend, mint for surplus).

### Transaction Ledger List
- Row container with hover state transitioning to `#1A1A1E`. Separators use 1px hairlines in `#1C1C22`.
- Left: Category glyph inside a 36x36px `#1A1A1E` square with `0.25rem` radius.
- Center: Transaction note in `body-md` (`#FFFFFF`), subline timestamp and account name in `body-sm` (`#63636E`).
- Right: Amount formatted in `mono-metric` (`#FF2A3B` for debit, `#00E599` for credit).

### Input Fields
- Background `#16161A`, border 1px `#27272F`, text `#FFFFFF`, placeholder `#63636E`.
- Focus state: border `#FF2A3B`, background `#121215`, box-shadow `0px 0px 0px 1px #FF2A3B`. Height `44px` with `0.25rem` radius.
- Quick numeric keypad input displays values in `headline-lg` with fixed currency prefix (`Rp`).

### Badges & Chips
- Offline indicator: Pill badge with `#1A1A1E` fill, `#27272F` border, `11px` uppercase label with a static 6px indicator dot (`#00E599` for local storage active / saved).
- Filter Chips: `#16161A` surface, `#27272F` border. Active state: `#FF2A3B` text and border with `rgba(255, 42, 59, 0.1)` background.

### Selection Controls
- Checkbox: 18x18px square, `0.25rem` radius, 1px `#353540` border. Checked: background `#FF2A3B` with `#FFFFFF` checkmark icon.
- Toggle Switch: Track `#1E1E24`, thumb `#A0A0AA`. Active track `#8E0E19`, thumb `#FF2A3B`.