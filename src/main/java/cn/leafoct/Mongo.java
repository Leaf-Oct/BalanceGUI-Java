package cn.leafoct;

import com.mongodb.client.model.Filters;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class Mongo {
    private MongoClient client;
    public MongoDatabase db;
    public String address, user, password, login_db;

    public boolean connect() {
        var url = "mongodb://" + user + ":" + password + "@" + address + "/" + login_db;
        try {
            client = MongoClients.create(url);
            db = client.getDatabase(login_db);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String insert(String collection_name, Document transaction) throws Exception{
        var collection = db.getCollection(collection_name);
        var result = collection.insertOne(transaction);
        return result.getInsertedId().toString();
    }

    public void close(){
        if (client!=null){
            client.close();
        }
    }
}
