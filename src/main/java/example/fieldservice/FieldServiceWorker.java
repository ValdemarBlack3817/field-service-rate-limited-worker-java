package example.fieldservice;

public final class FieldServiceWorker {
    public String nextDecision(WorkOrder order) {
        if (!"DISPATCHED".equals(order.dispatchStatus())) return "FOLLOW_UP_REQUIRED";
        if (order.photoUrl().isBlank() || order.technicianNote().isBlank()) return "FOLLOW_UP_REQUIRED";
        return "READY_FOR_REVIEW";
    }

    public static void main(String[] args) throws Exception {
        WorkerConfig config = WorkerConfig.fromEnvironment();
        InfraiQueueClient queue = new InfraiQueueClient(config);
        queue.createQueue();
        WorkOrder example = new WorkOrder("WO-1042", "https://example.test/photos/WO-1042.jpg",
                "Compressor replaced; customer walkthrough complete", "DISPATCHED");
        queue.publish(example);
        System.out.println(new FieldServiceWorker().nextDecision(example));
    }
}
