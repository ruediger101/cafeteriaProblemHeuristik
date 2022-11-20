import java.util.ArrayList;
import java.util.List;

public class Customer {
    protected List<Integer> initialOrders;
    protected List<Integer> orders;
    protected int no;
    protected int initialNo;
    protected double position;

    public Customer() {
        initialOrders = new ArrayList<>();
        orders = new ArrayList<>();
        position = 0.0;
        initialNo = -1;
        no = -1;
    }

    public Customer(Customer c, int newNo) {
        initialOrders = new ArrayList<>(c.initialOrders);
        orders = new ArrayList<>(c.orders);
        position = c.position;
        initialNo = c.initialNo;
        no = newNo;
    }

    public Customer(Customer c) {
        this(c, c.no);
    }

    public Customer(int no, double position, List<Integer> orders) {
        initialOrders = new ArrayList<>(orders);
        this.orders = new ArrayList<>(orders);
        this.initialNo = no;
        this.no = no;
        this.position = position;
    }

    public void reset() {
        orders = new ArrayList<>(initialOrders);
        position = -no;
    }
}
