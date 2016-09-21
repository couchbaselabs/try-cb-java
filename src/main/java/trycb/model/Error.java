package trycb.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * A standardized error format for failing responses, that the frontend application can interpret
 * for all endpoints.
 *
 * @author Simon Baslé
 */
@Data
@RequiredArgsConstructor
public class Error implements IValue {

    private final String failure;

}
