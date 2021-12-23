package ru.artemchernikov.mt;

import java.util.Random;

public class Randomizer {

    private static final Random rand = new Random(System.currentTimeMillis());

    public static int nextInt() {
        return rand.nextInt();
    }

}
