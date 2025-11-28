package mg.attribute;
import java.lang.Override;

public class MyOperation {
    private String operator;

    public MyOperation(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "Operation{" +
                "operator='" + operator + '\'' +
                '}';
    }
}