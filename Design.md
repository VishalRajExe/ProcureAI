# Design.md — ProcureAI

## Visual Design System

---

## 1. Design Direction

ProcureAI should look and feel like a **serious modern B2B SaaS / AI-agent product** — not a hackathon CRUD app, not a 2018 admin template, not a generic chatbot UI.

**Inspiration:** Linear, Stripe, Vercel, modern procurement SaaS tools, modern AI-agent dashboards.

**Principles:**
- Functionality first — polish supports clarity, never replaces it
- Confident whitespace, strong visual hierarchy
- Calm, professional, trustworthy — this product handles money and vendor negotiations
- Subtle motion only; nothing gimmicky or distracting

---

## 2. Color Palette

### Base (neutral foundation)
| Token | Hex | Usage |
|---|---|---|
| `bg-primary` | `#0B0D12` (dark) / `#FAFAFA` (light) | App background |
| `bg-surface` | `#12151C` / `#FFFFFF` | Cards, panels |
| `bg-surface-hover` | `#1A1E27` / `#F4F5F7` | Hover states |
| `border-default` | `#242832` / `#E5E7EB` | Card borders, dividers |
| `text-primary` | `#F5F6F8` / `#0F1115` | Headings, primary text |
| `text-secondary` | `#9AA1AE` / `#5B6270` | Supporting text |
| `text-muted` | `#6B7280` / `#9CA3AF` | Placeholders, disabled |

Default to a **dark-first** theme (fits the "AI agent" identity) with a light theme available via a toggle in Settings.

### Brand / Accent
| Token | Hex | Usage |
|---|---|---|
| `accent-primary` | `#4F7CFF` (indigo-blue) | Primary actions, links, active states, AI-related highlights |
| `accent-primary-hover` | `#3E68E6` | Hover on primary buttons |
| `accent-soft` | `#EEF2FF` / `rgba(79,124,255,0.12)` | Badge backgrounds, subtle highlights |

### Semantic Colors
| Token | Hex | Meaning |
|---|---|---|
| `success` | `#22C55E` | Within benchmark, approved, completed |
| `warning` | `#F59E0B` | Above benchmark, pending approval, needs review |
| `danger` | `#EF4444` | Failed, rejected, above max price |
| `info` | `#38BDF8` | AI insights, informational badges |
| `best-price` | `#A855F7` (purple, used with 🔥) | "Best price" highlight, distinct from generic success |
| `recommended` | `#FACC15` (gold, used with 🏆) | "Recommended vendor" highlight |

Use semantic colors consistently: green = good/within limits, amber = caution/above benchmark or pending, red = failed/rejected/over limit, blue/indigo = AI or primary action, purple = best price, gold = final recommendation.

---

## 3. Typography

- **Primary font:** Inter (UI text, body, tables) — clean, neutral, excellent at small sizes
- **Numeric/data font:** Inter with `font-variant-numeric: tabular-nums` for all price/number columns so figures align in tables
- **Optional display font:** A slightly tighter grotesk (e.g., Inter Tight or General Sans) for large dashboard KPI numbers, if desired — otherwise Inter Bold is sufficient

### Scale
| Style | Size / Weight | Usage |
|---|---|---|
| Display | 32–40px / 700 | Dashboard KPI values, hero numbers |
| H1 | 28px / 700 | Page titles |
| H2 | 20px / 600 | Section headers |
| H3 | 16px / 600 | Card titles |
| Body | 14px / 400 | Default text |
| Small | 13px / 400 | Table cells, secondary metadata |
| Caption | 12px / 500, uppercase, letter-spacing 0.04em | Badges, labels, table headers |

Line height: 1.5 for body text, 1.2 for headings.

---

## 4. Layout & Spacing

- 8px base spacing unit (8 / 16 / 24 / 32 / 48px rhythm)
- Max content width: ~1280px on wide screens, centered with generous side padding
- Sidebar navigation (persistent) + top bar (breadcrumb/page title + user menu) for the authenticated app shell
- Cards: 12px border radius, 1px border (`border-default`), subtle shadow only on hover/elevated elements — avoid heavy drop shadows
- Tables: zebra-free, rely on row hover highlight (`bg-surface-hover`) and clear column dividers via whitespace, not borders

---

## 5. Components

### Buttons
- Primary: solid `accent-primary`, white text, 8px radius
- Secondary: outline with `border-default`, `text-primary`
- Destructive: outline or solid `danger`, reserved for Reject actions
- Disabled: reduced opacity (0.5), no hover state

### Badges / Status Pills
Small, rounded-full, colored background at 12% opacity + full-opacity text/icon of the semantic color:
- `✓ Within Benchmark` — success
- `⚠ Above Benchmark` — warning
- `🔥 Best Price` — best-price (purple)
- `🏆 Recommended` — recommended (gold)
- `WAITING_APPROVAL`, `PROCESSING`, `COMPLETED`, `NEEDS_REVIEW`, `FAILED` — mapped to info/warning/success/warning/danger respectively

### Cards
- Dashboard KPI card: label (caption style) → large display number → small trend/context line
- Vendor comparison card/row: vendor name + logo initial avatar, key metrics, status badges, score

### AI Insights Panel
- Visually distinct from regular cards: subtle `accent-soft` background or left border accent in `accent-primary`, small robot/spark icon, concise bullet-point summary — never a wall of text

### Tables
- Sticky header row
- Right-aligned numeric columns with tabular numerals
- Sortable column headers with subtle sort-direction indicator
- Row-level status badge in a dedicated column, not buried in text

### Timeline (Negotiation Timeline)
- Vertical timeline with timestamp, icon, and short event description per node
- Connector line in `border-default`, active/current node highlighted in `accent-primary`

### Empty / Loading States
- Empty states: simple icon + one-line explanation + primary CTA (e.g., "No quotes yet — Upload your first vendor quote")
- Loading: skeleton placeholders matching final content shape (not spinners alone) for tables/cards; a small spinner is acceptable for button-level async actions
- Toasts: top-right, auto-dismiss, color-coded by semantic type, used for save/send/error confirmations
- Confirmation dialogs: required before Approve & Send, Reject, and Generate PO actions

---

## 6. Icons & Motion

- Icon set: a single consistent line-icon set (e.g., Lucide) throughout — no mixing icon styles
- Emoji is used sparingly and intentionally only in the specific badges/labels defined in `PRD.md` (✓ ⚠ 🔥 🏆 🤖 💰 ⏱), not decoratively elsewhere
- Motion (Framer Motion): subtle fade/slide-in on page and card mount (150–250ms), gentle hover scale (1.01–1.02) on interactive cards, no bouncy/playful easing — use `ease-out`
- Respect `prefers-reduced-motion`

---

## 7. Data Visualization (Recharts)

- Use `accent-primary` for primary series, semantic colors for status-based series (e.g., savings in green, delivery time in blue)
- Consistent axis typography (caption style), gridlines in `border-default` at low opacity
- Tooltips styled to match card design (surface background, border, rounded corners)

---

## 8. Accessibility

- Minimum 4.5:1 contrast for body text against backgrounds in both themes
- All status information conveyed by color must also include an icon or text label (never color-only)
- Full keyboard navigability for approval actions (Edit / Approve & Send / Reject)
- Visible focus states on all interactive elements
