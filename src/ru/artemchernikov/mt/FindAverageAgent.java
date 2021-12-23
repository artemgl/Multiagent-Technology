package ru.artemchernikov.mt;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

public class FindAverageAgent extends Agent {

    private final Behaviour sendState = new SendState();
    private final Behaviour acceptValues = new AcceptValues();
    private final Behaviour rejectRequests = new RejectRequests();

    private int value;
    private int amount = 1;
    private String root;

    private final ArrayList<String> neighbours = new ArrayList<>();
    private final ArrayList<String> children = new ArrayList<>();
    private String parent;
    private boolean isReadyForAcceptance;

    private void becomeInitial() {
        addBehaviour(new InitialBehaviour());
    }

    private void becomeSender() {
        isReadyForAcceptance = false;
        addBehaviour(new Send());
        addBehaviour(rejectRequests);
    }

    private void stopSender() {
        removeBehaviour(rejectRequests);
    }

    private void becomeReceiver() {
        isReadyForAcceptance = true;
        addBehaviour(new Receive());
    }

    private void becomePassive() {
        isReadyForAcceptance = false;
        addBehaviour(new Passive());
    }

    public void addChild(String child) {
        children.add(child);
    }

    public void removeChild(String child) {
        children.remove(child);
    }

    @Override
    protected void setup() {
        this.root = this.getAID().getLocalName();
        this.value = 50 + Randomizer.nextInt() % 50;
        System.out.println("Agent " + getAID().getLocalName() + " setup, value = " + value);

        Object[] args = getArguments();
        for (Object arg : args) {
            neighbours.add(arg.toString());
        }

        addBehaviour(sendState);
        addBehaviour(acceptValues);
        becomeInitial();
    }

