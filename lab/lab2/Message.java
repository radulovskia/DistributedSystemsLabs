package lab2;

import java.io.Serializable;
import java.util.ArrayList;

import com.google.gson.Gson;

public class Message implements Serializable{
    String sender;
    String receiver;
    String messageText;
    ArrayList<String> fileContents;

    public Message(){
    }

    public Message(String sender, String receiver, String messageText){
        this.sender = sender;
        this.receiver = receiver;
        this.messageText = messageText;
    }

    public Message(String sender, String receiver, String messageText, ArrayList<String> fileContents){
        this.sender = sender;
        this.receiver = receiver;
        this.messageText = messageText;
        this.fileContents = fileContents;
    }

    @Override
    public String toString(){
        return "\"" + messageText + "\" - from " + sender + " to " + receiver;
    }

//    for JSON serialization
    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Message fromJson(String json){
        Gson gson = new Gson();
        return gson.fromJson(json, Message.class);
    }
}
