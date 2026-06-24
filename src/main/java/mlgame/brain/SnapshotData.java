package mlgame.brain;

public record SnapshotData(int generation, float fitness, boolean hasReplay) {

    public String serialize() {
        return generation + "," + fitness + "," + (hasReplay ? 1 : 0);
    }

    public static SnapshotData deserialize(String data) {
        String[] parts = data.split(",");
        int generation    = Integer.parseInt(parts[0]);
        float fitness     = Float.parseFloat(parts[1]);
        boolean hasReplay = Integer.parseInt(parts[2]) == 1;
        return new SnapshotData(generation, fitness, hasReplay);
    }
}
