package my.com.prasarana.rapidbus.mapitintegration.client;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MongoClientConfig {

    public MongoCollection<Document> createClient() {

        Properties mongoProps = new Properties();
        try (InputStream inputStream = new FileInputStream("./config/mongodb.properties")) {
            mongoProps.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MongoClient mongoClient = MongoClients.create(mongoProps.getProperty("server.url"));
            MongoDatabase mongoDatabase = mongoClient.getDatabase(mongoProps.getProperty("db.name"));
            return mongoDatabase.getCollection(mongoProps.getProperty("collection.name"));
        } catch (MongoException e) {
            e.printStackTrace();
            return null;
        }
    }
}
