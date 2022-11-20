import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.lang.Math;


public class App {

    // function checks if order must not be changed
    private static boolean mustPrecede(Customer i, Customer j) {
        if (i.nr == j.nr) // could be removed due to implementation only left to be in line with original paper
            return i.orders.get(0) < j.orders.get(0);
        else if (i.nr < j.nr)
            return i.orders.get(0) <= j.orders.get(0);
        else
            return j.orders.get(0) < i.orders.get(0) && i.orders.get(0) <= j.orders.get(0) + (j.nr - i.nr -1);
    }

            


    // function to move all customers for a given order to process
    private static State serveCustomer(double v_waiter, double v_cust, double t_serving, State state, Customer nextCustomer){
        int servedOrderAt = nextCustomer.orders.remove(0);

        state.servicedOrder.add(new int[]{nextCustomer.nr, servedOrderAt});
        
        double walkedDistance = Math.abs(servedOrderAt-state.waiterPosition);
        state.walkedDistance += walkedDistance;

        double delta_t = Math.max(servedOrderAt - nextCustomer.position/v_cust, walkedDistance/v_waiter) + t_serving; // time required to serve customer

        state.time += delta_t;
        state.waiterPosition = servedOrderAt;
        nextCustomer.position = servedOrderAt;

        // update customer position of each of the n customers
        double previousPosition = Double.MAX_VALUE;
        for (Iterator<Customer> it= (new ArrayList<>(state.customers)).iterator();it.hasNext();){
            Customer c = it.next();
            if(!nextCustomer.equals(c)){
                c.position = Math.min(c.position + delta_t * v_cust, previousPosition - 1);//update position ignoring next order
                
                if(!c.orders.isEmpty()){ // take order into account if one exists
                    c.position = Math.min(c.orders.get(0), c.position);
                }
            }
            previousPosition = c.position; // save as previous position to check for blocking of next customer
        }

        return state;
    }

    private static List<Customer> getCandidates(State state){
        List<Customer> candidates = new ArrayList<>();
        if (!state.customers.get(0).orders.isEmpty()){ // first customer is always possible if order are present
            candidates.add(state.customers.get(0));
        }

        IntStream.range(1, state.customers.size()).forEach(j->{
            if (!state.customers.get(j).orders.isEmpty()
                && IntStream.range(0,j)
                .allMatch(i->state.customers.get(i).orders.isEmpty() || !mustPrecede(state.customers.get(i), state.customers.get(j)))){
                    candidates.add(state.customers.get(j));
            }
        });

        return candidates;
    }

    private static void testServeCustomer(){
        // Test of serve Customer function
        int n=3; //number of customers

        State s = new State();

        int pos = -1;
        s.customers.add(new Customer(0, pos--, List.of(1,3)));
        s.customers.add(new Customer(1, pos--, List.of(1)));
        s.customers.add(new Customer(2, pos--, List.of(2)));

        List<Integer> servedOrder= List.of(0,1,0,2);
        double v_waiter = 1.0;
        double v_cust = 1.0;
        double t_serving = 2.0;

        for (int nextCustomer : servedOrder){
            State newState = serveCustomer(v_waiter, v_cust, t_serving, s, s.customers.get(nextCustomer));
            System.out.println("Time: " + newState.time);
        }
    }


    // Beamsearch
    State beamSearch(int beta, double v_waiter, double v_cust, double t_serving, State state){
        int totalOrders = state.customers.stream().mapToInt(c-> c.orders.size()).sum();


        List<State> currentLevel = new ArrayList<>(List.of(state));//  list containing states at current level
        for(int i = 0; i<totalOrders; i++){ // the tree must be expanded once for every order
            List<State> newLevel = currentLevel.stream().parallel().map(s->
                getCandidates(s).stream().parallel().map(c-> serveCustomer(v_waiter, v_cust, t_serving, s, c)).toList()
            ).flatMap(List::stream).toList();

            if (newLevel.size()> beta){
                newLevel.sort(State::compare);
                currentLevel = newLevel.subList(0, beta);
            }else{
                currentLevel = newLevel;
            }
        }
        currentLevel.sort(State::compare);
        return currentLevel.get(0); // we only return the best result
    }


    public static void main(String[] args) {
        testServeCustomer();
    }
    
}
