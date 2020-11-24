package my.com.prasarana.rapidbus.mapitintegration.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import my.com.prasarana.rapidbus.mapitintegration.RKL_GTFS_RealtimeGrpc;
import my.com.prasarana.rapidbus.mapitintegration.RequestAVL;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.Executors;

public class MapitIntegrationClient {

    private boolean stopsig = false;

    public static void main(String[] args) throws SSLException {
        new MapitIntegrationClient().run();
    }

    public void run() throws SSLException {
        Properties gRPCprops = new Properties();

        try(InputStream inputStream = new FileInputStream(new File("./config/grpc.properties"))) {
            gRPCprops.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String serverAddress = gRPCprops.getProperty("server.address");
        String serverPort = gRPCprops.getProperty("server.port");

        ManagedChannel channel =
                NettyChannelBuilder.forAddress(serverAddress,
                        Integer.parseInt(serverPort))
                        .sslContext(
                                GrpcSslContexts.forClient().trustManager(new File("ssl/ca.crt")).build()
                        ).build();

        getRealtimeAVL(channel, serverAddress, serverPort);

    }

    private void getRealtimeAVL(ManagedChannel channel, String serverAddress, String serverPort) {

        Logger logger = LoggerFactory.getLogger(getClass().getName());

        RKL_GTFS_RealtimeGrpc.RKL_GTFS_RealtimeBlockingStub stub =
                RKL_GTFS_RealtimeGrpc.newBlockingStub(channel).withExecutor(Executors.newSingleThreadExecutor());

        MongoCollection<Document> collection = new MongoClientConfig().createClient();

        logger.info("Request established to {}:{}", serverAddress, serverPort);

        while (true) {
            try {
                stub.getGtfsRealtime(
                        RequestAVL.newBuilder()
                                .setTimestamp(new Timestamp(System.currentTimeMillis()).toString())
                                .build()
                ).forEachRemaining(response -> {
                    try {
                        Gson gson = new GsonBuilder().serializeNulls().create();
                        Document document = gson.fromJson(JsonFormat.printer().print(response), Document.class);
                        collection.insertOne(document);
                    } catch (MongoException | InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                });
            } catch (StatusRuntimeException e) {
                stopsig = true;
                e.printStackTrace();
            }

            if (stopsig) {
                break;
            }

        }
    }
}
