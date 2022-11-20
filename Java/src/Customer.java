import java.util.ArrayList;
import java.util.List;

public class Customer {
    private List<Integer> initialOrders;
    private List<Integer> orders;
    private int id;
    private int initialId;
    private double position;

    public Customer() {
        initialOrders = new ArrayList<>();
        orders = new ArrayList<>();
        position = 0.0;
        initialId = -1;
        id = -1;
    }

    public Customer(Customer c, int newId) {
        initialOrders = new ArrayList<>(c.initialOrders);
        orders = new ArrayList<>(c.orders);
        position = c.position;
        initialId = c.initialId;
        id = newId;
    }

    public Customer(Customer c) {
        this(c, c.id);
    }

    public Customer(int id, List<Integer> orders) {
        initialOrders = new ArrayList<>(orders);
        this.orders = new ArrayList<>(orders);
        this.initialId = id;
        this.id = id;
        this.position = -(id + 1.0);
    }

    public void reset() {
        orders = new ArrayList<>(initialOrders);
        position = -(id + 1.0);
    }

    public int getId() {
        return id;
    }

    public int getInitialId() {
        return initialId;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public List<Integer> getOrders() {
        return orders;
    }

    public List<Integer> getInitialOrders() {
        return initialOrders;
    }
}
