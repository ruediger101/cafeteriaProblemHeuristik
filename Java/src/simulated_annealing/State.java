package simulated_annealing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

public class State {
    private List<int[]> waiterSchedule;
    private double waiterPosition;
    private double walkedDistance;
    private double totalTime;
    private List<Customer> customers;
    private double waiterVelocity = 1.0;
    private double servingTime = 2.0;

    public State() {
        waiterSchedule = new ArrayList<>();
        waiterPosition = -1.0;
        walkedDistance = 0.0;
        customers = new ArrayList<>();
        totalTime = 0;
    }

    public State(State s) {
        waiterSchedule = new ArrayList<>(s.waiterSchedule);
        waiterPosition = s.waiterPosition;
        walkedDistance = s.walkedDistance;
        customers = new ArrayList<>(s.customers.stream().map(Customer::new).toList());
        totalTime = s.totalTime;
    }

    private static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++)
            result = result.multiply(BigInteger.valueOf(i));
        return result;
    }

    public void printStats() {
        System.out.println("No Customers: " + customers.size());
        System.out.println("No of possible customer sequences: " + factorial(customers.size()).toString());
        System.out.println("Last served counter: " + customers.stream().map(Customer::getOrders).flatMap(List::stream).mapToInt(c -> c + 1).max().orElse(0));
        System.out.println("Min no Request: " + customers.stream().mapToInt(c -> c.getOrders().size()).min().orElse(-1));
        System.out.println("Max no Request: " + customers.stream().mapToInt(c -> c.getOrders().size()).max().orElse(-1));
        System.out.println("Unique Orders: " + customers.stream().allMatch(c -> c.getOrders().size() == c.getOrders().stream().distinct().toList().size()));
        System.out.println();
    }

    // function to move all customers for a given order to process
    public void serveCustomer(int indexNextCustomer) {
        Customer nextCustomer = getCustomers().get(indexNextCustomer);
        int servedOrderAt = nextCustomer.getOrders().remove(0);

        waiterSchedule.add(new int[] { nextCustomer.getId(), servedOrderAt });

        double deltaDistance = Math.abs(servedOrderAt - getWaiterPosition());
        this.walkedDistance += deltaDistance;

        double deltaTime = Math.max(servedOrderAt - nextCustomer.getPosition() / nextCustomer.getVelocity(), deltaDistance / waiterVelocity) + servingTime; // time required to serve customer

        this.totalTime += deltaTime;
        setWaiterPosition(servedOrderAt);
        nextCustomer.setPosition(servedOrderAt);

        // update customer position of each of the n customers
        double previousPosition = Double.MAX_VALUE;
        for (Iterator<Customer> it = (new ArrayList<>(getCustomers())).iterator(); it.hasNext();) {
            Customer c = it.next();
            if (!nextCustomer.equals(c)) {
                c.setPosition(Math.min(c.getPosition() + deltaTime * c.getVelocity(), previousPosition - 1));// update position ignoring next order

                if (!c.getOrders().isEmpty()) { // take order into account if one exists
                    c.setPosition(Math.min(c.getOrders().get(0), c.getPosition()));
                }
            }
            previousPosition = c.getPosition(); // save as previous position to check for blocking of next customer
        }
    }

    // function checks if order must not be changed
    private static boolean mustPrecede(Customer i, Customer j) {
        if (i.getId() == j.getId()) { // could be removed due to implementation only left to be in line with original paper
            return i.getOrders().get(0) < j.getOrders().get(0);
        } else if (i.getId() < j.getId()) {
            return i.getOrders().get(0) <= j.getOrders().get(0) + (j.getId() - i.getId() - 1);
        } else
            return false;
    }

    public List<Integer> getCandidates() { // return all possible moves/customers to serve in form of an id list
        List<Integer> candidates = new ArrayList<>();
        if (!customers.get(0).getOrders().isEmpty()) { // first customer is always possible if order are present
            candidates.add(0);
        }

        IntStream.range(1, getNoCustomers()).forEach(j -> {
            if (!customers.get(j).getOrders().isEmpty()
                    && IntStream.range(0, j)
                            .allMatch(i -> customers.get(i).getOrders().isEmpty() || !mustPrecede(customers.get(i), customers.get(j)))) {
                candidates.add(j);
            }
        });

        return candidates;
    }

    public static int compare(State a, State b) {
        int result = Double.compare(a.totalTime, b.totalTime);
        if (result == 0) {
            result = Double.compare(a.walkedDistance, b.walkedDistance);
        }
        return result;
    }

    public void reset() {
        customers.stream().forEach(Customer::reset);
        waiterPosition = 0.0;
        walkedDistance = 0.0;
        totalTime = 0.0;
        waiterSchedule.clear();
    }

    public double getTime() {
        return totalTime;
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

    public double getWaiterVelocity() {
        return this.waiterVelocity;
    }

    public void setWaiterVelocity(double velocity) {
        this.waiterVelocity = velocity;
    }

    public double getServingTime() {
        return this.servingTime;
    }

    public void setServingTime(double servingTime) {
        this.servingTime = servingTime;
    }
}
