package space.n4krug.JACKxer.control;

import java.util.HashMap;

public class MidiRouter {

	private record Key(String controller, String id) {}

	private final HashMap<Key, ControlParameter<?>> map = new HashMap<>();

	public void map(String controller, String id, ControlParameter<?> param) {
		map.put(new Key(controller, id), param);
	}

	public void handle(ControlEvent e) {

		ControlParameter<?> p = map.get(new Key(e.controller, e.id));

		System.out.println(p);

		if (p == null) return;

		if (e.type == ControlEvent.Type.ABSOLUTE) {

			p.setNormalized(e.value);

		} else if (e.type == ControlEvent.Type.RELATIVE) {

			float newVal = p.getNormalized() + e.value * 0.01f;
			p.setNormalized(newVal);

		} else if (e.type == ControlEvent.Type.BUTTON) {

			p.setNormalized(e.value);

		}
	}
}