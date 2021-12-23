package ru.artemchernikov.mt;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import org.ejml.data.FMatrixSparse;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MainController {

    private static final String HOST = "localhost";
    private static final String PORT = String.valueOf(8932);
    private static final String GUI = String.valueOf(true);

    public void initAgents(FMatrixSparse adjacencyMtx, double alpha, long period, int ttl) {
        ContainerController container = createMainContainer();

        int numberOfAgents = adjacencyMtx.getNumRows();
        ArrayList<ArrayList<Integer>> args = getArgs(adjacencyMtx);

        try {
            for(int i = 0; i < numberOfAgents; i++) {
                ArrayList<Object> res = new ArrayList<>();
                res.add(alpha);
                res.add(period);
                res.add(ttl);
                res.addAll(args.get(i));
                AgentController agent = container.createNewAgent(String.valueOf(i), "ru.artemchernikov.mt.FindAverageAgent", res.toArray());
                agent.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ArrayList<ArrayList<Integer>> getArgs(FMatrixSparse adjacencyMtx) {
        ArrayList<ArrayList<Integer>> args = new ArrayList<>();

        int numberOfAgents = adjacencyMtx.getNumRows();
        for (int i = 0; i < numberOfAgents; i++) {
            args.add(new ArrayList<>());
        }

        Iterator<FMatrixSparse.CoordinateRealValue> it = adjacencyMtx.createCoordinateIterator();
        while (it.hasNext()) {
            FMatrixSparse.CoordinateRealValue coord = it.next();
            args.get(coord.row).add(coord.col);
        }

        return args;
    }

    private ContainerController createMainContainer() {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, HOST);
        profile.setParameter(Profile.MAIN_PORT, PORT);
        profile.setParameter(Profile.GUI, GUI);

        return Runtime.instance().createMainContainer(profile);
    }
}
