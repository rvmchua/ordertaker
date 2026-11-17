package com.rvmc.ordertaker.exception;

public class ResourceAlreadyTaggedAsDeletedException extends RuntimeException {
    public ResourceAlreadyTaggedAsDeletedException(Long id, String resource) {
        super(resource + " with id " + id + " is already deleted.");
    }
}
