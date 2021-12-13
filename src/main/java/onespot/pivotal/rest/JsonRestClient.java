package onespot.pivotal.rest;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mashape.unirest.http.HttpResponse;
import onespot.pivotal.api.ex.PivotalAPIException;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Created by ian on 3/27/15.
 */
public class JsonRestClient {
    private static final String NULL_DATE = "0000-00-00T00:00:00Z";
    private final RestClient restClient;
    private final Gson gson;

    public JsonRestClient(RestClient restClient) {
        this.restClient = restClient;
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .registerTypeAdapter(JsonObject.class, new JsonObjectTypeAdapter())
                .create();
    }

    public <T> T get(Class<T> cls, String path, Multimap<String, String> params) throws PivotalAPIException {
        HttpResponse<String> response = httpResponse(path, params);
        String body = extractBody(response);
        return gson.fromJson(body, cls);
    }

    public <T> T get(Type cls, String path, Multimap<String, String> params) throws PivotalAPIException {
        HttpResponse<String> response = httpResponse(path, params);
        try {
            String body = extractBody(response);
            return gson.fromJson(body, cls);
        } catch (RuntimeException e) {
            throw new PivotalAPIException("Failed to GET " + path, e);
        }
    }

    public <T> T put(Type cls, String path, Multimap<String, String> params, T payload) throws PivotalAPIException {
        return gson.fromJson(extractBody(restClient.put(path, params, gson.toJson(payload))), cls);
    }

    public <T> T put(Class<T> cls, String path, Multimap<String, String> params, T payload) throws PivotalAPIException {
        return gson.fromJson(extractBody(restClient.put(path, params, gson.toJson(payload))), cls);
    }

    public <T> T post(Class<T> cls, String path, Multimap<String, String> params, T payload) throws PivotalAPIException {
        String payloadJson = gson.toJson(payload);
        return gson.fromJson(extractBody(restClient.post(path, params, payloadJson)), cls);
    }

    public <T> T post(Type cls, String path, Multimap<String, String> params, T payload) throws PivotalAPIException {
        return gson.fromJson(extractBody(restClient.post(path, params, gson.toJson(payload))), cls);
    }

    public <T> T delete(Class<T> cls, String path, Multimap<String, String> params) throws PivotalAPIException {
        return gson.fromJson(extractBody(restClient.delete(path, params)), cls);
    }

    public <T> T delete(Type cls, String path, Multimap<String, String> params) throws PivotalAPIException {
        return gson.fromJson(extractBody(restClient.delete(path, params)), cls);
    }

    private String extractBody(HttpResponse<String> response) {
        if (response.getStatus() != 200) {
            throw new PivotalAPIException("Request failed, status: " + response.getStatus() + ", text: " + response.getStatusText() + " body: " + response.getBody());
        }
        return response.getBody();
    }

    private HttpResponse<String> httpResponse(String path) {
        return httpResponse(path, HashMultimap.create());
    }

    private HttpResponse<String> httpResponse(String path, Multimap<String, String> params) throws PivotalAPIException {
        return restClient.get(path, params);
    }

    private static class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (NULL_DATE.equals(json.getAsString())) {
                return null;
            }
            return Instant.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (NULL_DATE.equals(json.getAsString())) {
                return null;
            }
            return LocalDate.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class JsonObjectTypeAdapter implements JsonSerializer<JsonObject>, JsonDeserializer<JsonObject> {

        @Override
        public JsonObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return (JsonObject) json;
        }

        @Override
        public JsonElement serialize(JsonObject src, Type typeOfSrc, JsonSerializationContext context) {
            return src;
        }
    }
}
