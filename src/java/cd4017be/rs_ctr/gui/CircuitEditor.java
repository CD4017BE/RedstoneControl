package cd4017be.rs_ctr.gui;

import cd4017be.lib.Gui.DataContainer.IGuiData;
import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer;
import cd4017be.lib.Gui.HidableSlot;
import cd4017be.lib.Gui.comp.Button;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.InfoTab;
import cd4017be.lib.Gui.comp.Progressbar;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.Gui.comp.Tooltip;
import cd4017be.lib.util.Utils;
import cd4017be.rs_ctr.Main;
import cd4017be.rs_ctr.circuit.editor.CircuitInstructionSet;
import cd4017be.rs_ctr.tileentity.Editor;
import cd4017be.rscpl.editor.BoundingBox2D;
import cd4017be.rscpl.editor.Gate;
import cd4017be.rscpl.gui.GatePalette;
import cd4017be.rscpl.gui.SchematicBoard;

import static cd4017be.rs_ctr.tileentity.Editor.*;
import static cd4017be.rscpl.editor.Schematic.*;

import cd4017be.lib.Gui.ModularGui;
import cd4017be.lib.Gui.TileContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;


/**
 * @author CD4017BE
 *
 */
public class CircuitEditor extends ModularGui {

	private static final ResourceLocation BG_TEX = new ResourceLocation(Main.ID, "textures/gui/editor.png");
	private static final ResourceLocation COMP_TEX = new ResourceLocation(Main.ID, "textures/gui/palette.png");

	public final Editor tile;
	public final SchematicBoard board;
	public final GatePalette palette;
	public final TextField editLabel;// editCfg;
	public final GuiErrorMarker error;
	//private GuiDebugger debug;

	/**
	 * @param container
	 */
	public CircuitEditor(IGuiData data, EntityPlayer player) {
		super(new TileContainer(data, player));
		this.tile = (Editor)data;
		GuiFrame comps = new GuiFrame(this, 256, 256, 17).background(BG_TEX, 0, 0).title(tile.getName(), 0.1F);
		comps.texture(COMP_TEX, 256, 256);
		new InfoTab(comps, 7, 8, 7, 6, "gui.rs_ctr.editor.info");
		new TextField(comps, 120, 8, 128, 4, 64, ()-> tile.name, (name)-> sendPkt(A_NAME, name)).tooltip("gui.rs_ctr.editor.name");
		this.board = new SchematicBoard(comps, 8, 16, tile.schematic, this::changeSelPart);
		(this.palette = new GatePalette(comps, CircuitInstructionSet.TABS, 7, 173, board::place)).title("\\Gate Palette", 0.5F);
		(this.editLabel = new TextField(comps, 74, 7, 174, 174, 20, this::getLabel, (s)-> send(SET_LABEL, s))).tooltip("gui.rs_ctr.opLabel");
		//this.editCfg = new TextField(comps, 74, 7, 174, 185, 20, this::getConfig, (s)-> send(SET_VALUE, s));
		new Button(comps, 18, 9, 231, 195, 0, null, board::del).tooltip("gui.rs_ctr.editor.del");
		new Button(comps, 16, 16, 174, 192, 2, ()-> palette.enabled() ? 1 : 0, (s)-> {
			boolean hide = !palette.enabled();
			palette.setEnabled(hide);
			for (Slot slot : inventorySlots.inventorySlots)
				if (slot instanceof HidableSlot)
					((HidableSlot)slot).hideSlot(hide);
			drawInvTitle = !hide;
		}).texture(178, 0).tooltip("gui.rs_ctr.palette.open#");
		new Button(comps, 16, 16, 232, 210, 0, null, (i)-> sendCommand(A_NEW)).tooltip("gui.rs_ctr.editor.new");
		new Button(comps, 16, 16, 214, 210, 0, null, (i)-> sendCommand(A_LOAD)).tooltip("gui.rs_ctr.editor.load");
		new Button(comps, 16, 16, 196, 210, 0, null, (i)-> sendCommand(A_SAVE)).tooltip("gui.rs_ctr.editor.save");
		new Button(comps, 16, 16, 174, 210, 0, null, this::compile).tooltip("gui.rs_ctr.editor.compile");
		
		new Progressbar(comps, 56, 4, 192, 232, 0, 226, Progressbar.H_SLIDE, ()-> Math.min(this.tile.ingreds[0], 112), 0, 112);
		new Progressbar(comps, 56, 4, 192, 238, 0, 232, Progressbar.H_SLIDE, ()-> Math.min(this.tile.ingreds[1], 112), 0, 112);
		new Progressbar(comps, 56, 4, 192, 244, 0, 238, Progressbar.H_SLIDE, ()-> Math.min(this.tile.ingreds[2], 112), 0, 112);
		new Progressbar(comps, 56, 2, 192, 233, 0, 230, Progressbar.PIXELS, ()-> this.tile.ingreds[3], 0, 112);
		new Progressbar(comps, 56, 2, 192, 239, 0, 236, Progressbar.PIXELS, ()-> this.tile.ingreds[4], 0, 112);
		new Progressbar(comps, 56, 2, 192, 245, 0, 242, Progressbar.PIXELS, ()-> this.tile.ingreds[5], 0, 112);
		new Tooltip(comps, 56, 16, 192, 232, "gui.rs_ctr.editor.ingreds", ()-> new Object[] {
			this.tile.ingreds[0], this.tile.ingreds[3], this.tile.ingreds[1], this.tile.ingreds[4], this.tile.ingreds[2], this.tile.ingreds[5]
		});
		this.compGroup = comps;
		this.error = new GuiErrorMarker(this);
		palette.setEnabled(false);
		changeSelPart();
	}

	private void send(byte tag, String s) {
		BoundingBox2D<Gate<?>> part = board.selPart;
		if (part == null) return;
		PacketBuffer pkt = BlockGuiHandler.getPacketTargetData(((DataContainer)inventorySlots).data.pos());
		pkt.writeByte(tag).writeByte(part.owner.index).writeCharSequence(s, Utils.UTF8);
		BlockGuiHandler.sendPacketToServer(pkt);
	}

	void changeSelPart() {
		BoundingBox2D<Gate<?>> part = board.selPart;
		if (part == null) {
			editLabel.setEnabled(false);
			//editCfg.setEnabled(false);
		} /*else if (part.owner instanceof IConfigurable) {
			editLabel.setEnabled(true);
			editCfg.tooltip(((IConfigurable)part.owner).cfgTooltip());
			editCfg.setEnabled(true);
		}*/ else {
			editLabel.setEnabled(true);
			//editCfg.setEnabled(false);
		}
	}

	private String getLabel() {
		BoundingBox2D<Gate<?>> part = board.selPart;
		return part != null ? part.owner.label : "";
	}

	private void compile(int b) {
		/*if (isShiftKeyDown()) {
			Item item = tile.inventory.getItem();
			if (item instanceof IChipItem) {
				IChipItem cp = (IChipItem)item;
				Chip chip = cp.provideChip(tile.inventory);
				if (chip != null) {
					compGroup.remove(debug);
					debug = new GuiDebugger((GuiFrame)compGroup, chip);
					debug.init(width, height, zLevel, fontRenderer);
					debug.position(8, 8);
				}
			}
		}*/
		sendCommand(A_COMPILE);
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		board.update();
		error.update(tile.ingreds[6]);
		//if (debug != null)
		//	debug.update();
	}

}
