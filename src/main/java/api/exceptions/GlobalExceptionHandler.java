package api.exceptions;

import api.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global (Project Wide) exception handler for REST controllers.
 *
 * <p>Converts exceptions to standardized JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles IllegalArgumentException thrown when input validation fails.
     * @param ex the exception
     * @return a {@code ResponseEntity} with HTTP status 400 (Bad Request) and a specific
     * error message indicating the invalid parameter type
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("Validation failed", ex.getMessage(), 400);
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles malformed JSON bodies in requests.
     * @param ex the exception
     * @return a {@code ResponseEntity} with HTTP status 400 (Bad Request) and a specific
     * error message indicating the invalid parameter type
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseError(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse("Processing Error", "Invalid JSON format in request body", 400);
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles Validation errors in @Valid (@NotNull, @Positive).
     * @param ex the exception
     * @return a {@code ResponseEntity} with HTTP status 400 (Bad Request) and a specific
     * error message indicating the invalid parameter type
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailures(MethodArgumentNotValidException ex) {
        String allErrors = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Validation failed");

        ErrorResponse error = new ErrorResponse("Validation failed", allErrors, 400);
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handles requests to undefined endpoints (404).
     * Requires spring.mvc.throw-exception-if-no-handler-found=true and
     * spring.web.resources.add-mappings=false in application.properties.
     * @param ex the exception containing the request URL and method
     * @return a {@code ResponseEntity} with HTTP status 404 (Not found) and a specific
     * error message indicating the endpoint was not found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleEndpointNotFound(NoHandlerFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                "Endpoint not found",
                "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
                404
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles HTTP method not supported (405).
     * @param ex the exception
     * @return a {@code ResponseEntity} with HTTP status 405 (Method Not Allowed) and a
     * error message indicating that the HTTP method is not supported with the endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse(
                "Method not allowed",
                ex.getMethod() + " is not supported for this endpoint. Supported methods: " + ex.getSupportedHttpMethods(),
                405
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
    *  Handler for errors when executing a data-related operation, SQL statement, database connection
    *  Generic message in details over internal database exception for safety
    * @param ex the exception
    @return a {@code ResponseEntity} with HTTP status 500 (Internal Server Error) and a
    * error message about the unexpected error.
    */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessError(DataAccessException ex) {
        logger.error("Data Access error occurred", ex);
        ErrorResponse error = new ErrorResponse("DataAccess error",
                "An unexpected error occurred while trying to access the database", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     *  Handler for errors when request can't be processed as it violates with the current state of the targeted resource
     *  Generic message in details over internal database exception for safety
     * @param ex the exception
     * @return 409 Conflict with server state error
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Data integrity violation", ex);
        ErrorResponse error = new ErrorResponse("Data integrity violation",
                "The operation would violate a database constraint", 409);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     *  Handler for when book is not found
     * @param ex the exception
     * @return 404 not found
     */
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookNotFound(BookNotFoundException ex) {
        logger.warn("Book not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("Book not found", ex.getMessage(), 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex) {
        logger.error("Property reference error occurred", ex);
        ErrorResponse error = new ErrorResponse("Property reference error", ex.getMessage(), 400);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    /**
     * Handles cases where the provided data type does not match the expected parameter type.
     *
     * @param ex the exception thrown due to the type mismatch
     * @return a {@code ResponseEntity} with HTTP status 400 (Bad Request) and a specific
     *         error message indicating the invalid parameter type
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        ErrorResponse error = new ErrorResponse("Invalid parameter", message, 400);
        return ResponseEntity.badRequest().body(error);
    }


    /**
     * Fallback handler for any other unhandled exception.
     * @param ex the exception
     * @return 500 Internal Server Error with generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleEverythingElse(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        ErrorResponse error = new ErrorResponse("Internal server error", ex.getMessage(), 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }




}