package ru.artemchernikov.mt;

import org.ejml.data.FMatrixSparse;
import org.ejml.data.FMatrixSparseCSC;

public class Main {
    public static void main(String[] args) {
        int n = 5;
        FMatrixSparse a = new FMatrixSparseCSC(n, n);
        a.set(0, 1, 1);
        a.set(0, 2, 1);
        a.set(1, 2, 1);
        a.set(0, 3, 1);
        a.set(1, 3, 1);
        a.set(2, 3, 1);
        a.set(0, 4, 1);
        a.set(1, 4, 1);
        a.set(2, 4, 1);
        a.set(3, 4, 1);

        a.set(1, 0, 1);
        a.set(2, 0, 1);
        a.set(2, 1, 1);
        a.set(3, 0, 1);
        a.set(3, 1, 1);
        a.set(3, 2, 1);
        a.set(4, 0, 1);
        a.set(4, 1, 1);
        a.set(4, 2, 1);
        a.set(4, 3, 1);

        MainController mc = new MainController();
        mc.initAgents(a, 1.0 / (n + 1), 100, 20);
    }
}

