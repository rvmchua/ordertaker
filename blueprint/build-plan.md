# Build Plan

List the features that make up your project, high level and in rough build order.
Keep each item to one line; the details come later in `/feature`.

Plain bullets are fine. When both planning docs are ready, run `/overview`.
It adds tracking numbers and checkboxes to your feature list before generating
the project overview.

Run `/feature` to spec the next unchecked item, or `/feature 2` to pick one.
Keep completed items checked and append new features as the project grows.
Do not renumber completed features; their archived specs refer to those IDs.

Scaffolding the app and prototyping its look are pre-build steps, not features.
Start with your first real slice of functionality.

## Your features

- [x] 1. **Authentication and role-based access** - customer and restaurant sign-up, login, JWT issuance, and protected endpoint authorization.
- [ ] 2. **Restaurant and menu management** - restaurant profiles, menu item CRUD, availability toggles, and ownership boundaries.
- [ ] 3. **Customer cart and checkout flow** - create a single active cart, add items from one restaurant, validate totals, and convert cart to an order.
- [ ] 4. **Order lifecycle and fulfillment rules** - enforce valid status transitions, cancellation rules, and role-based action permissions.
- [ ] 5. **Order history and tracking APIs** - expose customer order history, restaurant incoming orders, and retrieval of past order details.
- [ ] 6. **Admin oversight and API hardening** - user and restaurant moderation, validation coverage, global error handling, and documentation polish.
