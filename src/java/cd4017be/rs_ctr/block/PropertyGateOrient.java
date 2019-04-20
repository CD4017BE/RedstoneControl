package cd4017be.rs_ctr.block;

import cd4017be.lib.property.PropertyOrientation;
import cd4017be.lib.util.Orientation;
import static cd4017be.lib.util.Orientation.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.AxisDirection;


/**
 * @author CD4017BE
 *
 */
public class PropertyGateOrient extends PropertyOrientation {

	public static final PropertyGateOrient GATE_ORIENT = new PropertyGateOrient("orient");

	protected PropertyGateOrient(String name) {
		super(name, Orientation.values());
	}

	@Override
	public Orientation getPlacementState(boolean sneak, int y, int p, EnumFacing f, float X, float Y, float Z) {
		switch(f) {
		case DOWN:
			if (sneak) {
				return X + Z < 1F ?
					X - Z > 0 ? Te : Tn :
					X - Z < 0 ? Tw : Ts;
			} else return Orientation.values()[y+1 & 3 | 12];
		case UP:
			if (sneak) {
				return X + Z < 1F ?
					X - Z > 0 ? Be : Bn :
					X - Z < 0 ? Bw : Bs;
			} else return Orientation.values()[y+1 & 3 | 4];
		case NORTH: return (sneak || (y & 1) == 0 ? X < 0.5F : (y & 2) != 0) ? Rn : S;
		case SOUTH: return (sneak || (y & 1) == 0 ? X > 0.5F : (y & 2) == 0) ? Rs : N;
		case WEST: return (sneak || (y & 1) != 0 ? Z > 0.5F : (y & 2) != 0) ? Rw : E;
		case EAST: return (sneak || (y & 1) != 0 ? Z < 0.5F : (y & 2) == 0) ? Re : W;
		default: return Orientation.N;
		}
	}

	@Override
	public EnumFacing[] rotations() {
		return EnumFacing.VALUES;
	}

	@Override
	public Orientation getRotatedState(Orientation state, EnumFacing side) {
		int y = state.ordinal(), p = y >> 2; y &= 3;
		int ofs = side.getAxisDirection() == AxisDirection.NEGATIVE ? 1 : -1;
		switch(side.getAxis()) {
		case Y:
			y += ofs;
			break;
		case X:
			if ((y & 1) == 0) p += ofs;
			else if ((p & 1) == 0) {
				p ^= 2; y ^= 2;
			} else p ^= 2;
			break;
		case Z:
			if ((y & 1) != 0) p += ofs;
			else if ((p & 1) == 0) {
				p ^= 2; y ^= 2;
			} else p ^= 2;
			break;
		default: return state;
		}
		return Orientation.values()[y & 3 | p << 2 & 12];
	}

}
