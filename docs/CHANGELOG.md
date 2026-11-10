# Changelog

What each version of Budgy added.

## What's in V1
- Accounts: cash, bank, e-wallet, credit card, with live balances; transfers don't count as spending
- Fast entry: number pad, category chips, remembers your last account and category
- Categories: 12 expense + 5 income defaults; add, rename, recolor, change icon
- Monthly budgets per category with progress and a "per day" allowance
- Subscriptions: weekly/monthly/yearly/custom cycles, free-trial tracking, monthly and yearly totals,
  "Mark as paid" logs the expense and moves to the next billing date
- Reminders: daily check around 9 AM plus a check whenever the app opens; test button in Settings
- Dashboard: total balance, month income/spent, budget overview, spending donut, next 7 days, recent
- Light mode by default with a dark mode toggle (Home header and Settings); currency symbol picker

## What's new in V2
- Recurring transactions (auto-log or "ask me first"), plus a Repeat option when adding
- Savings goals with a weekly amount to hit the target date
- Split bills and IOUs (who owes who), without inflating your spending or income
- Activity calendar view, search across all months, account and category filters
- Monthly report: month-over-month change, spending by day, top categories, quick facts
- Subscriptions: shared plans (your share only), "last used" with cancel hints, price history
- Home screen widget: what you can still spend today + Add button
- Back up / restore to a JSON file, export transactions to CSV
- App lock with fingerprint, face or screen lock
- Upgrading from V1 keeps all your data (database migration 1 → 2)

## What's new in V3
- CSV import (Budgy exports and most bank/e-wallet statements), alongside CSV export
- Debt payoff planner: snowball vs. avalanche, debt-free date, total interest, record payments
- Net worth over the last 12 months, with assets and liabilities breakdown
- No-spend day streaks on Home (current, best, last 7 days)
- Optional envelope budgeting: unspent budget rolls over, move money between envelopes
- Upgrading from V1 or V2 keeps all your data (migrations 1 → 2 → 3)

## What's new in V4
- Long-press the app icon: Add expense, Add income, Transfer
- Budget periods that start on payday (any day 1–28) or twice a month (15th and 30th)
- Notifications at 80% and 100% of a budget, spending alerts, and a Sunday weekly summary
- Tags for trips and events (#Baguio trip) with totals per tag
- Balance adjustment (fix a balance without fake income/expenses) and archived accounts
- Multi-currency accounts with your own exchange rates; totals stay in your main currency
- Spending alerts on Home ("Food is 40% higher than usual")
- Year in review and badges
- Filipino and Cebuano languages (Settings → Language)
- In-app guide: Settings → How to use Budgy (Home also shows a "New to Budgy?" card until it's opened or dismissed)
- Upgrading from any earlier version keeps all your data (migrations 1 → 2 → 3 → 4)

## What's new in V5
- Investments (Settings → Investments, or the card on Home): crypto and Pag-IBIG MP2
- Crypto prices live from CoinGecko's free public API, or a manual price for any coin it doesn't list
- Buy, sell and earnings (staking rewards, MP2 dividends); money moves in or out of your accounts
  without counting as spending or income (new "Investment buy" / "Investment sell" transactions)
- Gain/loss with average cost, realized gains from sales, MP2 estimate at maturity
- Investments count in Net worth, including past months (uses saved prices)
- Adds the INTERNET permission, used ONLY for CoinGecko prices. Nothing goes online until the user reads
  the notice in Investments and picks live or manual prices; Settings → Internet explains it and turns it off
- In-app guide has a new Investments section
- Upgrading keeps all your data (migration 4 → 5); backups now include investments (format v5)

## What's new in V6
- Fund / ETF investments (e.g. S&P 500 ETFs like VOO, index funds, UITFs) with a price you type
- Prices can be in US dollars (or another currency) with your own exchange rate; amounts convert to your main currency
- No new internet use: funds are fully offline
- Upgrading keeps all your data (migration 5 → 6); backups format v6
