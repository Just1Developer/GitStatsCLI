package net.justonedev.statswrapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MutableIntegerPair {
    private int first;
    private int second;

    public MutableIntegerPair() {
        first = 0;
        second = 0;
    }

    public MutableIntegerPair(int first, int second) {
        this.first = first;
        this.second = second;
    }

    public void incrementFirst() {
        incrementFirst(1);
    }

    public void incrementFirst(int by) {
        this.first += by;
    }

    public void incrementSecond() {
        incrementSecond(1);
    }

    public void incrementSecond(int by) {
        this.second += by;
    }
}
