package kasselsensors;

import com.rabbitmq.client.*;

import java.io.IOException;

public class Consumer {

	private final static String QUEUE_NAME = "sensorData";

	public static void main(String[] args) throws Exception {

		// Connecting to a broker
		ConnectionFactory factory = new ConnectionFactory();
		String uri = System.getenv("CLOUDAMQP_URL");
    	if (uri == null) uri = "amqp://guest:guest@localhost";
    	factory.setUri(uri);
		Connection connection = factory.newConnection();
		
		// Open a channel
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		DefaultConsumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {

				// Receive data string
				String message = new String(body, "UTF-8");
				System.out.println(" [x] Received '" + message + "'");
			
			}
		};
		channel.basicConsume(QUEUE_NAME, true, consumer);

	}
}
