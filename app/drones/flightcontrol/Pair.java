package drones.flightcontrol;

/**
 * A pair consisting of two elements.
 *
 * Created by Sander on 5/05/2015.
 */
public class Pair<K, V> {
    public final K key;
    public final V value;

    public Pair(K first, V second) {
        this.key = first;
        this.value = second;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}