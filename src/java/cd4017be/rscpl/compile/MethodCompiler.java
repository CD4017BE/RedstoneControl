package cd4017be.rscpl.compile;

import java.util.Arrays;
import java.util.Collection;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Creates the final Node where all execution flow leads to.
 * @author cd4017be
 */
public interface MethodCompiler extends NodeCompiler {

	/**
	 * 
	 * @param cw the class being assembled
	 * @param code the method to compile
	 * @param endPoints all Nodes that affect the program state or produce the method's return value.
	 */
	public static void addMethod(ClassWriter cw, MethodCompiler code, Collection<Node> endPoints) {
		code.compile(new Dep(new Node(code, endPoints.toArray(new Node[endPoints.size()])), null, code.getOutType()), cw);
	}

	@Override
	default Type getInType(int i) {
		return Type.VOID_TYPE;
	}

	@Override
	default Type getOutType() {
		return Type.VOID_TYPE;
	}

	@Override
	default void compile(Dep[] inputs, MethodVisitor mv, Context context) {
		Arrays.sort(inputs);
		for (Dep d : inputs)
			d.compile(mv, context);
	}

	/**
	 * compile the actual method and add it to the given class
	 * @param retVal call {@link Dep#compile(MethodVisitor, Context)} to compile the code
	 * @param cw the class being assembled
	 */
	void compile(Dep retVal, ClassWriter cw);

}
