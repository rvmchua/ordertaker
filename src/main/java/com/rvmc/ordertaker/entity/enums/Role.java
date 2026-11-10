package com.rvmc.ordertaker.entity.enums;

public enum Role {
    CUSTOMER,
    RESTAURANT;

    public String getPrefixedName() {
        return "ROLE_" + name();
    }
}
