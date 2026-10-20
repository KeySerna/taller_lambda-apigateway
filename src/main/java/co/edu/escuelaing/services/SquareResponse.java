package co.edu.escuelaing.services;

public class SquareResponse {

    private Integer value;
    private Integer square;

    public SquareResponse() {
    }

    public SquareResponse(Integer value, Integer square) {
        this.value = value;
        this.square = square;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getSquare() {
        return square;
    }

    public void setSquare(Integer square) {
        this.square = square;
    }
}
