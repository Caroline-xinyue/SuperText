/**
 * Created by Xinyue on 9/4/15.
 * The code is built based on the tutorial found at https://www.twilio.com/blog/2015/09/getting-started-with-gradle-and-the-spark-framework-3.html
 */
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.verbs.*;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

//watson language translation
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;

public class HelloSpark {
    public static void main(String[] args) {
        /*get("/", (req, res) -> {
            String name = req.queryParams("name");
            return "Hello " + name;*/
        //get("/", (req, res) -> "Hello, World!");

        TwilioRestClient client = new TwilioRestClient("AC337f1c5c266c5eeb86d8cfd6c8bf8eef", "fa33c9037bcc66b0099b53e2741de382"); // replace this
        Account mainAccount = client.getAccount();

        post("/sms", (req, res) -> {
            String ngrokUrl = "http://dbde8329.ngrok.io"; // replace this
            String body = req.queryParams("Body");
            String to = req.queryParams("From");
            String from = "(610) 365-4879"; // replace this

            String uri = null;
            CallFactory callFactory = null;
            Map<String, String> callParams = new HashMap<>();

            TwiMLResponse twiml = new TwiMLResponse();
            String[] tokens = body.split(" ");
            String action = null;
            if (tokens.length >= 1) {
                action = tokens[0];
                switch (action.toLowerCase()) {
                    case "play":
                        uri = ngrokUrl + "/call?q=" + URLEncoder.encode(body.substring(5), "UTF-8");
                        callFactory = mainAccount.getCallFactory();
                        callParams = new HashMap<>();
                        callParams.put("To", to);
                        callParams.put("From", from);
                        callParams.put("Url", uri);
                        callParams.put("Method", "GET");
                        callFactory.create(callParams);

                        twiml.append(new Message("Your tune is on the way!"));
                        res.type("text/xml");
                        return twiml.toXML();
                    case "translate":
                        if (tokens.length < 1) {
                            // return error message
                            return twiml.toXML();
                        }
                        String text = body.substring((action + " ").length());
                        System.out.println("translating " + text);

                        LanguageTranslation service2 = new LanguageTranslation();
                        service2.setUsernameAndPassword("17d780c4-46c1-4bd9-8bd9-059dc1f60913", "YdZgn3UctViZ");

                        TranslationResult translationResult = service2.translate(text, "en", "es");
                        String translation = translationResult.getTranslations().get(0).getTranslation();
                        System.out.println(translation);
                        twiml.append(new Message(translation));
                        res.type("text/xml");
                        return twiml.toXML();
                    default:
                        return twiml.toXML();
                }
            }
            return twiml.toXML();
        });

        get("/call", (req, res) -> {
            TwiMLResponse twiml = new TwiMLResponse();

            String query = req.queryParams("q");
            String trackUrl = getTrackUrl(query);

            if (trackUrl != null) {
                twiml.append(new Play(trackUrl));
            } else {
                twiml.append(new Say("Sorry, song not found."));
            }

            res.type("text/xml");
            return twiml.toXML();
        });
}

    private static String getTrackUrl(String query) {
        String url = "http://api.spotify.com/v1/search";
        HttpResponse<JsonNode> jsonResponse;
        try {
            jsonResponse = Unirest.get(url)
                    .header("accept", "application/json")
                    .queryString("q", query)
                    .queryString("type", "track")
                    .asJson();
            return jsonResponse.getBody().getObject().getJSONObject("tracks")
                    .getJSONArray("items").getJSONObject(0).getString("preview_url");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
