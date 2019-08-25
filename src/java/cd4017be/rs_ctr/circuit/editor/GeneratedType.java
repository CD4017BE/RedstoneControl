package cd4017be.rs_ctr.circuit.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import org.objectweb.asm.Type;

import com.google.common.base.Predicates;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import cd4017be.rs_ctr.circuit.editor.GeneratedGate.IParameterizedGate;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.GateType;
import cd4017be.rscpl.editor.InstructionSet;
import cd4017be.rscpl.util.IOUtils;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import net.minecraft.util.ResourceLocation;
import static cd4017be.rscpl.util.IOUtils.nextValue;

/**
 * 
 * @author cd4017be
 *
 */
public class GeneratedType extends GateType {

	public final int inputs, links, end;
	public final LinkVar[] outputs;
	public final byte[] heights;
	public final GeneratedNode[] nodes;
	private final IGateProvider provider;

	public GeneratedType(String name, int width, int height, byte[] heights, int ins, int links, int end, LinkVar[] outs, GeneratedNode[] nodes, IGateProvider provider) {
		super(name, width, height);
		this.inputs = ins;
		this.links = links;
		this.outputs = outs;
		this.heights = heights;
		this.nodes = nodes;
		this.end = end;
		this.provider = provider;
	}

	@Override
	public Gate newGate(int index) {
		return provider.newGate(this, index);
	}

	@Override
	public int getInputHeight(int i) {
		return heights[i];
	}

	@Override
	public int getOutputHeight(int o) {
		return heights[o + inputs];
	}

	@Override
	public Type getOutType(int o) {
		return outputs[o].type;
	}

	@Override
	public boolean isInputTypeValid(int i, Type type) {
		for (GeneratedNode n : nodes)
			do
				for (LinkVar v : n.inputs)
					if (v.name == i && Dep.canConvert(type, v.type, false))
						return true;
			while((n = n.next) != null);
		for (LinkVar v : outputs)
			if (v.name == i && Dep.canConvert(type, v.type, false))
				return true;
		return false;
	}

	public Node getNode(GeneratedGate gate, int i) {
		if (i < inputs) 
			return gate.getInput(i).getNode();
		i -= inputs;
		Node node = gate.nodeCache[i];
		if (node != null) return node;
		if (i < links) node = gate.createLink(i);
		else {
			GeneratedNode n = nodes[i - links];
			while ((node = n.createNode(this, gate)) == null && (n = n.next) != null);
		}
		gate.nodeCache[i] = node;
		return node;
	}

	@Override
	public Node createNode(Gate gate, int o) {
		return getNode((GeneratedGate) gate, outputs[o].name);
	}


	public static GeneratedType read(String id, JsonReader jr, InstructionSet inset) throws IOException {
		try {
			jr.beginObject();
			int w = 3;
			String in = "", out = "", link = "", type = null;
			char end = 0;
			int i = -1;
			CharArrayList names = new CharArrayList();
			ArrayList<GeneratedNode> nodes = new ArrayList<>();
			while(jr.hasNext()) {
				switch(jr.nextName()) {
				case "type":
					type = jr.nextString();
					break;
				case "id":
					i = jr.nextInt();
					break;
				case "width":
					w = jr.nextInt();
					break;
				case "pins_in":
					in = jr.nextString();
					break;
				case "pins_out":
					out = jr.nextString();
					break;
				case "link":
					link = jr.nextString();
					break;
				case "end":
					end = jr.nextString().charAt(0);
					break;
				case "nodes":
					if (jr.peek() == JsonToken.BEGIN_OBJECT) {
						parseNode(jr, nodes, names);
						break;
					}
					jr.beginArray();
					while(jr.hasNext())
						parseNode(jr, nodes, names);
					jr.endArray();
					break;
				default:
					jr.skipValue();
				}
			}
			jr.endObject();
			ArrayList<LinkVar> ins = new ArrayList<>(), outs = new ArrayList<>();
			ByteArrayList heights = new ByteArrayList();
			int h = Math.max(parsePins(ins, in, heights), parsePins(outs, out, heights));
			int ni = ins.size() + link.length(), nn = nodes.size();
			for (LinkVar v : ins)
				if (!names.contains(v.name))
					names.add(v.name);
				else throw new IllegalStateException("duplicate supplier for variable '" + v.name + "' defined in input pins");
			for (char c : link.toCharArray())
				if (!names.contains(c))
					names.add(c);
				else throw new IllegalStateException("duplicate supplier for variable '" + c + "' defined in link");
			for (LinkVar v : outs) v.name = (char) lookup(names, v.name, nn, ni);
			for (GeneratedNode n : nodes) n.translateVars(names, nn, ni);
			if (end != 0) end = (char)(lookup(names, end, nn, ni) + 1);
			IGateProvider provider = IGateProvider.REGISTRY.getOrDefault(type, IGateProvider.DEFAULT);
			GeneratedType t = new GeneratedType(id, w, h, heights.toByteArray(), ins.size(), link.length(), end - 1, outs.toArray(new LinkVar[outs.size()]), nodes.toArray(new GeneratedNode[nn]), provider);
			if (i >= 0) inset.add(i, t);
			return t;
		} catch(IllegalStateException | IllegalArgumentException e) {
			throw new IOException("invalid gate definition json:", e);
		}
	}

