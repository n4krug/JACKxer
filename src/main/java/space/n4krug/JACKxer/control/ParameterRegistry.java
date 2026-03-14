package space.n4krug.JACKxer.control;

import java.util.HashMap;
import java.util.Map;

public class ParameterRegistry {

    private final Map<String, ControlParameter<?>> params = new HashMap<>();

    public void register(String id, ControlParameter param) {
        params.put(id, param);
    }

    public ControlParameter get(String id) {
        return params.get(id);
    }

    /**
     * Returns a shallow copy of the current registry map for safe iteration.
     */
    public Map<String, ControlParameter<?>> snapshot() {
        return new HashMap<>(params);
    }
}
