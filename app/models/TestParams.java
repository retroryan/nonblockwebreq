package models;

public class TestParams {

    public int pauseDuration;
    public int memoryFillSize;

    public String host;

    public TestParams() {
    }

    public TestParams(int pauseDuration, int memoryFillSize, String host) {
        this.pauseDuration = pauseDuration;
        this.memoryFillSize = memoryFillSize;
        this.host = host;
    }
}
