import java.util.ArrayList;
import java.util.List;

public class State {
    private List<int[]> waiterSchedule;
    private double waiterPosition;
    private double walkedDistance;
    private double time;
    private List<Customer> customers;

    public State() {
        waiterSchedule = new ArrayList<>();
        waiterPosition = -1.0;
        walkedDistance = 0.0;
        customers = new ArrayList<>();
        time = 0;
    }

    public State(State s) {
        waiterSchedule = new ArrayList<>(s.waiterSchedule);
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
        waiterSchedule.clear();
    }

    public double getTime() {
        return time;
    }

    public void incTime(double time) {
        this.time += time;
    }

    public void incWalkedDistance(double distance) {
        this.walkedDistance += distance;
    }

    public void setWaiterPosition(double position) {
        this.waiterPosition = position;
    }

    public double getWaiterPosition() {
        return waiterPosition;
    }

    public List<int[]> getWaiterSchedule() {
        return waiterSchedule;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void addCustomer(Customer customer) {
        this.customers.add(customer);
    }

    public Customer getCustomer(int id) {
        return customers.get(id);
    }

    public int getNoCustomers() {
        return customers.size();
    }
}
