import com.rabbitmq.client.*;
import java.util.Scanner;

public class Controller {

    private static final String PUSH_EXCHANGE = "push";

    public static void main(String[] argv) throws Exception {
        System.out.println("PUSH message controller. Type 'ping' or 'pong' to send PUSH message there.");
        Scanner scanner = new Scanner(System.in);

        // setup connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // setup exchange for push messages
        channel.exchangeDeclare(PUSH_EXCHANGE, BuiltinExchangeType.DIRECT);

        // ping and pong have each different queue + binding
        String ping_route = "ping_route";
        String pong_route = "pong_route";
        // allow sending messages
        while(true){
            String queue = scanner.nextLine();
            // push ping
            if(queue.equals("ping")){
                channel.basicPublish(PUSH_EXCHANGE, ping_route, null, "PUSH".getBytes("UTF-8"));
                System.out.println("PUSH sent to " + ping_route);
            }
            // push pong
            else if(queue.equals("pong")){
                channel.basicPublish(PUSH_EXCHANGE, pong_route, null, "PUSH".getBytes("UTF-8"));
                System.out.println("PUSH sent to " + pong_route);
            }
            else{
                System.out.println("The input has to be 'ping' or 'pong'.");
            }
        }
    }
}