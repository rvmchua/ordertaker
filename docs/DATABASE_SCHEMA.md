# Database Schema

OrderTaker uses PostgreSQL for deployment and H2 for repository/context tests.
Flyway applies migrations in filename order at startup.

## Migration History

| Migration | Purpose |
|---|---|
| `V1__create_initial_schema.sql` | Creates users, profiles, menu, cart, and order tables |
| `V2__add_active_cart_state.sql` | Adds `carts.is_active` and renames `cart_items.menu_id` |
| `V3__add_unique_user_email.sql` | Enforces unique login emails |
| `V4__store_order_status_as_name.sql` | Converts order status storage from integer to string |

## Tables

### users

Shared account identity.

- `user_id` identity primary key
- `username`, `email`, unique `email`, `password_hash`, `role`
- `created_at`

### customers

- `customer_id` identity primary key
- `customer_name`, `address1`, `address2`, `is_active`
- Unique `user_id` foreign key to `users`

### restaurants

- `restaurant_id` identity primary key
- `restaurant_name`, `address1`, `address2`, `is_active`, `to_delete`
- Unique `owner_id` foreign key to `users`
- `updated_at`

### menu_items

- `menu_item_id` identity primary key
- `restaurant_id` foreign key
- `item_name`, `description`, `price`
- `is_available`, `is_deleted`

### carts

- `cart_id` identity primary key
- Unique `customer_id` foreign key
- `restaurant_id` foreign key
- `total_amount`, `created_at`, `is_active`

### cart_items

- `cart_item_id` identity primary key
- `cart_id` foreign key
- `menu_item_id` foreign key
- `quantity`

### orders

- `order_id` identity primary key
- `customer_id` and `restaurant_id` foreign keys
- `status` stored as a string enum
- `total_amount`, `created_at`, optimistic `version`

### order_items

- `order_item_id` identity primary key
- `order_id` and `menu_item_id` foreign keys
- `quantity`
- `price_at_order_time`, preserving the menu price at checkout

## Important Relationships

```text
User 1 ---- 0..1 Customer
User 1 ---- 0..1 Restaurant
Restaurant 1 ---- 0..* MenuItem
Customer 1 ---- 0..1 active Cart
Cart 1 ---- 0..* CartItem ---- 1 MenuItem
Customer 1 ---- 0..* Order
Restaurant 1 ---- 0..* Order
Order 1 ---- 1..* OrderItem ---- 1 MenuItem
```

## Design Decisions

- `priceAtOrderTime` is stored on order items so later menu changes do not alter
  historical invoices.
- Cart items hold live references plus quantities; checkout creates snapshots.
- The customer has at most one active cart because the cart repository enforces
  one cart per customer and the service prevents a second active cart.
- `toDelete` and `isActive` implement restaurant owner soft deletion. These
  flags exclude a soft-deleted restaurant from public listings.
- Backend admin hard deletion removes the restaurant and its owner account
  from the database.
- Backend admin reactivation reverses the soft-deletion flags by setting
  `toDelete=false` and `isActive=true`.
- `isDeleted` implements soft deletion for menu items, allowing historical order
  references to remain valid.
- `@Version` on orders enables optimistic concurrency and a conflict response
  when concurrent updates are detected.
