package com.rvmc.ordertaker.entity.enums;

public enum Role {
    CUSTOMER,
    RESTAURANT,
    RESTAURANT_OWNER,
    ADMIN;

    public String getPrefixedName() {
        return "ROLE_" + name();
    }
}
