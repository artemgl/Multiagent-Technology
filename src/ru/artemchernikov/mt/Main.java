package ru.artemchernikov.mt;

import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.ejml.data.FMatrixSparse;
import org.ejml.data.FMatrixSparseCSC;

import java.util.ArrayList;
import java.util.Iterator;

public class Main {
    public static void main(String[] args) throws StaleProxyException {

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

//        int n = 3;
//        FMatrixSparse a = new FMatrixSparseCSC(n, n);
//        a.set(0, 1, 1);
//        a.set(0, 2, 1);
//        a.set(1, 2, 1);
//
//        a.set(1, 0, 1);
//        a.set(2, 0, 1);
//        a.set(2, 1, 1);



        MainController mc = new MainController();
        mc.initAgents(a);


//        for (int i = 0; i < n; i++) {
//            for (int j = 0; j < m; j++) {
//                System.out.print((a.get(i, j, 0) == 1.0 ? "X" : "O") + " ");
//            }
//            System.out.println("");
//        }


//        Runtime runtime = Runtime.instance();
//        Profile profile = new ProfileImpl();
//        profile.setParameter(Profile.MAIN_HOST, "localhost");
//        profile.setParameter(Profile.MAIN_PORT, "8932");
//        profile.setParameter(Profile.GUI, "true");
//
//        runtime.createMainContainer(profile).createNewAgent("klukva", "ru.artemchernikov.mt.BookBuyerAgent", new Object[0]).start();
    }
}

