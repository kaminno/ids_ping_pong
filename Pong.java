import com.rabbitmq.client.*;
import java.util.Scanner;

// same class as Ping, just with swapped messages, names, variables etc.
public class Pong {
    private static final String PING_PONG_EXCHANGE = "ping_pong";
    private static final String PUSH_EXCHANGE = "push";
    private static boolean started = false;

    public static void main(String[] argv) throws Exception {
        Integer id = 2;
        // boolean started = false;
        String msg_pong = "PONG";
        String msg_ask = "ASK-" + id;
        String msg_ack = "ACK";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(PING_PONG_EXCHANGE, BuiltinExchangeType.DIRECT);
            
        String send_route = "pong";
        String receive_route = "ping";
        String pong_route = "pong_route";
        String pong_queue = "pong_queue";
        String pong_push_queue = "pong_push_queue";
        channel.queueDeclare(pong_queue, false, false, false, null);
        channel.queueBind(pong_queue, PING_PONG_EXCHANGE, receive_route);
        channel.queueDeclare(pong_push_queue, false, false, false, null);
        channel.queueBind(pong_push_queue, PUSH_EXCHANGE, pong_route);

        System.out.println("Listening...");
        DeliverCallback pingPongCallback = (consumerTag, delivery) -> {
            try{
                String requestMessage = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + requestMessage + "'");
                Thread.sleep(2000);
                
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
                else if(requestMessage.equals(msg_ack)){
                    System.out.println("ACK received");
                    String msg = msg_pong;
                    channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, msg.getBytes("UTF-8"));
                }
                else if(requestMessage.equals("PING")){
                    System.out.println("PING received");
                    String msg = msg_pong;
                    channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, msg.getBytes("UTF-8"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Obnoví stav přerušení vlákna
                System.err.println("Sleep interrupted: " + e.getMessage());
            }
        };
        channel.basicConsume(pong_queue, true, pingPongCallback, consumerTag -> {});

        DeliverCallback pushCallback = (consumerTag, delivery) -> {
            String requestMessage = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + delivery.getEnvelope().getRoutingKey() + "':'" + requestMessage + "'");
            if(requestMessage.equals("PUSH") && !started){
                String message = msg_ask;
                started = true;
                channel.basicPublish(PING_PONG_EXCHANGE, send_route, null, message.getBytes("UTF-8"));
                System.out.println("ASK sent");
            }
        };
        channel.basicConsume(pong_push_queue, true, pushCallback, consumerTag -> {});
    }
}
