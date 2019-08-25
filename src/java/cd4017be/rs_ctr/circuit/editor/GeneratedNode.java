package cd4017be.rs_ctr.circuit.editor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import cd4017be.rs_ctr.circuit.editor.ASMCode.CompCont;
import cd4017be.rs_ctr.circuit.editor.ASMCode.Insn;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.compile.NodeCompiler;
import it.unimi.dsi.fastutil.chars.CharArrayList;

/**
 * 
 * @author cd4017be
 *
 */
public class GeneratedNode implements NodeCompiler {

	public final ASMCode code;
	public final LinkVar[] inputs;
	public final Type out;
	protected final int sort;
	private final boolean strictTypes;
	private final Predicate<GeneratedGate> precondition;
	private final Function<GeneratedGate, Object>[] arguments;
	public GeneratedNode next;

	@SuppressWarnings("unchecked")
	public GeneratedNode(Type result, ASMCode code, List<LinkVar> inputs, int sort, boolean strict, Predicate<GeneratedGate> precond, List<Function<GeneratedGate, Object>> args) {
		this.out = result;
		this.inputs = inputs.toArray(new LinkVar[inputs.size()]);
		this.code = code;
		this.sort = sort;
		this.strictTypes = strict;
		this.precondition = precond;
		this.arguments = args.toArray(new Function[args.size()]);
	}

	public void translateVars(CharArrayList names, int nn, int ni) {
		for (LinkVar v : inputs)
			v.name = (char)GeneratedType.lookup(names, v.name, nn, ni);
		if (next != null) next.translateVars(names, nn, ni);
	}

	public Node createNode(GeneratedType type, GeneratedGate gate) {
		if (!precondition.test(gate)) return null;
		Node[] ins = new Node[inputs.length];
		for (int i = 0; i < ins.length; i++) {
			LinkVar v = inputs[i];
			Node n = type.getNode(gate, v.name);
			if (n == null || !Dep.canConvert(n.code.getOutType(), v.type, strictTypes)) return null;
			ins[i] = n;
		}
		Object[] args = new Object[arguments.length];
		for (int i = 0; i < args.length; i++)
			args[i] = arguments[i].apply(gate);
		return new Node(this, args, ins);
	}

	@Override
	public Type getInType(int i) {
		return inputs[i].type;
	}

	@Override
	public Type getOutType() {
		return out;
	}

	@Override
	public void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context) {
		if (sort < inputs.length)
			Arrays.sort(inputs, sort, inputs.length);
		CompCont c = new CompCont(code, inputs, (Object[])param, context, null);
		for (Insn i : code.instructions)
			i.visit(mv, c);
	}

	public static class Bool extends GeneratedNode implements NodeCompiler.Bool {

		public Bool(Type result, ASMCode code, List<LinkVar> inputs, int sort, boolean strict, Predicate<GeneratedGate> precond, List<Function<GeneratedGate, Object>> args) {
			super(result, code, inputs, sort, strict, precond, args);
		}

		@Override
		public void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context, Label target, boolean cond) {
			if (sort < inputs.length)
				Arrays.sort(inputs, sort, inputs.length);
			CompCont c = new CompCont(code, inputs, (Object[])param, context, target);
			ASMCode code = this.code.extra;
			if (cond) code = code.extra;
			for (Insn i : code.instructions)
				i.visit(mv, c);
		}

	}

}
