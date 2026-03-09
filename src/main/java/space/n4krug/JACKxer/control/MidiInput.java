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

			if (sm.getCommand() == ShortMessage.CONTROL_CHANGE) {

				int channel = sm.getChannel();
				int cc = sm.getData1();
				int value = sm.getData2();
				
				router.handleCC(device, channel, cc, value / 127f);
			}
		}
	}

	@Override
	public void close() {
	}
}