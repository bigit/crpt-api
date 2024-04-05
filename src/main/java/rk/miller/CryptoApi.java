package rk.miller;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CryptoApi {

    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/document/create";

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final ScheduledExecutorService requestCounter = Executors.newSingleThreadScheduledExecutor();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting().create();
    private final int requestLimit;

    public CryptoApi(TimeUnit timeUnit, int limit) {
        this.requestLimit = limit;
        startRequestCounter(timeUnit);
    }

    public void createDocument(Document document, String signature) {
        String json = gson.toJson(document);
        sendRequest(json);
    }

    private void sendRequest(String json) {
        try {
            lock.lock();
            if (requestCount.get() >= requestLimit) {
                condition.await();
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(new URI(URL))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());

            requestCount.incrementAndGet();
            lock.unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRequestCounter(TimeUnit timeUnit) {
        requestCounter.scheduleWithFixedDelay(
                () -> {
                    lock.lock();
                    this.requestCount.set(0);
                    condition.signalAll();
                    lock.unlock();
                }, 0, 1, timeUnit);
    }
}

@Data
class Document {
    private Description description = new Description();
    private String docId = "string";
    private String docStatus = "string";
    private String docType = "LP_INTRODUCE_GOODS";
    private boolean importRequest = true;
    private String ownerInn = "string";
    private String participantInn = "string";
    private String producerInn = "string";
    private Date productionDate = new Date();
    private String productionType = "string";
    private ArrayList<Product> products;
    private Date regDate = new Date();
    private String regNumber = "string";
}

@Data
class Description {
    private String participantInn = "string";
}

@Data
class Product {
    private String certificateDocument = "string";
    private Date certificateDocumentDate = new Date();
    private String certificateDocumentNumber = "string";
    private String ownerInn = "string";
    private String producerInn = "string";
    private Date productionDate = new Date();
    private String tnvedCode = "string";
    private String uitCode = "string";
    private String uituCode;
}