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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (!key.equals(pair.key)) return false;
        if (!value.equals(pair.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}