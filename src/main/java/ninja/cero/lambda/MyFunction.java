package ninja.cero.lambda;

import java.util.function.Function;

public class MyFunction implements Function<MyRequest, MyResponse> {
    public static void main(String[] args) {
        Bootstrap.run(MyFunction.class);
    }

    @Override
    public MyResponse apply(MyRequest myRequest) {
        MyResponse response = new MyResponse();
        response.setOutput("Hello, " + myRequest.getInput());
        return response;
    }
}
