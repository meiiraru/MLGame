package mlgame.brain;

import cinnamon.utils.IOUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Replay {

    private long seed;
    private final List<Boolean> actions = new ArrayList<>();

    public Replay(long seed) {
        this.seed = seed;
    }

    public void recordAction(boolean jumped) {
        actions.add(jumped);
    }

    public void clear() {
        actions.clear();
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    public List<Boolean> getActions() {
        return actions;
    }

    public String serialize() {
        //simple serialization format: "seed;010001010"
        StringBuilder sb = new StringBuilder();
        sb.append(seed).append(";");
        for (Boolean action : actions)
            sb.append(action ? "1" : "0");
        return sb.toString();
    }

    public static Replay deserialize(String data) {
        String[] parts = data.split(";");
        Replay replay = new Replay(Long.parseLong(parts[0]));

        if (parts.length > 1) {
            for (char c : parts[1].toCharArray())
                replay.actions.add(c == '1');
        }
        return replay;
    }

    public void save(String filename) {
        Path path = IOUtils.ROOT_FOLDER.resolve(filename + ".replay");
        IOUtils.createOrGetPath(path);
        IOUtils.writeFileCompressed(path, serialize().getBytes());
    }

    public static Replay load(String filename) {
        Path path = IOUtils.ROOT_FOLDER.resolve(filename + ".replay");
        byte[] bytes = IOUtils.readFileCompressed(path);
        return bytes == null ? null : deserialize(new String(bytes));
    }
}