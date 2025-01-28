import os
import json
from azure.servicebus import ServiceBusClient, ServiceBusMessage

# Set up the Service Bus namespace connection details
CONNECTION_STR = "https://BusFor8917LabOne.servicebus.windows.net/lab1queue"
QUEUE_NAME = "lab1queue"

# JSON message body
message_body = {
    "orderId": "ORD-123",
    "items": [
        "Laptop",
        "Mouse",
        "Keyboard"
    ]
}

def send_message_to_service_bus():
    # Create a Service Bus client
    servicebus_client = ServiceBusClient.from_connection_string(conn_str=CONNECTION_STR)

    # Get a sender for the queue
    with servicebus_client:
        sender = servicebus_client.get_queue_sender(queue_name=QUEUE_NAME)
        with sender:
            # Convert the JSON body to a string
            message_content = json.dumps(message_body)
            
            # Create a Service Bus message
            message = ServiceBusMessage(message_content)

            # Send the message
            print(f"Sending message: {message_content}")
            sender.send_messages(message)
            print("Message sent successfully!")

if __name__ == "__main__":
    send_message_to_service_bus()
