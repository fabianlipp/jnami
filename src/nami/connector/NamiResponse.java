package nami.connector;

public class NamiResponse<DataT> {
    private boolean success;
    private DataT data;
    private int totalEntries;
    private String responseType;
    
    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }
    /**
     * @return the data
     */
    public DataT getData() {
        return data;
    }
    /**
     * @return the totalEntries
     */
    public int getTotalEntries() {
        return totalEntries;
    }
    
    
}
