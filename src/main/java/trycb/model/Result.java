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
public class Result {

    private final Object data;
    private final String[] context;

    public Result(Object data, String... contexts) {
        this.data = data;
        this.context = contexts;
    }
}
