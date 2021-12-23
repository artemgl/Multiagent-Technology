package ru.artemchernikov.mt;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

public class FindAverageAgent extends Agent {

    private double alpha;
    private double value;

    private int ttl;

    private static final double NOISE = 0.5;
    private static final double FAIL_PROBABILITY = 0.2;
    private static final double DELAY_PROBABILITY = 0.3;

    private final ArrayList<String> neighbours = new ArrayList<>();

    @Override
    protected void setup() {
        this.value = 50 + Randomizer.nextInt() % 50;
        System.out.println("Agent " + getAID().getLocalName() + " setup, value = " + value);

        Object[] args = getArguments();
        alpha = Double.parseDouble(args[0].toString());
        long period = Long.parseLong(args[1].toString());
        ttl = Integer.parseInt(args[2].toString());

        for (int i = 3; i < args.length; i++) {
            neighbours.add(args[i].toString());
        }

        addBehaviour(new Receive(this, period));
        addBehaviour(new Send(this, period));
    }

    public class Receive extends TickerBehaviour {

        public Receive(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            ACLMessage msg = myAgent.receive();
            double sum = 0.0;
            while (msg != null) {
                sum += Double.parseDouble(msg.getContent()) - value;
                msg = myAgent.receive();
            }
            sum *= alpha;
            value += sum;
            ttl -= 1;
            if (ttl == 0) {
                System.out.println("Agent " + getAID().getLocalName() + " finish, value = " + value);
            }
        }
    }

    public class Send extends TickerBehaviour {

        private final ArrayList<Boolean> needsSending = new ArrayList<>();
        private double previousValue;

        public Send(Agent a, long period) {
            super(a, period);
            FindAverageAgent agent = (FindAverageAgent)a;
            for (Object obj : agent.neighbours) {
                needsSending.add(true);
            }
        }

        @Override
        protected void onTick() {
            for (int i = 0; i < neighbours.size(); i++) {
                String neighbour = neighbours.get(i);
                if (Randomizer.nextDouble() < FAIL_PROBABILITY) {
                    continue;
                }

                if (!needsSending.get(i)) {
                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                    inform.addReceiver(new AID(neighbour, AID.ISLOCALNAME));
                    inform.setContent((previousValue + (2.0 * Randomizer.nextDouble() - 1.0) * NOISE) + "");
                    inform.setConversationId("Send value");
                    inform.setReplyWith("inform" + System.currentTimeMillis());
                    myAgent.send(inform);
                }

                needsSending.set(i, Randomizer.nextDouble() > DELAY_PROBABILITY);
                if (needsSending.get(i)) {
                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                    inform.addReceiver(new AID(neighbour, AID.ISLOCALNAME));
                    inform.setContent((value + (2.0 * Randomizer.nextDouble() - 1.0) * NOISE) + "");
                    inform.setConversationId("Send value");
                    inform.setReplyWith("inform" + System.currentTimeMillis());
                    myAgent.send(inform);
                }
            }
            previousValue = value;
        }
    }

}