	static int lookup(CharArrayList names, char name, int r1, int r2) {
		int i = names.indexOf(name);
		if (i < 0) throw new IllegalStateException("no supplier defined for variable '" + name + "'");
		return i < r1 ? i + r2 : i - r1;
	}

	private static int parsePins(ArrayList<LinkVar> vars, String pins, ByteArrayList heights) {
		try {
		char[] ca = pins.toCharArray();
		int h = 0;
		for (int i = 0; i < ca.length;) {
			char c = ca[i++];
			if (Character.isWhitespace(c)) {
				h++;
				continue;
			}
			int j = descrLength(ca, i);
			vars.add(new LinkVar(c, IOUtils.getValidType(pins.substring(i, i += j))));
			heights.add((byte) h++);
		}
		return h;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("invalid type descriptor in pin layout: '" + pins + "'");
		}
	}

	private static int descrLength(char[] code, int ofs) {
		char c = code[ofs];
		if (c == '[')
			return descrLength(code, ofs + 1) + 1;
		if (c == 'L') {
			int n = 2;
			while(code[++ofs] != ';') n++;
			return n;
		}
		return 1;
	}

	private static void parseNode(JsonReader jr, ArrayList<GeneratedNode> nodes, CharArrayList names) throws IOException {
		jr.beginObject();
		LinkVar out = null;
		ArrayList<LinkVar> in = new ArrayList<>();
		ArrayList<Function<GeneratedGate, Object>> args = new ArrayList<>();
		int sort = Integer.MAX_VALUE;
		ASMCode code = null;
		boolean strict = false;
		Predicate<GeneratedGate> precond = alwaysTrue;
		while(jr.hasNext()) {
			switch(jr.nextName()) {
			case "typestrict":
				strict = jr.nextBoolean();
				break;
			case "if":
				precond = parsePrecondition(jr, precond);
				break;
			case "in":
				jr.beginArray();
				while(jr.hasNext()) {
					LinkVar v = new LinkVar(jr.nextString());
					if (sort <= in.size())
						in.add(sort++, v);
					else in.add(v);
				}
				jr.endArray();
				break;
			case "sortin":
				jr.beginArray();
				sort = Math.min(sort, in.size());
				while(jr.hasNext())
					in.add(new LinkVar(jr.nextString()));
				jr.endArray();
				break;
			case "out":
				out = new LinkVar(jr.nextString());
				break;
			case "code":
				if (jr.peek() == JsonToken.BEGIN_ARRAY) {
					jr.beginArray();
					try {
						while(jr.hasNext())
							ASMCode.PARSER.accept(jr.nextString());
						code = ASMCode.PARSER.build();
					} finally {
						ASMCode.PARSER.clear();
					}
					jr.endArray();
				} else code = ASMCode.get(new ResourceLocation(jr.nextString()));
				break;
			case "args":
				jr.beginArray();
				while(jr.hasNext()) {
					Object o = nextValue(jr);
					if (o instanceof String) {
						String s = (String)o;
						if (s.length() > 0 && s.charAt(0) == '$') {
							if (s.length() > 1 && s.charAt(1) == 'l') args.add(g -> g.label);
							else {
								int i = Integer.parseInt(s.substring(1));
								args.add(g -> ((IParameterizedGate)g).getParam(i));
							}
							continue;
						}
						if (s.length() > 0 && s.charAt(0) == '\\') o = s.substring(1);
					}
					Object o_ = o;
					args.add(g -> o_);
				}
				jr.endArray();
				break;
			default:
				jr.skipValue();
			}
		}
		jr.endObject();
		if (code == null) throw new IllegalStateException("missing code definition");
		GeneratedNode node = code.extra == null ? 
				new GeneratedNode(out.type, code, in, sort, strict, precond, args) :
				new GeneratedNode.Bool(out.type, code, in, sort, strict, precond, args);
		int i = names.indexOf(out.name);
		if (i < 0) {
			nodes.add(node);
			names.add(out.name);
		} else {
			GeneratedNode n = nodes.get(i);
			while (n.next != null) n = n.next;
			n.next = node;
		}
	}

	private static final Predicate<GeneratedGate> alwaysTrue = Predicates.alwaysTrue();

	private static Predicate<GeneratedGate> parsePrecondition(JsonReader jr, Predicate<GeneratedGate> precond) throws IOException {
		jr.beginObject();
		int var = -1;
		while(jr.hasNext()) {
			Predicate<GeneratedGate> cond;
			switch(jr.nextName()) {
			case "var":
				var = jr.nextInt();
				continue;
			case "eq": {
				Object val = nextValue(jr); int var_ = var;
				cond = g -> ((IParameterizedGate)g).getParam(var_).equals(val);
			}	break;
			case "neq": {
				Object val = nextValue(jr); int var_ = var;
				cond = g -> !((IParameterizedGate)g).getParam(var_).equals(val);
			}	break;
			default:
				jr.skipValue();
				continue;
			}
			if (precond == alwaysTrue) precond = cond;
			else precond = precond.and(cond);
		}
		jr.endObject();
		return precond;
	}

}
