package space.n4krug.JACKxer.tools;

import junit.framework.TestCase;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ParameterStateStoreTest extends TestCase {

    public void testRoundTripPersistsEqAndCompButNotGainOrOn() throws IOException {
        Path baseDir = Files.createTempDirectory("jackxer-state-test");
        String cfg = "main.cfg";

        ParameterRegistry before = new ParameterRegistry();
        before.register("Out 0.Ch0.1.compressor.threshold", ControlParameter.range(-60, 0, -12));
        before.register("Out 0.Ch0.1.compressor.ratio", ControlParameter.range(1, 10, 3));
        before.register("Out 0.Ch0.1.compressor.attack", ControlParameter.range(0.001f, 0.2f, 0.05f));
        before.register("Out 0.Ch0.1.compressor.release", ControlParameter.range(0.01f, 1f, 0.4f));
        before.register("Out 0.Ch0.1.compressor.makeup", ControlParameter.range(-6, 24, 6));
        before.register("Out 0.Ch0.1.compressor.bypass", ControlParameter.toggle(true));

        before.register("Out 0.Ch0.2.eq.band0.freq", new ControlParameter<>(v -> v, 0.25f));
        before.register("Out 0.Ch0.2.eq.band0.gain", new ControlParameter<>(v -> v, 0.5f));
        before.register("Out 0.Ch0.2.eq.band0.q", new ControlParameter<>(v -> v, 0.75f));
        before.register("Out 0.Ch0.2.eq.band0.type", new ControlParameter<>(v -> v, 0.125f));
        before.register("Out 0.Ch0.2.eq.bypass", ControlParameter.toggle(false));

        // These must NOT be persisted.
        before.register("Out 0.Ch0.3.gain.gain", ControlParameter.range(-60, 6, -3));
        before.register("Out 0.Ch0.3.gain.on", ControlParameter.toggle(false));

        ParameterStateStore.save(baseDir, cfg, before);

        ParameterRegistry after = new ParameterRegistry();
        ControlParameter<Float> threshold = ControlParameter.range(-60, 0, -60);
        ControlParameter<Boolean> compBypass = ControlParameter.toggle(false);
        ControlParameter<Float> eqFreq = new ControlParameter<>(v -> v, 0f);
        ControlParameter<Float> eqGain = new ControlParameter<>(v -> v, 0f);
        ControlParameter<Boolean> eqBypass = ControlParameter.toggle(true);
        ControlParameter<Float> gain = ControlParameter.range(-60, 6, -60);
        ControlParameter<Boolean> on = ControlParameter.toggle(true);

        after.register("Out 0.Ch0.1.compressor.threshold", threshold);
        after.register("Out 0.Ch0.1.compressor.bypass", compBypass);
        after.register("Out 0.Ch0.2.eq.band0.freq", eqFreq);
        after.register("Out 0.Ch0.2.eq.band0.gain", eqGain);
        after.register("Out 0.Ch0.2.eq.bypass", eqBypass);
        after.register("Out 0.Ch0.3.gain.gain", gain);
        after.register("Out 0.Ch0.3.gain.on", on);

        ParameterStateStore.loadAndApply(baseDir, cfg, after);

        assertTrue("compressor threshold should be applied", threshold.getNormalized() > 0f);
        assertTrue("compressor bypass should be applied", compBypass.getValue().booleanValue());

        assertEquals("eq freq should be applied", 0.25f, eqFreq.getNormalized(), 1e-6f);
        assertEquals("eq band gain should be applied", 0.5f, eqGain.getNormalized(), 1e-6f);
        assertFalse("eq bypass should be applied", eqBypass.getValue().booleanValue());

        // gain/on must remain at their defaults in the new registry
        assertEquals("gain must not be restored", 0f, gain.getNormalized(), 1e-6f);
        assertTrue("on must not be restored", on.getValue().booleanValue());
    }

    public void testBypassSavedOnlyForPersistableClients() throws IOException {
        Path baseDir = Files.createTempDirectory("jackxer-state-test");
        String cfg = "main.cfg";

        ParameterRegistry before = new ParameterRegistry();
        // A persistable compressor knob implies compressor bypass can be saved.
        before.register("A.compressor.threshold", new ControlParameter<>(v -> v, 0.7f));
        before.register("A.compressor.bypass", ControlParameter.toggle(true));

        // No EQ/comp knobs for this client; its bypass must not be saved/restored.
        before.register("B.other.bypass", ControlParameter.toggle(true));

        ParameterStateStore.save(baseDir, cfg, before);

        ParameterRegistry after = new ParameterRegistry();
        ControlParameter<Boolean> aBypass = ControlParameter.toggle(false);
        ControlParameter<Boolean> bBypass = ControlParameter.toggle(false);
        after.register("A.compressor.threshold", new ControlParameter<>(v -> v, 0f));
        after.register("A.compressor.bypass", aBypass);
        after.register("B.other.bypass", bBypass);

        ParameterStateStore.loadAndApply(baseDir, cfg, after);

        assertTrue(aBypass.getValue().booleanValue());
        assertFalse(bBypass.getValue().booleanValue());
    }

    public void testMissingFileIsNoOp() throws IOException {
        Path baseDir = Files.createTempDirectory("jackxer-state-test");
        String cfg = "main.cfg";

        ParameterRegistry params = new ParameterRegistry();
        ControlParameter<Float> p = new ControlParameter<>(v -> v, 0.1f);
        params.register("X.compressor.threshold", p);

        // No file written; load should not throw and should not change anything.
        ParameterStateStore.loadAndApply(baseDir, cfg, params);
        assertEquals(0.1f, p.getNormalized(), 1e-6f);
    }

    public void testCorruptValuesAreSkipped() throws IOException {
        Path baseDir = Files.createTempDirectory("jackxer-state-test");
        String cfg = "main.cfg";

        ParameterRegistry before = new ParameterRegistry();
        before.register("X.compressor.threshold", new ControlParameter<>(v -> v, 0.2f));
        ParameterStateStore.save(baseDir, cfg, before);

        Path path = baseDir.resolve("JACKxer").resolve("state-main.cfg.properties");
        String content = Files.readString(path);
        // Inject a corrupt value for a persisted key.
        Files.writeString(path, content + "\nX.compressor.threshold=not-a-float\n");

        ParameterRegistry after = new ParameterRegistry();
        ControlParameter<Float> p = new ControlParameter<>(v -> v, 0.9f);
        after.register("X.compressor.threshold", p);

        ParameterStateStore.loadAndApply(baseDir, cfg, after);
        // Corrupt value should be ignored, leaving default.
        assertEquals(0.9f, p.getNormalized(), 1e-6f);
    }
}
