package cd4017be.rscpl.editor;

import io.netty.buffer.ByteBuf;

/**
 * @author CD4017BE
 *
 */
public interface ConfigurableGate {

	void writeCfg(ByteBuf data);

	void readCfg(ByteBuf data);

}
