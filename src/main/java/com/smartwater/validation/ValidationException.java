package com.smartwater.validation;

/**
 * Custom exception for data validation errors.
 * Wraps validation failures with descriptive error messages.
 */
public class ValidationException extends Exception {
    private final String fieldName;
    private final Object value;

    /**
     * Constructs a ValidationException with error message and field details.
     *
     * @param message   the error message
     * @param fieldName the name of the field that failed validation
     * @param value     the invalid value
     */
    public ValidationException(String message, String fieldName, Object value) {
        super(message);
        this.fieldName = fieldName;
        this.value = value;
    }

    /**
     * Constructs a ValidationException with just an error message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.value = null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (fieldName != null) {
            return String.format("ValidationError [%s]: %s (value: %s)", fieldName, getMessage(), value);
        }
        return "ValidationError: " + getMessage();
    }
}
