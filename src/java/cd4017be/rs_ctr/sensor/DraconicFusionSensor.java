package cd4017be.rs_ctr.sensor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import cd4017be.api.rs_ctr.com.BlockReference;
import cd4017be.api.rs_ctr.sensor.IBlockSensor;
import cd4017be.lib.jvm_utils.FieldWrapper;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.Main;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import static cd4017be.lib.jvm_utils.ClassUtils.*;

/**
 * @author CD4017BE
 *
 */
public class DraconicFusionSensor implements IBlockSensor, INBTSerializable<NBTBase> {

	private static final Class<?>
		C_REACTOR_COMP = getClassOrNull("com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorComponent"),
		C_REACTOR_CORE = getClassOrNull("com.brandon3055.draconicevolution.blocks.reactor.tileentity.TileReactorCore"),
		C_MANAGED_ENUM = getClassOrNull("com.brandon3055.brandonscore.lib.datamanager.ManagedEnum"),
		C_MANAGED_DOUBLE = getClassOrNull("com.brandon3055.brandonscore.lib.datamanager.ManagedDouble"),
		C_MANAGED_INT = getClassOrNull("com.brandon3055.brandonscore.lib.datamanager.ManagedInt");
	private static final Method
		M_GET_CORE = getMethodOrNull(C_REACTOR_COMP, "tryGetCore");
	private static final Field
		F_RS_MODE = getFieldOrNull(C_REACTOR_COMP, "rsMode"),
		F_REACTABLE_FUEL = getFieldOrNull(C_REACTOR_CORE, "reactableFuel"),
		F_CONVERTED_FUEL = getFieldOrNull(C_REACTOR_CORE, "convertedFuel"),
		F_TEMPERATURE = getFieldOrNull(C_REACTOR_CORE, "temperature"),
		F_SHIELD = getFieldOrNull(C_REACTOR_CORE, "shieldCharge"),
		F_MAX_SHIELD = getFieldOrNull(C_REACTOR_CORE, "maxShieldCharge"),
		F_SATURATION = getFieldOrNull(C_REACTOR_CORE, "saturation"),
		F_MAX_SATURATION = getFieldOrNull(C_REACTOR_CORE, "maxSaturation"),
		F_ENUM_VALUE = getFieldOrNull(C_MANAGED_ENUM, "value"),
		F_DOUBLE_VALUE = getFieldOrNull(C_MANAGED_DOUBLE, "value"),
		F_INT_VALUE = getFieldOrNull(C_MANAGED_INT, "value");

	public static final ResourceLocation MODEL = new ResourceLocation(Main.ID, "block/_sensor.draconic()");
	private static final double PRECISION = 1000D;
	public static boolean INVALID_API;
	static {
		if (F_RS_MODE == null || F_REACTABLE_FUEL == null || F_CONVERTED_FUEL == null ||
			F_TEMPERATURE == null || F_SHIELD == null || F_MAX_SHIELD == null ||
			F_SATURATION == null || F_MAX_SATURATION == null || F_ENUM_VALUE == null ||
			F_DOUBLE_VALUE == null || F_INT_VALUE == null || M_GET_CORE == null) {
			INVALID_API = true;
			Main.LOG.warn("API to Draconic Fusion Reactor is probably outdated: some classes, methods or fields not found!");
		}
	}

	IHost host;
	/**
	 * cached references to Reactor Core state fields.
	 * Conveniently DE wraped them in isolated objects so there is no reference to the TileEntity itself kept im memory.
	 */
	FieldWrapper<?> satur, maxSatur, fuel, chaos, temp, shield, maxShield;
	FieldWrapper<Enum<?>> mode;
	IBlockState lastBlock;
	byte lastMode = -2;

	@Override
	public int readValue(BlockReference block) {
		if (block.getState() != lastBlock) onRefChange(block, host);
		if (lastMode < 0) return 0;
		int m = mode.get().ordinal();
		if (m != lastMode && host != null) {
			lastMode = (byte) m;
			host.syncSensorState();
		}
		switch(m) {
		case 0: //TEMP -> T [°C]
			return (int)temp.getAsDouble();
		case 1: { //TEMP_INV -> reactor status
			int t = (int)temp.getAsDouble();
			return t > 2500 ? (t > 8000 ? 3 : 2) : t > 2000 ? 1 : t > 100 ? 0 : -1;
		}
		case 2: //FIELD -> E_shield [RF]
			return (int)shield.getAsDouble();
		case 3: //FIELD_INV -> E_shield [0/00]
			return (int)(shield.getAsDouble() / maxShield.getAsDouble() * PRECISION);
		case 4: //SAT -> E_core [RF]
			return satur.getAsInt();
		case 5: //SAT_INV -> E_core [0/00]
			return (int)(satur.getAsDouble() / maxSatur.getAsDouble() * PRECISION);
		case 6: //FUEL
			return (int)(fuel.getAsDouble() + chaos.getAsDouble());
		case 7: { //FUEL_INV
			double c = chaos.getAsDouble();
			return (int)(c / (c + fuel.getAsDouble()) * PRECISION);
		}
		default: return 0;
		}
	}

	@Override
	public void onRefChange(BlockReference block, IHost host) {
		if ((this.host = host) == null) {
			fuel = chaos = temp = shield = maxShield = satur = maxSatur = mode = null;
			lastBlock = null;
			lastMode = -2;
			return;
		}
		lastBlock = block != null ? block.getState() : null;
		if (INVALID_API) return;
		TileEntity te = block == null ? null : block.getTileEntity();
		if (!C_REACTOR_COMP.isInstance(te)) {
			fuel = chaos = temp = shield = maxShield = satur = maxSatur = mode = null;
			if (lastMode != -1) {
				lastMode = -1;
				host.syncSensorState();
			}
			return;
		}
		try {
			mode = new FieldWrapper<>(F_ENUM_VALUE, F_RS_MODE.get(te));
			Object core = M_GET_CORE.invoke(te);
			if (core != null) {
				fuel = new FieldWrapper<>(F_DOUBLE_VALUE, F_REACTABLE_FUEL.get(core));
				chaos = new FieldWrapper<>(F_DOUBLE_VALUE, F_CONVERTED_FUEL.get(core));
				temp = new FieldWrapper<>(F_DOUBLE_VALUE, F_TEMPERATURE.get(core));
				shield = new FieldWrapper<>(F_DOUBLE_VALUE, F_SHIELD.get(core));
				maxShield = new FieldWrapper<>(F_DOUBLE_VALUE, F_MAX_SHIELD.get(core));
				satur = new FieldWrapper<>(F_INT_VALUE, F_SATURATION.get(core));
				maxSatur = new FieldWrapper<>(F_INT_VALUE, F_MAX_SATURATION.get(core));
				if (lastMode < 0) {
					lastMode = (byte) mode.get().ordinal();
					host.syncSensorState();
				}
				return;
			} else
				fuel = chaos = temp = shield = maxShield = satur = maxSatur = null;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Main.LOG.error("API to Draconic Fusion Reactor is probably outdated: ", e);
			INVALID_API = true;
		}
		if (lastMode != -1) {
			lastMode = -1;
			host.syncSensorState();
		}
	}

	@Override
	public String getTooltipString() {
		return TooltipUtil.translate("sensor.rs_ctr.dfr" + lastMode);
	}

	@Override
	public ResourceLocation getModel() {
		return MODEL;
	}

	@Override
	public NBTBase serializeNBT() {
		return new NBTTagByte(lastMode);
	}

	@Override
	public void deserializeNBT(NBTBase nbt) {
		lastMode = ((NBTPrimitive)nbt).getByte();
	}

}
