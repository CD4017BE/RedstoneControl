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
		for (Gate<?> g : gatesIn)
			if (g == null);
			else if (g.type == CircuitInstructionSet.in) {
				((Input)g).portID = inputs.size();
				inputs.add((Input)g);
			} else if (g.type == CircuitInstructionSet.out) {
				((Output)g).portID = outputs.size();
				outputs.add((Output)g);
			}
		cc.setIOPins(inputs, outputs);
		return cc;
	}

	@Override
	protected void addMain(CompiledCircuit program, ClassWriter cw, List<Branch> parts) throws InvalidSchematicException {
		Context c = new Context(this, 3);
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "tick", "()Z", null, null);
		mv.visitCode();
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, Context.DIRTY_IDX);
		mv.visitVarInsn(ALOAD, Context.THIS_IDX);
		mv.visitFieldInsn(GETFIELD, C_THIS, "inputs", "[I");
		mv.visitVarInsn(ASTORE, Context.IO_IDX);
		boolean processIn = true;
		for (Branch b : parts) {
			if (processIn && b.hasIO == Branch.IS_OUT) {
				mv.visitVarInsn(ALOAD, Context.THIS_IDX);
				mv.visitFieldInsn(GETFIELD, C_THIS, "outputs", "[I");
				mv.visitVarInsn(ASTORE, Context.IO_IDX);
				processIn = false;
			}
			b.compile(c).accept(mv);
		}
		mv.visitVarInsn(ILOAD, Context.DIRTY_IDX);
		mv.visitInsn(IRETURN);
		mv.visitMaxs(0, 3); //automatically computed
		mv.visitEnd();
	}

}
