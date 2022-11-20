import java.util.ArrayList;
import java.util.List;

public class State {
    protected List<int[]> servicedOrder;
    protected double waiterPosition;
    protected double walkedDistance;
    protected double time;
    protected List<Customer> customers;

    public State() {
        servicedOrder = new ArrayList<>();
        waiterPosition = 0.0;
        walkedDistance = 0.0;
        customers = new ArrayList<>();
        time = 0;
    }

    public State(State s) {
        servicedOrder = new ArrayList<>(s.servicedOrder);
        waiterPosition = s.waiterPosition;
        walkedDistance = s.walkedDistance;
        customers = new ArrayList<>(s.customers.stream().map(Customer::new).toList());
        time = s.time;
    }

    public static int compare(State a, State b) {
        int result = Double.compare(a.time, b.time);
        if (result == 0) {
            result = Double.compare(a.walkedDistance, b.walkedDistance);
        }
        return result;
    }

    public void reset() {
        customers.stream().forEach(Customer::reset);
        waiterPosition = 0.0;
        walkedDistance = 0.0;
        time = 0.0;
        servicedOrder.clear();
    }
}
