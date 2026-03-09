package space.n4krug.JACKxer.control;

import java.util.HashMap;

public class ParameterRegistry {

    private HashMap<String, ControlParameter> params =
        new HashMap<>();

    public void register(String id, ControlParameter param) {
        params.put(id, param);
    }

    public ControlParameter get(String id) {
        return params.get(id);
    }
}
