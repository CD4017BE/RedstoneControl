package cd4017be.rs_ctr.circuit.gates;

import static org.objectweb.asm.Opcodes.*;

import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.rs_ctr.circuit.editor.BasicType;
import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.editor.ConfigurableGate;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.graph.Operator;
import cd4017be.rscpl.graph.Pin;
import cd4017be.rscpl.graph.ReadOp;
import cd4017be.rscpl.graph.WriteOp;
import cd4017be.rscpl.gui.GateTextureHandler;
import cd4017be.rscpl.gui.ISpecialCfg;
import cd4017be.rscpl.gui.ISpecialRender;
import cd4017be.rscpl.gui.SchematicBoard;
import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public class WriteVar extends Combinator implements WriteOp, ISpecialRender, ISpecialCfg, ConfigurableGate {

	private boolean interrupt = true;

	/**
	 * @param type
	 * @param index
	 */
	public WriteVar(BasicType type, int index) {
		super(type, index);
	}

	@Override
	public void writeCfg(ByteBuf data) {
		data.writeBoolean(interrupt);
	}

	@Override
	public void readCfg(ByteBuf data) {
		interrupt = data.readBoolean();
	}

	@Override
	public void compile(MethodVisitor mv, Context context) {
		Operator op = inputs[visibleInputs()];
		if (interrupt) {
			if (op == null)
				inputs[visibleInputs()] = new Read();
			type.outputs[0].compile(mv, context, inputs, label);
			if (receivers.isEmpty()) mv.visitInsn(POP);
		} else {
			//not actually using the code, just make sure any local variable is freed up
			if (op != null) op.compile(null, context);
			
			mv.visitVarInsn(ALOAD, Context.THIS_IDX);
			inputs[0].compile(mv, context);
			if (!receivers().isEmpty()) mv.visitInsn(DUP_X1);
			mv.visitFieldInsn(PUTFIELD, context.compiler.C_THIS, label, outType().getDescriptor());
		}
	}

	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public String name() {
		return label;
	}

	@Override
	public void link(ReadOp read) {
		this.setInput(visibleInputs(), read);
	}

	private class Read implements Operator {
		@Override
		public Set<Pin> receivers() {return null;}
		@Override
		public Type outType() {return WriteVar.this.outType();}
		@Override
		public int inputCount() {return 0;}
		@Override
		public int getPin() {return 0;}
		@Override
		public Operator getInput(int pin) {return null;}
		@Override
		public Gate<?> getGate() {return null;}
		@Override
		public Operator getActual() {return null;}
		@Override
		public void compile(MethodVisitor mv, Context context) {
			mv.visitVarInsn(ALOAD, Context.THIS_IDX);
			mv.visitFieldInsn(GETFIELD, context.compiler.C_THIS, label, outType().getDescriptor());
		}
	}

	@Override
	public void draw(SchematicBoard board, int x, int y) {
		GateTextureHandler.drawTinyText(board.parent.getDraw(), label, x + 2, y + 2, 5, board.parent.zLevel + 1);
	}

	@Override
	public void setupCfgGUI(GuiFrame gui, Runnable updateCfg) {
		new Button(gui, 76, 9, 0, 9, 2, ()-> interrupt ? 1 : 0, (i)-> {
			interrupt = i != 0;
			updateCfg.run();
		}).texture(180, 59).tooltip("gui.rs_ctr.interrupt#");
	}

}
