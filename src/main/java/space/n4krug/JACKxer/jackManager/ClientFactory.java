package space.n4krug.JACKxer.jackManager;

import java.util.HashMap;
import java.util.function.Function;

import org.jaudiolibs.jnajack.Jack;
import space.n4krug.JACKxer.tools.NodeSpec;

public class ClientFactory {

	private static final HashMap<String, Function<NodeSpec, Client>> factories = new HashMap<>();

	static {
		factories.put("gain", node -> {
			try {
				return new Gain(node.name, node.registry);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		factories.put("compressor", node -> {
			try {
				return new Compressor(node.name, node.registry);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		factories.put("noise", node -> {
			try {
				NoiseGen.Type t = NoiseGen.Type.valueOf(node.args[0].toUpperCase());
				return new NoiseGen(node.name, t, node.registry);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		factories.put("wav", node -> {
            try {
                return new WavPlayer(node.name, node.args[0], node.registry);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
		
		factories.put("stereoToMono", node -> {
			try {
				return new StereoToMono(node.name, node.registry);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		factories.put("eq", node -> {
			try {
				int bandCount = 4;

				if (node.args.length > 0) {
					bandCount = Integer.parseInt(node.args[0]);
				}

				Biquad.Type[] bands = new Biquad.Type[bandCount];

				for (int i = 0; i < bandCount; i++) {
					bands[i] = Biquad.Type.PEAK;
				}

				return new ParametricEQ(node.name, node.registry, bands);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

    public static Client create(NodeSpec spec) {

        Function<NodeSpec, Client> factory =
                factories.get(spec.type);

        if (factory == null) {
            throw new RuntimeException(
                    "Unknown node type: " + spec.type);
        }

        return factory.apply(spec);
    }

}