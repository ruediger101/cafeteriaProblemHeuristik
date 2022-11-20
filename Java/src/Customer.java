import java.util.ArrayList;
import java.util.List;

public class Customer{
    protected List<Integer> initialOrders;
    protected List<Integer> orders;
    protected int nr;
    protected double position;

    public Customer(){
        initialOrders = new ArrayList<>();
        orders = new ArrayList<>();
        position = 0.0;
        nr = -1;
    }

    public Customer(Customer c){
        initialOrders = new ArrayList<>(c.initialOrders);
        orders = new ArrayList<>(c.orders);
        position = c.position;
        nr = c.nr;
    }

    public Customer(int nr, double position, List<Integer> orders){
        initialOrders = new ArrayList<>(orders);
        this.orders = new ArrayList<>(orders);
        this.nr = nr;
        this.position = position;
    }

    public void reset(){
        orders = new ArrayList<>(initialOrders);
        position = -nr;
    }
}
