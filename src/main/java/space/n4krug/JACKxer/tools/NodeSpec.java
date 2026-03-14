package space.n4krug.JACKxer.tools;

import space.n4krug.JACKxer.control.ParameterRegistry;

public class NodeSpec {

	public final ParameterRegistry registry;
	public final String name;
    public final String type;
    public final String[] args;

    public NodeSpec(String name, String type, String[] args, ParameterRegistry registry) {
        this.name = name;
        this.type = type;
        this.args = args;
        this.registry = registry;
    }
    
	/**
	 * Parses a node specification of the form {@code "type(arg1, arg2, ...)"}.
	 */
	public static NodeSpec parseNode(String name, String spec, ParameterRegistry registry) {

		int start = spec.indexOf('(');
		int end = spec.lastIndexOf(')');

		String type = spec.substring(0, start).trim();

		String argString = spec.substring(start + 1, end).trim();

		String[] args;

		if (argString.isEmpty()) {
			args = new String[0];
		} else {
			args = argString.split(",");
			for (int i = 0; i < args.length; i++) {
				args[i] = args[i].trim();
			}
		}

		return new NodeSpec(name, type, args, registry);
	}
}
