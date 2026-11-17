package com.rvmc.ordertaker.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(String name) {
        super(name + " not found.");
    }
}
