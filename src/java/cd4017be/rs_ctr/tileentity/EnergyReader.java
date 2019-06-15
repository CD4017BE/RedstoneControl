package cd4017be.rs_ctr.tileentity;

import java.util.List;

import org.apache.logging.log4j.core.util.Loader;

import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.api.com.BlockReference;
import cd4017be.rs_ctr.api.interact.IInteractiveComponent;
import cd4017be.rs_ctr.gui.BlockButton;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * @author CD4017BE
 *
 */
public class EnergyReader extends Sensor {

	private static final boolean HAS_IC2 = Loader.isClassAvailable("ic2.api.tile.IEnergyStorage");
	private static final int FE = 0, EU = 1, N = 2;

	int readMode = FE;
	BlockButton btn = new BlockButton(
		(a)-> {
			readMode = (readMode + ((a & BlockButton.A_SNEAKING) != 0 ? N - 1 : 1)) % N;
			markDirty(REDRAW);
		},
		()-> "_buttons.energy(" + readMode + ")",
		()-> TooltipUtil.translate("port.rs_ctr.energy" + readMode)
	).setSize(0.1875F, 0.125F);

	@Override
	protected int readValue(BlockReference ref) {
		switch(readMode) {
		case FE: {
			IEnergyStorage es = ref.getCapability(CapabilityEnergy.ENERGY);
			return es != null ? es.getEnergyStored() : 0;
		}
		case EU: if (HAS_IC2) {
				TileEntity te = ref.world.getTileEntity(ref.pos);
				if (te instanceof ic2.api.tile.IEnergyStorage)
					return ((ic2.api.tile.IEnergyStorage)te).getStored();
			}
		default: return 0;
		}
	}

	@Override
	protected void storeState(NBTTagCompound nbt, int mode) {
		super.storeState(nbt, mode);
		nbt.setByte("mode", (byte)readMode);
	}

	@Override
	protected void loadState(NBTTagCompound nbt, int mode) {
		super.loadState(nbt, mode);
		readMode = nbt.getByte("mode");
	}

	@Override
	protected void initGuiComps(List<IInteractiveComponent> list) {
		super.initGuiComps(list);
		list.add(btn);
	}

	@Override
	protected void orient() {
		super.orient();
		btn.setLocation(0.5, 0.75, 0.25, o);
	}

}
