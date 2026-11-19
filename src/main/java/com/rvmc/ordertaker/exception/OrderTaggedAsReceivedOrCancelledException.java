package com.rvmc.ordertaker.exception;

public class OrderTaggedAsReceivedOrCancelledException extends RuntimeException {
    public OrderTaggedAsReceivedOrCancelledException(String message) {
        super(message);
    }
}
