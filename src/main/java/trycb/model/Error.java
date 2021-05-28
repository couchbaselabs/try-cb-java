package trycb.model;

/**
 * A standardized error format for failing responses, that the frontend
 * application can interpret for all endpoints.
 */
public class Error implements IValue {

    private final String message;

    public Error(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
