package trycb.model;

import lombok.Data;

/**
 * A standardized result format for successful responses, that the frontend application can interpret
 * for all endpoints. Allows to contain user-facing data and an array of context strings, eg. N1QL
 * queries, to be displayed in a "learn more" or console kind of UI element on the front end.
 *
 * @author Simon Baslé
 */
@Data
public class Result<T> implements IValue {

    private final T data;
    private final String[] context;

    private Result(T data, String... contexts) {
        this.data = data;
        this.context = contexts;
    }

    public static <T> Result<T> of(T data, String... contexts) {
        return new Result<T>(data, contexts);
    }
}
