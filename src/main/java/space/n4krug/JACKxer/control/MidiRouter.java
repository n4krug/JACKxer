package space.n4krug.JACKxer.control;

import java.util.HashMap;

public class MidiRouter {
    private record CCKey(String controller, int channel, int cc) {}
	private final HashMap<CCKey, ControlParameter> ccMap = new HashMap<>();

	public void mapCC(String controller, int channel, int cc, ControlParameter param) {
		ccMap.put(new CCKey(controller, channel, cc), param);
	}

	public void handleCC(String controller, int channel, int cc, float normalized) {

		ControlParameter p = ccMap.get(new CCKey(controller, channel, cc));

		if (p != null) {
			p.setNormalized(normalized);
		}
	}
}