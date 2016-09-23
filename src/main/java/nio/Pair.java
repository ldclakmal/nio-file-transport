package nio;

/**
 * This class is to keep the pair of time slots which is related to the overflow
 * The start time of the overflow and the end time of the overflow is kept here
 *
 * @author Chanaka Lakmal
 */
@SuppressWarnings("WeakerAccess")
public class Pair<T, U> {

    private T t;
    private U u;

    public Pair(T t, U u) {
        this.t = t;
        this.u = u;
    }

    public T getT() {
        return t;
    }

    public U getU() {
        return u;
    }

    public void setT(T t) {
        this.t = t;
    }

    public void setU(U u) {
        this.u = u;
    }
}