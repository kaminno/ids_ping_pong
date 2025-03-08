import com.rabbitmq.client.*;
import java.util.Scanner;

public class Ping {

    // different exchanges for different type of communication - push -> ping/pong, ping <-> pong
    private static final String PING_PONG_EXCHANGE = "ping_pong";
    private static final String PUSH_EXCHANGE = "push";
    private static boolean started = false;

    public static void main(String[] argv) throws Exception {
        // message definition
        Integer id = 1;
        // boolean started = false;
        String msg_ping = "PING";
        String msg_ask = "ASK-" + id;
        String msg_ack = "ACK";

        // connection setup
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(PING_PONG_EXCHANGE, BuiltinExchangeType.DIRECT);
        
        // two queues and binding keys for two communications; one with controller and one with pong
        String send_route = "ping";
        String receive_route = "pong";
        String ping_route = "ping_route";
        String ping_queue = "ping_queue";
        String ping_push_queue = "ping_push_queue";
        channel.queueDeclare(ping_queue, false, false, false, null);
        channel.queueBind(ping_queue, PING_PONG_EXCHANGE, receive_route);
        channel.queueDeclare(ping_push_queue, false, false, false, null);
        channel.queueBind(ping_push_queue, PUSH_EXCHANGE, ping_route);

        System.out.println("Listening...");
        DeliverCallback pingPongCallback = (consumerTag, delivery) -> {
            try{
                String requestMessage = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + requestMessage + "'");
                // sleep to make the exchange (printing) better visible
                Thread.sleep(2000);
                
                // getting ask message
                if(requestMessage.contains("-")){
                    System.out.println("ASK received");
                    if(!started){
                        String msg = msg_ack;
                        channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, msg.getBytes("UTF-8"));
                        started = true;
                        System.out.println("ACK send");
                    }
                    else if(started){
                        Integer msgId = Integer.parseInt(requestMessage.split("-")[1]);
                        System.out.println("message id: " + msgId);
                        if(msgId < id){
                            String msg = msg_ack;
                            channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, msg.getBytes("UTF-8"));
                            System.out.println("ACK send");
                        }
                    }
                }
                // getting acknolegemeng
                else if(requestMessage.equals(msg_ack)){
                    System.out.println("ACK received");
                    String msg = msg_ping;
                    channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, msg.getBytes("UTF-8"));
                }
                // getting pong
                else if(requestMessage.equals("PONG")){
                    System.out.println("PONG received");
                    String msg = msg_ping;
                    channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, msg.getBytes("UTF-8"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Obnoví stav přerušení vlákna
                System.err.println("Sleep interrupted: " + e.getMessage());
            }
        };
        // subscribing for pong
        channel.basicConsume(ping_queue, true, pingPongCallback, consumerTag -> {});

        DeliverCallback pushCallback = (consumerTag, delivery) -> {
            String requestMessage = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + requestMessage + "'");
            // algo code
            if(requestMessage.equals("PUSH") && !started){
                String message = msg_ask;
                started = true;
                // sending message
                channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, message.getBytes("UTF-8"));
                System.out.println("ASK sent");
            }
        };
        // subscribing for push
        channel.basicConsume(ping_push_queue, true, pushCallback, consumerTag -> {});
    }
}