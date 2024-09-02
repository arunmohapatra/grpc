package com.grpc;

import com.grpc.stub.OrderServiceGrpc;
import com.grpc.stub.OrderCount;
import com.grpc.stub.OrderDetails;
import com.grpc.stub.OrderId;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@GrpcService
public class OrderService extends OrderServiceGrpc.OrderServiceImplBase{

    private static final Logger logger = LogManager.getLogger(OrderService.class);

    //Synchronous method call
    //Client will wait for server response
    @Override
    public void createOrderForGivenOrderId(OrderId request, StreamObserver<OrderDetails> responseObserver) {
        logger.info("Get Order id and generate order {} ", request.getOrderId());
        OrderDetails orderDetails
                = OrderDetails.newBuilder().setOrderId(request.getOrderId()).setName("Shoes").build();
        responseObserver.onNext(orderDetails);
        responseObserver.onCompleted();
    }

    //Server Side streaming
    //Create order detail for given count and return it back tp client
    @Override
    public void streamAllOrderForGivenCount(OrderCount request, StreamObserver<OrderDetails> responseObserver) {
        logger.info("Server side streaming , create order and sent it back to client asynchronously for count  {} ", request.getCount());
        for(int id = 0 ; id < request.getCount(); id++) {
            OrderDetails orderDetails = OrderDetails.newBuilder().setOrderId(Integer.toString(id++)).setName("Order-"+id++).build();
            responseObserver.onNext(orderDetails);
            responseObserver.onCompleted();
        }
    }

    //Client side streaming
    //Continously uploading the Order Details and return count
    @Override
    public StreamObserver<OrderDetails> uploadOrderDetails(StreamObserver<OrderCount> responseObserver) {
        return new StreamObserver<OrderDetails>() {
            String orderId;
            int count;
            @Override
            public void onNext(OrderDetails orderDetails) {
                count++;
                logger.info("Order details uploaded to server for order id {} and order name {}",
                        orderDetails.getOrderId(), orderDetails.getName());
                orderId = orderDetails.getOrderId();
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Upload failed");
            }

            @Override
            public void onCompleted() {
                OrderCount orderCount = OrderCount.newBuilder().setCount(count).build();
                responseObserver.onNext(orderCount);
                responseObserver.onCompleted();
                logger.info("Total number of order uploaded til now {}", count);
            }
        };
    }


    /*Bi-Directional*/
    @Override
    public StreamObserver<OrderDetails> uploadAOrderDetailsAndReturnCount(StreamObserver<OrderCount> responseObserver) {
            return new StreamObserver<OrderDetails>() {
                String orderId;
                int count= 0;
                @Override
                public void onNext(OrderDetails orderDetails) {
                    //Get Order details
                    orderId = orderDetails.getOrderId();
                    String name = orderDetails.getName();
                    logger.info("Order id {} and Order name {} get updated",orderId, name);
                    responseObserver.onNext(OrderCount.newBuilder().setCount(Integer.parseInt(orderId)).build());
                }

                @Override
                public void onError(Throwable throwable) {
                    logger.info("error..");
                }

                @Override
                public void onCompleted() {
                    responseObserver.onNext(OrderCount.newBuilder().setCount(Integer.parseInt(orderId)).build());
                    responseObserver.onCompleted();
                    logger.info("Bi-Streaming complete..");
                }
            };
    }
}