    public class SendState extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(((FindAverageAgent)myAgent).isReadyForAcceptance ? ACLMessage.CONFIRM : ACLMessage.DISCONFIRM);
                reply.setContent(myAgent.getAID().getLocalName());
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    public class AcceptValues extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = this.myAgent.receive(MessageTemplate.and(
                    MessageTemplate.MatchConversationId("Tuple"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
            ));
            if (msg != null) {
                FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;

                String[] words = msg.getContent().split(" ");
                int value = Integer.parseInt(words[0]);
                int agentAmount = Integer.parseInt(words[1]);
                myAgent.value += value;
                myAgent.amount += agentAmount;
            } else {
                block();
            }
        }

    }

    public class Passive extends Behaviour {

        private int indexOfChild = 0;

        private int state = 0;
        private static final int INITIAL_STATE = 0;
        private static final int PROCESS_JOIN = 1;
        private static final int TRY_TO_DELEGATE = 2;
        private static final int PROCESS_DELEGATION = 3;
        private static final int PROCESS_STATE = 4;
        private static final int FINAL_STATE = 5;

        private ACLMessage reply;

        private void stopWork(FindAverageAgent agent) {
            state = FINAL_STATE;
        }

        @Override
        public void action() {
            switch (state) {
                case INITIAL_STATE: {
                    ACLMessage msg = this.myAgent.receive();
                    if (msg != null) {
                        String conversationId = msg.getConversationId();
                        FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                        if (conversationId.equals("Delegate")) {
                            if (!myAgent.neighbours.isEmpty()) {
                                // Присваиваем информацию о дереве
                                String[] words = msg.getContent().split(" ");
                                myAgent.value = Integer.parseInt(words[0]);
                                myAgent.amount = Integer.parseInt(words[1]);

                                // Добавляем отправителя в список детей
                                myAgent.addChild(msg.getSender().getLocalName());

                                // Возвращаем подтверждение
                                ACLMessage confirm = msg.createReply();
                                confirm.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                myAgent.send(confirm);

                                // Сообщаем о необходимости обновить информацию о корне всему своему поддереву
                                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                                for (String child : myAgent.children) {
                                    inform.addReceiver(new AID(child, AID.ISLOCALNAME));
                                }
                                inform.setContent(myAgent.root);
                                inform.setConversationId("Root");
                                inform.setReplyWith("inform" + System.currentTimeMillis());
                                myAgent.send(inform);

                                stopWork(myAgent);
                                myAgent.addBehaviour(sendState);
                                myAgent.addBehaviour(acceptValues);
                                myAgent.becomeInitial();
                            } else {
                                reply = msg.createReply();
                                state = TRY_TO_DELEGATE;
                            }
                        } else if (conversationId.equals("Tuple")) {
                            // Перенаправляем информацию родителю
                            msg.clearAllReceiver();
                            msg.addReceiver(new AID(myAgent.parent, AID.ISLOCALNAME));
                            myAgent.send(msg);
                        } else if (conversationId.equals("Root")) {
                            String root = msg.getContent();

                            // Обновляем информацию о корне дерева
                            myAgent.root = root;

                            // Сообщаем о необходимости обновить информацию о корне всему своему поддереву
                            if (!myAgent.children.isEmpty()) {
                                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                                for (String child : myAgent.children) {
                                    inform.addReceiver(new AID(child, AID.ISLOCALNAME));
                                }
                                inform.setContent(root);
                                inform.setConversationId("Root");
                                inform.setReplyWith("inform" + System.currentTimeMillis());
                                myAgent.send(inform);
                            }
                        } else if (conversationId.equals("State")) {
                            // Запрашиваем состояние корня у родителя
                            ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
                            query.addReceiver(new AID(myAgent.parent, AID.ISLOCALNAME));
                            query.setConversationId("State");
                            query.setReplyWith("query" + System.currentTimeMillis());
                            myAgent.send(query);

                            reply = msg.createReply();
                            state = PROCESS_STATE;
                        } else if (conversationId.equals("Join")) {
                            // Запрашиваем состояние корня у родителя
                            ACLMessage query = new ACLMessage(ACLMessage.QUERY_IF);
                            query.addReceiver(new AID(myAgent.parent, AID.ISLOCALNAME));
                            query.setConversationId("State");
                            query.setReplyWith("query" + System.currentTimeMillis());
                            myAgent.send(query);

                            reply = msg.createReply();
                            state = PROCESS_JOIN;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case PROCESS_JOIN: {
                    ACLMessage msg = this.myAgent.receive(
                            MessageTemplate.and(
                                    MessageTemplate.MatchConversationId("State"),
                                    MessageTemplate.or(
                                            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                            MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)
                                    )
                            )
                    );
                    if (msg != null) {
                        FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;

                        if (myAgent.root.equals(msg.getSender().getLocalName())) {
                            // Отклоняем запрос на присоединение
                            reply.setPerformative(ACLMessage.CANCEL);
                            reply.setContent("Same AID");
                            myAgent.send(reply);

                            state = INITIAL_STATE;
                            break;
                        }

                        int performative = msg.getPerformative();
                        switch (performative) {
                            case ACLMessage.CONFIRM:
                                // Одобряем запрос на присоединение
                                reply.setPerformative(ACLMessage.AGREE);
                                reply.setContent(myAgent.root);
                                myAgent.send(reply);

                                // Отсылаем данные отправителя родителю
                                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                                inform.addReceiver(new AID(myAgent.parent, AID.ISLOCALNAME));
                                inform.setConversationId("Tuple");
                                inform.setReplyWith("inform" + System.currentTimeMillis());
                                myAgent.send(inform);

                                state = INITIAL_STATE;
                                break;
                            case ACLMessage.DISCONFIRM:
                                // Отклоняем запрос на присоединение
                                reply.setPerformative(ACLMessage.CANCEL);
                                reply.setContent("Sending");
                                myAgent.send(reply);

                                state = INITIAL_STATE;
                                break;
                        }
                    } else {
                        block();
                    }
                    break;
                }
                case TRY_TO_DELEGATE: {
                    // Отправляем предложение стать корнем

                    FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                    if (indexOfChild < myAgent.children.size()) {
                        ACLMessage propose = new ACLMessage(ACLMessage.REQUEST);
                        propose.addReceiver(new AID(children.get(indexOfChild++), AID.ISLOCALNAME));
                        propose.setContent(myAgent.value + " " + myAgent.amount);
                        propose.setConversationId("Delegate");
                        propose.setReplyWith("propose" + System.currentTimeMillis());
                        myAgent.send(propose);

                        state = PROCESS_DELEGATION;
                    } else {
                        // Не осталось детей, готовых стать корнем

                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        myAgent.send(reply);
                        indexOfChild = 0;
                        state = INITIAL_STATE;
                    }

                    break;
                }
                case PROCESS_DELEGATION: {
                    // Ждём ответа на предложение

                    ACLMessage msg = this.myAgent.receive(
                            MessageTemplate.and(
                                    MessageTemplate.MatchConversationId("Delegate"),
                                    MessageTemplate.or(
                                            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                            MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                                    )
                            )
                    );
                    if (msg != null) {
                        int performative = msg.getPerformative();
                        switch (performative) {
                            case ACLMessage.ACCEPT_PROPOSAL:
                                FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                                myAgent.parent = msg.getSender().getLocalName();
                                myAgent.removeChild(msg.getSender().getLocalName());

                                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                myAgent.send(reply);
                                indexOfChild = 0;
                                state = INITIAL_STATE;
                                break;
                            case ACLMessage.REJECT_PROPOSAL:
                                state = TRY_TO_DELEGATE;
                                break;
                        }
                    } else {
                        block();
                    }

                    break;
                }
                case PROCESS_STATE: {
                    // Ожидаем сообщение о состоянии корня и отвечаем тому, кто его запрашивал

                    ACLMessage msg = this.myAgent.receive(MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)
                    ));
                    if (msg != null) {
                        reply.setPerformative(msg.getPerformative());
                        reply.setContent(msg.getContent());
                        myAgent.send(reply);
                        state = INITIAL_STATE;
                    } else {
                        block();
                    }

                    break;
                }
            }
        }

        @Override
        public boolean done() {
            return state == FINAL_STATE;
        }
    }

    public class RejectRequests extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage msg = this.myAgent.receive(MessageTemplate.and(
                    MessageTemplate.MatchConversationId("Join"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
            ));
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CANCEL);
                reply.setContent("Sending");
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    public class Send extends Behaviour {

        private int indexOfChild = 0;

        private int state = 0;
        private static final int TRY_TO_JOIN = 0;
        private static final int PROCESS_ANSWER = 1;
        private static final int TRY_TO_DELEGATE = 2;
        private static final int PROCESS_DELEGATION = 3;
        private static final int FINAL_STATE = 4;

        private void stopWork(FindAverageAgent agent) {
            agent.stopSender();
            state = FINAL_STATE;
        }

        @Override
        public void action() {
            switch (state) {
                case TRY_TO_JOIN: {
                    // Отправляем запрос на присоединение

                    FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                    if (!myAgent.neighbours.isEmpty()) {
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(new AID(neighbours.get(0), AID.ISLOCALNAME));
                        request.setContent(myAgent.value + " " + myAgent.amount);
                        request.setConversationId("Join");
                        request.setReplyWith("request" + System.currentTimeMillis());
                        myAgent.send(request);

                        state = PROCESS_ANSWER;
                    } else {
                        // Некому отправлять запрос, изменяем корень дерева

                        state = TRY_TO_DELEGATE;
                    }

                    break;
                }
                case PROCESS_ANSWER: {
                    // Ждём ответа на присланный запрос

                    ACLMessage msg = this.myAgent.receive(
                            MessageTemplate.and(
                                    MessageTemplate.MatchConversationId("Join"),
                                    MessageTemplate.or(
                                            MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                            MessageTemplate.MatchPerformative(ACLMessage.CANCEL)
                                    )
                            )
                    );
                    if (msg != null) {
                        int performative = msg.getPerformative();
                        switch (performative) {
                            case ACLMessage.AGREE: {
                                String root = msg.getContent();
                                FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;

                                // Убираем соседа
                                myAgent.neighbours.remove(msg.getSender().getLocalName());

                                // Запоминаем родителя
                                myAgent.parent = msg.getSender().getLocalName();

                                // Обновляем информацию о корне дерева
                                myAgent.root = root;

                                // Сообщаем о необходимости обновить информацию о корне всему своему поддереву
                                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                                for (String child : myAgent.children) {
                                    inform.addReceiver(new AID(child, AID.ISLOCALNAME));
                                }
                                inform.setContent(root);
                                inform.setConversationId("Root");
                                inform.setReplyWith("inform" + System.currentTimeMillis());
                                myAgent.send(inform);

                                stopWork(myAgent);
                                myAgent.removeBehaviour(sendState);
                                myAgent.removeBehaviour(acceptValues);
                                becomePassive();
                                break;
                            }
                            case ACLMessage.CANCEL: {
                                FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                                String reason = msg.getContent();
                                if (reason.equals("Sending")) {
                                    // Тот, к кому обратились, сам отправитель

                                    stopWork(myAgent);
                                    becomeInitial();
                                } else if (reason.equals("Same AID")) {
                                    // Обратились к узлу нашего дерева

                                    // Убрать из соседей узел нашего дерева
                                    myAgent.neighbours.remove(msg.getSender().getLocalName());
                                    stopWork(myAgent);
                                    becomeInitial();
                                } else {
                                    System.out.println("Unknown reason of cancel for " + this.myAgent.getAID().getLocalName());
                                }
                                break;
                            }
                        }
                    } else {
                        block();
                    }

                    break;
                }
                case TRY_TO_DELEGATE: {
                    // Отправляем предложение стать корнем

                    FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                    if (indexOfChild < children.size()) {
                        ACLMessage propose = new ACLMessage(ACLMessage.REQUEST);
                        propose.addReceiver(new AID(children.get(indexOfChild++), AID.ISLOCALNAME));
                        propose.setContent(myAgent.value + " " + myAgent.amount);
                        propose.setConversationId("Delegate");
                        propose.setReplyWith("propose" + System.currentTimeMillis());
                        myAgent.send(propose);

                        state = PROCESS_DELEGATION;
                    } else {
                        // Не осталось детей, готовых стать корнем

                        // Алгоритм завершён
                        System.out.println("The answer is " + (float)myAgent.value / (float)myAgent.amount);
                        stopWork(myAgent);
                    }

                    break;
                }
                case PROCESS_DELEGATION: {
                    // Ждём ответа на предложение

                    ACLMessage msg = this.myAgent.receive(
                            MessageTemplate.and(
                                    MessageTemplate.MatchConversationId("Delegate"),
                                    MessageTemplate.or(
                                            MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                            MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                                    )
                            )
                    );
                    if (msg != null) {
                        int performative = msg.getPerformative();
                        switch (performative) {
                            case ACLMessage.ACCEPT_PROPOSAL:
                                FindAverageAgent myAgent = (FindAverageAgent) this.myAgent;
                                myAgent.children.remove(msg.getSender().getLocalName());
                                myAgent.parent = msg.getSender().getLocalName();

                                indexOfChild = 0;
                                stopWork(myAgent);
                                becomePassive();
                                break;
                            case ACLMessage.REJECT_PROPOSAL:
                                state = TRY_TO_DELEGATE;
                                break;
                        }
                    } else {
                        block();
                    }

                    break;
                }
            }
        }

        @Override
        public boolean done() {
            return state == FINAL_STATE;
        }
    }

    public class Receive extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            FindAverageAgent myAgent = (FindAverageAgent)this.myAgent;
            while (msg != null) {
                // Убираем отправителя из соседей
                myAgent.neighbours.remove(msg.getSender().getLocalName());
                // Добавляем отправителя в детей
                myAgent.addChild(msg.getSender().getLocalName());
                // Запоминаем сумму чисел и количество агентов в дереве
                String[] words = msg.getContent().split(" ");
                int value = Integer.parseInt(words[0]);
                int agentAmount = Integer.parseInt(words[1]);
                myAgent.value += value;
                myAgent.amount += agentAmount;

                // Отвечаем одобрением отправителю и сообщаем свой идентификатор
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.AGREE);
                reply.setContent(myAgent.getAID().getLocalName());
                myAgent.send(reply);
                msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            }
            becomeInitial();
        }
    }

    public class InitialBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            int randInt = Randomizer.nextInt();
            FindAverageAgent myAgent = (FindAverageAgent)this.myAgent;
            if (randInt % 2 == 0) {
                myAgent.becomeReceiver();
            } else {
                myAgent.becomeSender();
            }
        }
    }

}

