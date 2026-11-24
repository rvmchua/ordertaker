package com.rvmc.ordertaker.entity.enums;

public enum Role {
    CUSTOMER,
    RESTAURANT,
    ADMIN;

    public String getPrefixedName() {
        return "ROLE_" + name();
    }
}
