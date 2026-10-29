package example.fieldservice;

public record WorkOrder(String workOrderId, String photoUrl, String technicianNote, String dispatchStatus) {
    public String toJson() {
        return "{\"workOrderId\":\"" + escape(workOrderId) + "\",\"photoUrl\":\""
                + escape(photoUrl) + "\",\"technicianNote\":\"" + escape(technicianNote)
                + "\",\"dispatchStatus\":\"" + escape(dispatchStatus) + "\"}";
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
