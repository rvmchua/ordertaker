# OrderTaker API Contract

The application uses `/api/v1` as its REST API prefix. The live controller,
security configuration, and DTO classes are authoritative. This document
describes routes that exist in the current codebase.

## Authentication

All JWT-protected routes require:

```http
Authorization: Bearer <access-token>
```

### Register a customer

`POST /api/v1/auth/register/customer`

```json
{
  "username": "royce",
  "email": "royce@example.com",
  "password": "minimum8",
  "customerName": "Royce Chua",
  "address1": "Street",
  "address2": "City"
}
```

### Register a restaurant owner

`POST /api/v1/auth/register/restaurant-owner`

```json
{
  "username": "owner",
  "email": "owner@example.com",
  "password": "minimum8",
  "restaurantName": "Example Kitchen",
  "address1": "Street",
  "address2": "City"
}
```

### Login

`POST /api/v1/auth/login`

```json
{
  "email": "royce@example.com",
  "password": "minimum8"
}
```

Both registration and login return:

```json
{
  "accessToken": "...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "royce@example.com",
  "role": "CUSTOMER"
}
```

## Public Routes

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/restaurants` | List restaurants |
| `GET` | `/api/v1/restaurants?restaurant=<name>` | Case-insensitive name search |
| `GET` | `/api/v1/restaurants/{restaurantId}` | Restaurant profile |
| `GET` | `/api/v1/restaurants/{restaurantId}/menu` | Active, non-deleted menu |
| `GET` | `/api/v1/restaurants/menu/{menuItemId}` | Active menu item |
| `GET` | `/actuator/health` | Deployment health |

## Customer Routes

`ROLE_CUSTOMER` is required. IDs come from the authenticated principal; request
IDs are not the source of customer ownership.

| Method | Route | Body | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/customers/profile` | — | Own customer profile |
| `PUT` | `/api/v1/customers/profile` | `CustomerProfileRequest` | Update mutable profile fields |
| `GET` | `/api/v1/customers/cart` | — | Own active cart |
| `POST` | `/api/v1/customers/cart` | `CreateCartRequest` | Create active cart |
| `PUT` | `/api/v1/customers/cart` | `UpdateCartRequest` | Replace or clear cart items |
| `GET` | `/api/v1/customers/order` | — | Own order history |
| `GET` | `/api/v1/customers/order/{orderId}` | — | Own order detail |
| `POST` | `/api/v1/customers/order` | — | Checkout active cart |
| `PATCH` | `/api/v1/customers/order/{orderId}` | `{"status":"CANCELLED"}` | Cancel eligible order |

Profile updates are partial: omitted or `null` fields remain unchanged. A
provided email must be valid and unique across users; accepted emails are
trimmed and normalized to lowercase.

Cart creation shape:

```json
{
  "customerId": 1,
  "restaurantId": 1,
  "cartItems": [
    {"menuItemId": 10, "quantity": 2}
  ]
}
```

The controller replaces the customer ID with the authenticated customer ID.
Cart update uses the same item shape and may use an empty `cartItems` list to
clear the cart.

## Restaurant Owner Routes

`ROLE_RESTAURANT` is required.

| Method | Route | Body | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/restaurants/profile` | — | Own restaurant profile |
| `PUT` | `/api/v1/restaurants/profile` | `RestaurantProfileRequest` | Update mutable profile fields |
| `DELETE` | `/api/v1/restaurants/profile` | — | Soft-delete own restaurant |
| `POST` | `/api/v1/restaurants/menu` | `MenuItemRequest` | Create menu item |
| `PUT` | `/api/v1/restaurants/menu/{menuItemId}` | `MenuItemRequest` | Update owned item |
| `PATCH` | `/api/v1/restaurants/menu/{menuItemId}` | — | Toggle availability |
| `DELETE` | `/api/v1/restaurants/menu/{menuItemId}` | — | Soft-delete owned item |
| `GET` | `/api/v1/restaurants/order/{restaurantId}` | — | List owned restaurant orders |
| `PATCH` | `/api/v1/restaurants/order/{orderId}?restaurantId=<id>` | `{"status":"CONFIRMED"}` | Apply allowed transition |

The service verifies menu items and orders against the authenticated
restaurant. A restaurant ID supplied on a route or query parameter is not
used to bypass that ownership check.

Restaurant profile updates are partial: omitted or `null` fields remain
unchanged. A supplied email must be valid and unique across users. Accepted
emails are trimmed and normalized to lowercase.

```json
{
  "username": "owner",
  "email": "owner@example.com",
  "restaurantName": "Example Kitchen",
  "address1": "Street",
  "address2": "City"
}
```

`RESTAURANT` owner accounts can soft-delete their own restaurant profile.
The operation marks the profile inactive and excluded from public listings.

## Admin Routes

`ROLE_ADMIN` is required.

| Method | Route | Body | Purpose |
|---|---|---|---|
| `DELETE` | `/api/v1/admin/restaurants/{restaurantId}` | — | Permanently delete a restaurant |
| `PATCH` | `/api/v1/admin/restaurants/{restaurantId}/reactivate` | — | Reverse soft deletion |

Backend administration hard-deletes the restaurant and its owner account.
Only the admin route permanently removes the restaurant record.
Reactivation restores `isActive=true` and `toDelete=false`, making the restaurant
eligible for public listings again.

## HTTP Error Responses

| Status | Meaning |
|---|---|
| `400` | Invalid argument, malformed JSON, missing parameter, or validation failure |
| `401` | Missing or invalid bearer token |
| `403` | Authenticated user lacks the required role or ownership |
| `404` | Resource or owned resource not found |
| `409` | Duplicate email, already-deleted resource, or invalid order transition |
| `500` | Unexpected server failure |

Typical validation and resource responses use JSON:

```json
{"error":"Customer", "reason":"not found"}
```

Validation failures use:

```json
{
  "error": "Validation failed",
  "fields": {
    "email": "must be a well-formed email address"
  }
}
