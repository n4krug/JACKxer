package space.n4krug.JACKxer.control;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class MidiInput implements Receiver {

	private final String device;
	private final MidiRouter router;

	public MidiInput(MidiRouter router, String device) {
		this.router = router;
		this.device = device;
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {

		if (message instanceof ShortMessage sm) {
			System.out.println(sm.getChannel() + " : " + sm.getData1() + " : " + sm.getData2());

			if (sm.getCommand() == ShortMessage.CONTROL_CHANGE) {

				String id = "ch" + sm.getChannel() + ".cc" + sm.getData1();

				float value = sm.getData2() / 127f;

				System.out.println("Sending: " + id + ", " + value);

				router.handle(new ControlEvent(
						device,
						id,
						ControlEvent.Type.ABSOLUTE,
						value
				));
			}
		//	if (sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {

		//		int program = sm.getChannel();
		//		int value = sm.getData1();

		//		router.handleProgram(device, program, value);
		//	}
		}
	}

	@Override
	public void close() {
	}
}