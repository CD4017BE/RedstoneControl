package cd4017be.api.rs_ctr.frame;

import java.util.Collection;
import net.minecraft.util.math.BlockPos;

/** @author cd4017be */
public interface IFrame {

	Collection<BlockPos> getLinks();

	void link(BlockPos pos);

	void unlink(BlockPos pos);

}
