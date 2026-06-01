package com.praio.backend.exception;

public class BeachNotFoundException extends RuntimeException {

    public BeachNotFoundException(String id) {
        super("Praia não encontrada com o ID: " + id);
    }
}
