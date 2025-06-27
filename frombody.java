import com.fasterxml.jackson.databind.ObjectMapper;

public class MftFile {
    private String name;
    private long size;
    private String status;

    // getters / setters

    public static MftFile fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, MftFile.class);
        } catch (Exception e) {
            throw new RuntimeException("Échec de parsing JSON MftFile", e);
        }
    }
}