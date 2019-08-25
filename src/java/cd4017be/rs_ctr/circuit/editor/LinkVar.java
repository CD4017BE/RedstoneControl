package cd4017be.rs_ctr.circuit.editor;

import org.objectweb.asm.Type;

import cd4017be.rscpl.util.IOUtils;

public class LinkVar {
	public final Type type;
	public char name;

	public LinkVar(String s) {
		if (s.length() < 2) throw new IllegalArgumentException("invalid variable descriptor '" + s + "'");
		name = s.charAt(0);
		type = IOUtils.getValidType(s.substring(1));
	}

	public LinkVar(char name, Type type) {
		this.type = type;
		this.name = name;
	}
}