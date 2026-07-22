package app.global.responses;

public class ErrorResponse {
    private final String error;
    private final String details;
    private final long timestamp;
    private final int statusCode;


    public ErrorResponse(String error, String details, int statusCode) {
        this.error = error;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
        this.statusCode = statusCode;
    }


    public int getStatusCode() {
        return statusCode;
    }
    public String getError() {
        return error;
    }
    public String getDetails() {
        return details;
    }
    public long getTimestamp() {
        return timestamp;
    }
}