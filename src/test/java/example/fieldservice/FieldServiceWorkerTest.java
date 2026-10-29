package example.fieldservice;

public final class FieldServiceWorkerTest {
    public static void main(String[] args) {
        FieldServiceWorker worker = new FieldServiceWorker();
        WorkOrder complete = new WorkOrder("WO-1", "photo.jpg", "Installed and tested", "DISPATCHED");
        WorkOrder missingPhoto = new WorkOrder("WO-2", "", "Installed and tested", "DISPATCHED");
        if (!"READY_FOR_REVIEW".equals(worker.nextDecision(complete))) throw new AssertionError();
        if (!"FOLLOW_UP_REQUIRED".equals(worker.nextDecision(missingPhoto))) throw new AssertionError();
        System.out.println("field-service decision test passed");
    }
}
