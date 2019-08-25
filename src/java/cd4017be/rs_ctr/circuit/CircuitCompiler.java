package cd4017be.rs_ctr.circuit;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import cd4017be.rscpl.compile.Compiler;
import cd4017be.rs_ctr.circuit.editor.GetFieldNode;
import cd4017be.rs_ctr.circuit.gates.Input;
import cd4017be.rs_ctr.circuit.gates.Output;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.compile.Dep;
import cd4017be.rscpl.compile.Node;
import cd4017be.rscpl.compile.NodeCompiler;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;

/**
 * @author CD4017BE
 *
 */
public class CircuitCompiler extends Compiler<CompiledCircuit> {

	public static final CircuitCompiler INSTANCE = new CircuitCompiler();
	public static final NodeCompiler getIOArr = new GetFieldNode(Type.getType("[I"));


	private CircuitCompiler() {
		super(Circuit.class);
		setName(UnloadedCircuit.name(new UUID(0, 0)));
	}

	@Override
	protected CompiledCircuit newProgram(Collection<Gate> gatesIn) throws InvalidSchematicException {
		CompiledCircuit cc = new CompiledCircuit();
		List<Input> inputs = new ArrayList<>();
		List<Output> outputs = new ArrayList<>();
		Node in = new Node(getIOArr, "inputs"),
			out = new Node(getIOArr, "outputs");
		for (Gate g : gatesIn)
			if (g == null);
			else if (g instanceof Input) {
				Input i = (Input)g;
				i.setLink(in, 0);
				inputs.add(i);
			} else if (g instanceof Output) {
				Output o = (Output)g;
				o.setLink(out, 0);
				outputs.add(o);
			}
		Collections.sort(inputs, BY_VERT_POS);
		for (int i = 0; i < inputs.size(); i++)
			inputs.get(i).portID = i;
		Collections.sort(outputs, BY_VERT_POS);
		for (int i = 0; i < outputs.size(); i++)
			outputs.get(i).portID = i;
		cc.setIOPins(inputs, outputs);
		return cc;
	}

	private static final Comparator<Gate> BY_VERT_POS = (g1, g2) -> g1.rasterY - g2.rasterY;

	@Override
	public void compile(Dep retVal, ClassWriter cw) {
		Context c = new Context(this, 2);
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "tick", "()I", null, null);
		mv.visitCode();
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, Context.DIRTY_IDX);
		retVal.compile(mv, c);
		mv.visitVarInsn(ILOAD, Context.DIRTY_IDX);
		mv.visitInsn(IRETURN);
		mv.visitMaxs(0, 2); //automatically computed
		mv.visitEnd();
	}

}
