package pt.matriz;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(basePackages = {"pt.matriz.auth", "pt.matriz.projects", "pt.matriz.tasks", "pt.matriz.microsoft"})
public class ApiProblems extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<Object> {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static ProblemDetail problem(int status, String detail) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
    }

    public static ResponseEntity<ProblemDetail> response(int status) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem(status, HttpStatus.valueOf(status).getReasonPhrase()));
    }

    public static ResponseEntity<ProblemDetail> validation(String field, String message) {
        var problem = problem(422, "There are validation errors.");
        problem.setTitle("Unprocessable Content");
        problem.setProperty("errors", List.of(Map.of("field", field, "message", message)));
        return ResponseEntity.unprocessableEntity().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    public static void write(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        // Explicit properties also work outside MVC's ProblemDetail Jackson mix-in.
        var title = HttpStatus.valueOf(status).getReasonPhrase();
        JSON.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", title,
            "status", status, "detail", title));
    }

    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) { return true; }

    @Override public Object beforeBodyWrite(Object body, MethodParameter method, MediaType type,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
        if (status < 400) return body;
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (body instanceof ProblemDetail problem) {
            if (problem.getDetail() == null) problem.setDetail(HttpStatus.valueOf(status).getReasonPhrase());
            return problem;
        }
        String detail = body instanceof Map<?, ?> map && map.get("error") instanceof String message
            ? message : HttpStatus.valueOf(status).getReasonPhrase();
        return problem(status, detail);
    }
}
