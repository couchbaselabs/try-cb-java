package trycb.model;

/**
 * A standardized error format for failing responses, that the frontend application can interpret
 * for all endpoints.
 *
 * @author Simon Baslé
 */
public class Error implements IValue {

    private final String failure;

    public Error(String failure) {
        this.failure = failure;
    }

    public String getFailure() {
        return failure;
    }
}
