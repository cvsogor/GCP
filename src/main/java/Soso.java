import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
        import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
        import com.google.api.client.http.HttpTransport;
        import com.google.api.client.http.InputStreamContent;
        import com.google.api.client.json.JsonFactory;
        import com.google.api.client.json.jackson2.JacksonFactory;
        import com.google.api.services.storage.Storage;
        import com.google.api.services.storage.StorageScopes;
        import com.google.api.services.storage.model.Bucket;
        import com.google.api.services.storage.model.ObjectAccessControl;
        import com.google.api.services.storage.model.Objects;
        import com.google.api.services.storage.model.StorageObject;

        import java.io.ByteArrayInputStream;
        import java.io.File;
        import java.io.IOException;
        import java.io.InputStream;
        import java.security.GeneralSecurityException;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.List;

class StorageSample {

    private static final String APPLICATION_NAME = "[[INSERT_YOUR_APP_NAME_HERE]]";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TEST_FILENAME = "json-test.txt";
    private static Storage storageService;

    private static Storage getService() throws IOException, GeneralSecurityException
    {
        if (null == storageService)
        {
            GoogleCredential credential = GoogleCredential.getApplicationDefault();
            if (credential.createScopedRequired()) {
                credential = credential.createScoped(StorageScopes.all());
            }
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            storageService = new Storage.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME).build();
        }
        return storageService;
    }
    public static List<StorageObject> listBucket(String bucketName) throws IOException, GeneralSecurityException
    {
        Storage client = getService();
        Storage.Objects.List listRequest = client.objects().list(bucketName);

        List<StorageObject> results = new ArrayList<StorageObject>();
        Objects objects;

        do
        {
            objects = listRequest.execute();
            results.addAll(objects.getItems());

            listRequest.setPageToken(objects.getNextPageToken());
        }
        while (null != objects.getNextPageToken());
        return results;
    }

    public static Bucket getBucket(String bucketName) throws IOException, GeneralSecurityException {
        Storage client = getService();

        Storage.Buckets.Get bucketRequest = client.buckets().get(bucketName);
        // Fetch the full set of the bucket's properties (e.g. include the ACLs in the response)
        bucketRequest.setProjection("full");
        return bucketRequest.execute();
    }
    public static void uploadStream(String name, String contentType, InputStream stream, String bucketName) throws IOException, GeneralSecurityException
    {
        InputStreamContent contentStream = new InputStreamContent(contentType, stream);
        StorageObject objectMetadata = new StorageObject()
                // Set the destination object name
                .setName(name)
                // Set the access control list to publicly read-only
                .setAcl(Arrays.asList(
                        new ObjectAccessControl().setEntity("allUsers").setRole("READER")));

        // Do the insert
        Storage client = getService();
        Storage.Objects.Insert insertRequest = client.objects().insert(
                bucketName, objectMetadata, contentStream);

        insertRequest.execute();
    }
    public static void deleteObject(String path, String bucketName) throws IOException, GeneralSecurityException
    {
        Storage client = getService();
        client.objects().delete(bucketName, path).execute();
    }
    public static void main(String[] args)
    {

        String bucketName = "heyzap";

        try {
            // Get metadata about the specified bucket.
            Bucket bucket = getBucket(bucketName);
            System.out.println("name: " + bucketName);
            System.out.println("location: " + bucket.getLocation());
            System.out.println("timeCreated: " + bucket.getTimeCreated());
            System.out.println("owner: " + bucket.getOwner());


            // List the contents of the bucket.
            List<StorageObject> bucketContents = listBucket(bucketName);
            if (null == bucketContents) {
                System.out.println(
                        "There were no objects in the given bucket; try adding some and re-running.");
            }
            for (StorageObject object : bucketContents) {
                System.out.println(object.getName() + " (" + object.getSize() + " bytes)");
            }


            // Upload a stream to the bucket. This could very well be a file.
            uploadStream(
                    TEST_FILENAME, "text/plain",
                    new ByteArrayInputStream("Test of json storage sample".getBytes()),
                    bucketName);

            // Now delete the file
            deleteObject(TEST_FILENAME, bucketName);

        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}