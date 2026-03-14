package space.n4krug.JACKxer.tools;

import java.util.*;
import java.util.Map.Entry;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;

public class ConfigParser {

	/**
	 * Parses {@code var} or {@code counter} entries from a config section.
	 * <p>
	 * Expected syntax: {@code "<prefix> <name> = <int>"}.
	 */
	public static Map<String, Integer> parseParams(String prefix, List<String> lines) {
		Map<String, Integer> map = new HashMap<>();

		for (String line : lines) {
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			int splitIndex = line.indexOf(' ');
			if (splitIndex <= 0) {
				continue;
			}

			String type = line.substring(0, splitIndex);
			if (!type.equals(prefix)) {
				continue;
			}

			String[] keyVal = line.substring(splitIndex + 1).split(" = ");

			if (keyVal.length <= 1) {
				continue;
			}

			map.put(keyVal[0], Integer.parseInt(keyVal[1]));
		}

		return map;
	}

	public static List<StringPair> parseKeyword(String prefix, Map<String, Integer> vars, Map<String, Integer> counters,
			List<String> lines) throws EvaluationException, ParseException {
		List<StringPair> parsed = new ArrayList<>();

		for (String line : lines) {
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			int splitIndex = line.indexOf(' ');
			if (splitIndex <= 0) {
				continue;
			}

			String type = line.substring(0, splitIndex);
			if (!type.equals(prefix)) {
				continue;
			}

			String val = line.substring(splitIndex);

			List<String> formattedLine = format(val, vars, counters, new HashMap<>());

			for (String formLine : formattedLine) {
				String[] split = formLine.split(" = ");
				parsed.add(new StringPair(split[0].strip(), split[1].strip()));
			}
		}

		return parsed;
	}

	public static List<String> format(String input, Map<String, Integer> vars, Map<String, Integer> counters,
			HashMap<String, Integer> context) throws EvaluationException, ParseException {

		int start = input.indexOf('{');

		if (start == -1) {
			return List.of(input);
		}

		int end = input.indexOf('}', start);

		String before = input.substring(0, start);
		String expr = input.substring(start + 1, end);
		String after = input.substring(end + 1);

		List<String> results = new ArrayList<>();

		Expression e = new Expression(expr);

		// apply constants
		for (var v : vars.entrySet()) {
			e = e.and(v.getKey(), v.getValue());
		}

		// apply already bound counters
		for (Entry<String, Integer> c : context.entrySet()) {
			e = e.and(c.getKey(), c.getValue());
		}

		boolean expanded = false;

		for (Entry<String, Integer> counter : counters.entrySet()) {

			String name = counter.getKey();

			if (expr.contains(name) && !context.containsKey(name)) {

				expanded = true;

				int count = counter.getValue();

				for (int i = 0; i < count; i++) {

					HashMap<String, Integer> nextContext = new HashMap<>(context);

					nextContext.put(name, i);

					List<String> sub = format(input, vars, counters, nextContext);

					results.addAll(sub);
				}

				break;
			}
		}

		if (!expanded) {

			Expression eval = new Expression(expr);

			for (Entry<String, Integer> v : vars.entrySet()) {
				eval = eval.and(v.getKey(), v.getValue());
			}

			for (Entry<String, Integer> c : context.entrySet()) {
				eval = eval.and(c.getKey(), c.getValue());
			}

			String value = eval.evaluate().getNumberValue().toString();

			String replaced = before + value + after;

			results.addAll(format(replaced, vars, counters, context));
		}

		return results;
	}

	public static List<String> getChainNodes(Map<String, String> nodes, String chainName, Comparator<String> order) {
		List<String> chainNodes = new ArrayList<>();
		for (Entry<String, String> node : nodes.entrySet()) {
			if (node.getKey().startsWith(chainName)) {
				chainNodes.add(node.getKey());
			}
		}
		chainNodes.sort(order);
		return chainNodes;
	}

	//public static List<String> getChainNodes(Map<String, String> nodes, String chainName) {
	//	return getChainNodes(nodes, chainName, Comparator.naturalOrder());
	//}

	public static SortedMap<String, List<String>> splitPages(List<String> lines) throws EvaluationException, ParseException {
		List<String> globalLines = new ArrayList<>();
		for (String line : lines) {
			if (line.startsWith("[")) {
				break;
			}
			globalLines.add(line);
		}
		Map<String, Integer> globCounters = parseParams("counter", globalLines);
		Map<String, Integer> globVars = parseParams("var", globalLines);
		SortedMap<String, List<String>> out = new TreeMap<>();

		List<String> pageNames = new ArrayList<>();
		pageNames.add("global");
		out.put("global", new ArrayList<>());
		for (String line : lines) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			if (line.startsWith("[")) {
				String unParsedPageName = line.split("\\[")[1];
				unParsedPageName = unParsedPageName.split("]")[0];
				unParsedPageName = unParsedPageName.strip();

				pageNames = format(unParsedPageName, globVars, globCounters, new HashMap<>());
				for (String pageName : pageNames) {
					out.put(pageName, new ArrayList<>());
				}
				continue;
			}
			for (String pageName : pageNames) {
				out.get(pageName).add(line);
			}
		}

		return out;
	}
}
