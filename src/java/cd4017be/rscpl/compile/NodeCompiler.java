package cd4017be.rscpl.compile;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Compiles a {@link Node} into JVM instructions
 * @author cd4017be
 */
public interface NodeCompiler {

	/**
	 * @param i input index
	 * @return the expected type of the given input
	 */
	Type getInType(int i);

	/**
	 * @return the type of the result left behind on the operand stack<br>
	 * {@link Type#VOID_TYPE} means no result.
	 */
	Type getOutType();

	/**
	 * Compile the Node into JVM instructions so that the result is put on top of the operand stack.
	 * @param inputs use {@link Dep#compile(MethodVisitor, Context)} on these to get the Node's inputs onto the operand stack.
	 * @param param custom data feed through from {@link Node#Node(NodeCompiler, Object, Node...)}
	 * @param mv the method being assembled
	 * @param context handler for local variables
	 */
	void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context);

	/**
	 * For Nodes that support a boolean result in the form of a conditional jump
	 * @author cd4017be
	 */
	public interface Bool extends NodeCompiler {
		/**
		 * Compile the Node into JVM instructions so that the result controls a conditional jump to the given target label.
		 * @param inputs use {@link Dep#compile(MethodVisitor, Context)} on these to get the Node's inputs onto the operand stack.
		 * @param param custom data feed through from {@link Node#Node(NodeCompiler, Object, Node...)}
		 * @param mv the method being assembled
		 * @param context handler for local variables
		 * @param target the label to jump to
		 * @param cond the value to jump on
		 */
		void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context, Label target, boolean cond);
	}

	/** has no inputs, does nothing and returns void */
	public static final NodeCompiler NOP = new NodeCompiler() {

		@Override
		public Type getInType(int i) {
			return null;
		}

		@Override
		public Type getOutType() {
			return Type.VOID_TYPE;
		}

		@Override
		public void compile(Dep[] inputs, Object param, MethodVisitor mv, Context context) {}

	};

}
