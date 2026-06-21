package com.typeahead.core.dataset;

/**
 * Thrown when the dataset CSV cannot be loaded at startup.
 *
 * <p>Per DESIGN.md §13.4: "Dataset loading failure at startup → fail fast,
 * app should not start in a half-loaded state."
 *
 * <p>This is an unchecked exception so Spring's {@code ApplicationRunner} propagates
 * it as a startup failure, preventing the server from accepting traffic.
 */
public class DatasetLoadException extends RuntimeException {

    public DatasetLoadException(String message) {
        super(message);
    }

    public DatasetLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
