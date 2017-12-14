import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Handler implements RequestStreamHandler {

    private static final String BUCKET = "ktka-learn-tracker";
    private static final String KEY = "-headlines.txt";

    public void handleRequest(final InputStream inputStream, final OutputStream outputStream, final Context context) {
        handleRequest();
    }

    public String handleRequest() {
        try {
            System.out.println("Attempting to get golem.de");
            final Document document = Jsoup.connect("http://www.golem.de").get();
            System.out.println("Successfully acquired golem.de");
            final Elements headlines = document.select(".head2");
            final String data = toString(headlines);
            System.out.println("Data parsed: " + data);

            final AmazonS3 amazonS3 = buildS3Client();
            System.out.println("Attempting to upload data");
            tryUploadFile(BUCKET, currentTime(), amazonS3, data);

        } catch (IOException e) {
            System.out.println("An exception occurred: " + e.getMessage());
            e.printStackTrace();
            return "FAIL";
        }
        return "OK";
    }

    private String currentTime() {
        final Instant now = Instant.now();
        final int year = now.get(DateTimeFieldType.year());
        final int month = now.get(DateTimeFieldType.monthOfYear());
        final int day = now.get(DateTimeFieldType.dayOfMonth());
        final int hours = now.get(DateTimeFieldType.clockhourOfDay());
        final int minutes = now.get(DateTimeFieldType.minuteOfHour());
        final int seconds = now.get(DateTimeFieldType.secondOfMinute());
        return year + "-" + month + "-" + day + "---" + hours + "-" + minutes + "-" + seconds + KEY;
    }

    private AmazonS3 buildS3Client() {
        return AmazonS3Client.builder().build();
    }

    private String toString(final Elements headlines) {
        StringBuffer result = new StringBuffer();
        for (Element headline : headlines) {
            result.append("\n" + headline.text());
        }
        return result.toString();
    }

    private void tryUploadFile(final String dstBucket, final String dstKey, final AmazonS3 client, String content) {
        try {
            client.putObject(dstBucket, dstKey, content);
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " + "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" + " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " + "means the client encountered " +
                    "an internal error while trying to " + "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
            System.out.println("Caught some other unexpected exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
