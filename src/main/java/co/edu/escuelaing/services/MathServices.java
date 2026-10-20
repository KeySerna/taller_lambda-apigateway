package co.edu.escuelaing.services;

public class MathServices {

    public static Integer square(Integer i) {
        return i * i;
    }

    public static SquareResponse squareJson(SquareRequest request) {
        Integer value = request.getValue();
        return new SquareResponse(value, value * value);
    }
}
