package ninja.cero.lambda;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.DefaultHttpClient;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.RxHttpClient;

import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Function;

public class Bootstrap<IN, OUT> {
    private static final String HEADER_RUNTIME_AWS_REQUEST_ID = "Lambda-Runtime-Aws-Request-Id";
    private static final String NEXT_INVOCATION_URI = "/2018-06-01/runtime/invocation/next";
    private static final String INVOCATION_TEMPLATE = "/2018-06-01/runtime/invocation/%s/response";
    // private static final String ERROR_TEMPLATE = "/2018-06-01/runtime/invocation/$requestId/error";
    // private static final String INIT_ERROR_URI = "/2018-06-01/runtime/init/error";

    public static <IN, OUT> void run(Class<? extends Function<IN, OUT>> clazz) {
        try {
            new Bootstrap().run(clazz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected Bootstrap() {
    }

    protected void run(Function<IN, OUT> function) {
        Class<IN> inType = getArgType(function);

        URL apiEndpoint = lookupRuntimeApiEndpoint();
        final DefaultHttpClientConfiguration config = new DefaultHttpClientConfiguration();
        config.setReadIdleTimeout(null);
        config.setReadTimeout(null);
        RxHttpClient client = new DefaultHttpClient(apiEndpoint, config);

        while (true) {
            runOnce(client, function, inType);
        }
    }

    protected void runOnce(RxHttpClient client, Function<IN, OUT> function, Class<IN> inType) {
        HttpResponse<IN> eventResponse = client.exchange(HttpRequest.GET(NEXT_INVOCATION_URI), inType)
                .blockingFirst();
        IN in = eventResponse.body();
        OUT out = function.apply(in);

        String requestId = eventResponse.header(HEADER_RUNTIME_AWS_REQUEST_ID);
        String invocationUri = String.format(INVOCATION_TEMPLATE, requestId);

        client.exchange(HttpRequest.POST(invocationUri, out), String.class)
                .blockingSubscribe();
    }

    private Class<IN> getArgType(Function<IN, OUT> function) {
        Method[] methods = function.getClass().getDeclaredMethods();
        Class<?> argType = Arrays.stream(methods)
                .filter((m) -> m.getName().equals("apply"))
                .map(Method::getParameterTypes)
                .filter((types) -> types.length == 1)
                .map((types) -> types[0])
                .filter((t) -> t != Object.class)
                .findAny()
                .orElseThrow(() -> new RuntimeException("No function method found"));
        return (Class<IN>) argType;
    }

    protected URL lookupRuntimeApiEndpoint() {
        final String runtimeApiEndpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");
        if (runtimeApiEndpoint == null || runtimeApiEndpoint.isEmpty()) {
            throw new RuntimeException("env value AWS_LAMBDA_RUNTIME_API not found");
        }

        try {
            return new URL("http://" + runtimeApiEndpoint);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }
}
