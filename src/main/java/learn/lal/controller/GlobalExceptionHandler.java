package learn.lal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex) {
        String message = ex.getMessage();
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();

        Map<String, String> response = new HashMap<>();
        response.put("trace", stackTrace);

        if (message != null && (message.contains("401") || message.contains("Unauthorized"))) {
            response.put("error", "API Key is invalid or expired. Please check your AI API keys.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        if (message != null && message.contains("429")) {
            response.put("error", "API Rate Limit Exceeded or Out of Quota.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
        if (message != null && (message.contains("500") || message.contains("Internal Server Error"))) {
            response.put("error", "AI Provider failed to respond (Internal Error). Check API Key or quota.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        response.put("error", "AI Provider Error: " + message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
