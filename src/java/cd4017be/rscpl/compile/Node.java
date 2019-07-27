package cd4017be.rscpl.compile;

import java.util.ArrayList;
import java.util.HashSet;
import org.objectweb.asm.Type;

/**
 * A computation Node in a program's data flow.
 * @author cd4017be
 */
public class Node {

	final NodeCompiler code;
	/**the inputs of this node */
	final Dep[] deps;
	/**Nodes that use the result of this Node as input */
	final ArrayList<Dep> users;
	/**total level of recursion required to evaluate the result of this Node (= 0 if no inputs)*/
	final int order;
	/**index into the local variable table where the result of this Node is stored.<br>
	 * -1 means it's not evaluated yet and {@link Integer#MAX_VALUE} means it's evaluated but not stored in local variable table. */
	int localIdx = -1;
	/**remaining times the value will be loaded from the local variable table.<br>
	 * Used to determine when the index can be reused. */
	int remUses;
	/**cached result of {@link #getCommonUser()} */
	Node commonUser;

	public Node(NodeCompiler code, Node... inputs) {
		this.code = code;
		this.users = new ArrayList<>(1);
		this.deps = new Dep[inputs.length];
		int order = -1;
		for (int i = 0; i < inputs.length; i++) {
			Node n = inputs[i];
			if (n == null) continue;
			deps[i] = new Dep(n, this, code.getInType(i));
			if (n.order > order) order = n.order;
		}
		this.order = order;
	}

	int getNumOfUsers() {
		int n = 0;
		for (Dep d : users)
			if (d.type != Type.VOID_TYPE)
				n++;
		return remUses = n;
	}

	/**@return the lowest order Node that is the only Node who (indirectly) uses the result of this Node. */
	Node getCommonUser() {
		if (commonUser != null) return commonUser;
		if (users.size() == 1) return commonUser = users.get(0).dst;
		HashSet<Node> set = new HashSet<>();
		for (Dep u : users) set.add(u.dst);
		int last = order + 1;
		while(set.size() > 1) {
			Node lowest = null;
			int lvl = Integer.MAX_VALUE;
			for (Node n : set) {
				int l = n.order;
				if (l < lvl) {
					lvl = l;
					lowest = n;
					if (l == last) break;
				}
			}
			last = lvl;
			set.remove(lowest);
			set.add(lowest.getCommonUser());
		}
		return set.iterator().next();
	}

}
