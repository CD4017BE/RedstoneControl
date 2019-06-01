package cd4017be.rs_ctr.circuit;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import cd4017be.rscpl.compile.Compiler;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.circuit.gates.Input;
import cd4017be.rs_ctr.circuit.gates.Output;
import cd4017be.rs_ctr.circuit.gates.SyntheticOp;
import cd4017be.rscpl.compile.Branch;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.editor.InvalidSchematicException;

/**
 * @author CD4017BE
 *
 */
public class CircuitCompiler extends Compiler<CompiledCircuit> {

	public static final CircuitCompiler INSTANCE = new CircuitCompiler();

	private CircuitCompiler() {
		super(Circuit.class);
		setName(UnloadedCircuit.name(new UUID(0, 0)));
	}

	@Override
	protected CompiledCircuit newProgram(Collection<Gate<?>> gatesIn) throws InvalidSchematicException {
		CompiledCircuit cc = new CompiledCircuit();
		List<Input> inputs = new ArrayList<>();
		List<Output> outputs = new ArrayList<>();
		SyntheticOp in =  new SyntheticOp(0, CircuitInstructionSet.getInArr),
				out = new SyntheticOp(0, CircuitInstructionSet.getOutArr);
		for (Gate<?> g : gatesIn)
			if (g == null);
			else if (g.type == CircuitInstructionSet.in) {
				Input i = (Input)g;
				i.portID = inputs.size();
				i.setInput(0, in);
				inputs.add(i);
			} else if (g.type == CircuitInstructionSet.out) {
				Output o = (Output)g;
				o.portID = outputs.size();
				o.setInput(1, o.getInput(0));
				o.setInput(2, out);
				o.setInput(3, out);
				outputs.add(o);
			}
		cc.setIOPins(inputs, outputs);
		return cc;
	}

	@Override
	protected void addMain(CompiledCircuit program, ClassWriter cw, List<Branch> parts) throws InvalidSchematicException {
		Context c = new Context(this, 2);
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "tick", "()I", null, null);
		mv.visitCode();
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, Context.DIRTY_IDX);
		for (Branch b : parts) b.compile(mv, c);
		mv.visitVarInsn(ILOAD, Context.DIRTY_IDX);
		mv.visitInsn(IRETURN);
		mv.visitMaxs(0, 2); //automatically computed
		mv.visitEnd();
	}

}
