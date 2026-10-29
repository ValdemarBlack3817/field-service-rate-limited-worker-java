package example.fieldservice;

public record WorkerConfig(String apiKey, String queue, int maxMessages, int visibilityTimeoutSeconds) {
    public static WorkerConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before starting the worker");
        }
        return new WorkerConfig(key, "field-service-jobs", 4, 60);
    }
}
